package com.autofish;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class AutoFishClient implements ClientModInitializer {

    public static AutoFishClient INSTANCE;

    public static final KeyBinding.Category AUTOFISH_CATEGORY = KeyBinding.Category.create(Identifier.of("autofish", "category"));

    private static KeyBinding toggleKey;
    private static KeyBinding configKey;

    public boolean enabled = false;
    public boolean debugMode = false;
    private boolean wasHoldingRod = false;
    public FishingState state = FishingState.IDLE;
    public int tickDelay = 0;
    
    public int settlingTimeout = 0;
    public int soundCooldown = 0;

    // Anti-AFK Variables
    private int postCatchDelay = 0;
    private int movementTicksLeft = 0;
    private boolean isForcingMovement = false;
    private int jumpTicksLeft = 0;
    private boolean isForcingJump = false;
    
    private int p1Ticks = 0;
    private int p2Ticks = 0;
    private int p3Ticks = 0;

    private int mythicalReactionTimer = 0;
    private int mythicalClickTimer = 0;
    
    private int bobberId = -1;
    private double lastBobberX = 0;
    private double lastBobberY = 0;
    private double lastBobberZ = 0;
    private double bobberSpeedSq = 0;

    private final Random random = new Random();

    private static final int SETTLE_MIN = 20, SETTLE_MAX = 40;

    public enum FishingState { IDLE, SETTLING, WATCHING, REEL_WAIT, RECAST_WAIT, MYTHICAL_WAITING, MYTHICAL_REELING }

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        AutoFishConfig.load();
        AutoFishStats.load();
        AutoFishTracker.loadCategories();
        
        // This is the line that makes the HUD draw!
        AutoFishHud.register();
        
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            AutoFishTracker.onMessage(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            AutoFishTracker.onMessage(message.getString());
        });
        
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle AutoFish", GLFW.GLFW_KEY_P, AUTOFISH_CATEGORY));
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open Settings Menu", GLFW.GLFW_KEY_O, AUTOFISH_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("autofish")
                .then(literal("debug")
                    .executes(context -> {
                        debugMode = !debugMode;
                        context.getSource().sendFeedback(Text.literal("§8[§bAutoFish§8] §7Debug mode " + (debugMode ? "§aEnabled" : "§cDisabled")));
                        return 1;
                    })
                )
                // Mod Toggle Commands
                .then(literal("toggle").executes(context -> { setEnabled(!enabled, MinecraftClient.getInstance(), context.getSource()); return 1; }))
                .then(literal("on").executes(context -> { setEnabled(true, MinecraftClient.getInstance(), context.getSource()); return 1; }))
                .then(literal("off").executes(context -> { setEnabled(false, MinecraftClient.getInstance(), context.getSource()); return 1; }))
                
                // Reaction Commands
                .then(literal("minreaction").then(argument("value", IntegerArgumentType.integer(0, 20)).executes(context -> setMinReaction(context.getSource(), IntegerArgumentType.getInteger(context, "value")))))
                .then(literal("maxreaction").then(argument("value", IntegerArgumentType.integer(0, 20)).executes(context -> setMaxReaction(context.getSource(), IntegerArgumentType.getInteger(context, "value")))))
                .then(literal("reaction")
                    .then(argument("min", IntegerArgumentType.integer(0, 20))
                        .then(argument("max", IntegerArgumentType.integer(0, 20))
                            .executes(context -> setReaction(context.getSource(), IntegerArgumentType.getInteger(context, "min"), IntegerArgumentType.getInteger(context, "max")))
                        )
                    )
                )

                // Recast Commands
                .then(literal("minrecast").then(argument("value", IntegerArgumentType.integer(0, 60)).executes(context -> setMinRecast(context.getSource(), IntegerArgumentType.getInteger(context, "value")))))
                .then(literal("maxrecast").then(argument("value", IntegerArgumentType.integer(0, 60)).executes(context -> setMaxRecast(context.getSource(), IntegerArgumentType.getInteger(context, "value")))))
                .then(literal("recast")
                    .then(argument("min", IntegerArgumentType.integer(0, 60))
                        .then(argument("max", IntegerArgumentType.integer(0, 60))
                            .executes(context -> setRecast(context.getSource(), IntegerArgumentType.getInteger(context, "min"), IntegerArgumentType.getInteger(context, "max")))
                        )
                    )
                )

                // Jump Movement Commands
                .then(literal("jump")
                    .then(literal("toggle").executes(context -> setJump(context.getSource(), !AutoFishConfig.INSTANCE.jumpMovement)))
                    .then(literal("on").executes(context -> setJump(context.getSource(), true)))
                    .then(literal("off").executes(context -> setJump(context.getSource(), false)))
                )

                // Random Movement Commands
                .then(literal("movement")
                    .then(literal("toggle").executes(context -> setMovement(context.getSource(), !AutoFishConfig.INSTANCE.randomMovement)))
                    .then(literal("on").executes(context -> setMovement(context.getSource(), true)))
                    .then(literal("off").executes(context -> setMovement(context.getSource(), false)))
                )

                // Catch Mythical Commands
                .then(literal("mythical")
                    .then(literal("toggle").executes(context -> setMythical(context.getSource(), !AutoFishConfig.INSTANCE.catchMythical)))
                    .then(literal("on").executes(context -> setMythical(context.getSource(), true)))
                    .then(literal("off").executes(context -> setMythical(context.getSource(), false)))
                )
                
                // Track Manual Fishing Command
                .then(literal("trackmanual")
                    .then(literal("toggle").executes(context -> setTrackManual(context.getSource(), !AutoFishConfig.INSTANCE.trackManualFishing)))
                    .then(literal("on").executes(context -> setTrackManual(context.getSource(), true)))
                    .then(literal("off").executes(context -> setTrackManual(context.getSource(), false)))
                )
                
                // Stats Commands
                .then(literal("stats")
                    .then(argument("period", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"lifetime", "session"}, builder))
                        .executes(context -> {
                            StatsPrinter.print(context.getSource(), StringArgumentType.getString(context, "period"), "all");
                            return 1;
                        })
                        .then(argument("category", StringArgumentType.word())
                            .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"all", "water", "lava", "ice", "mythical", "creatures", "coins"}, builder))
                            .executes(context -> {
                                StatsPrinter.print(context.getSource(), StringArgumentType.getString(context, "period"), StringArgumentType.getString(context, "category"));
                                return 1;
                            })
                        )
                    )
                )
            );
        });
    }

    // --- Command Helper Methods ---
    
    public void setEnabled(boolean newState, MinecraftClient client, FabricClientCommandSource source) {
        enabled = newState;
        if (enabled && client != null && client.player != null) {
            if (client.player.fishHook != null) {
                setState(FishingState.SETTLING);
                settlingTimeout = 60;
            } else {
                setState(FishingState.IDLE);
            }
            tickDelay = 0;
        } else if (!enabled && client != null) {
            resetAntiAfk(client);
        }
        
        Text msg = Text.literal("§8[§bAutoFish§8] " + (enabled ? "§aEnabled" : "§cDisabled"));
        if (source != null) {
            source.sendFeedback(msg);
        } else if (client != null && client.player != null) {
            client.player.sendMessage(msg, true);
        }
    }

    private int setMinReaction(FabricClientCommandSource source, int val) {
        AutoFishConfig.INSTANCE.minReactionTime = val;
        AutoFishConfig.save();
        source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Min Reaction set to §a" + val + " ticks"));
        return 1;
    }

    private int setMaxReaction(FabricClientCommandSource source, int val) {
        AutoFishConfig.INSTANCE.maxReactionTime = val;
        AutoFishConfig.save();
        source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Max Reaction set to §a" + val + " ticks"));
        return 1;
    }

    private int setReaction(FabricClientCommandSource source, int min, int max) {
        AutoFishConfig.INSTANCE.minReactionTime = min;
        AutoFishConfig.INSTANCE.maxReactionTime = max;
        AutoFishConfig.save();
        source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Reaction set to §a" + min + " - " + max + " ticks"));
        return 1;
    }

    private int setMinRecast(FabricClientCommandSource source, int val) {
        AutoFishConfig.INSTANCE.minRecastDelay = val;
        AutoFishConfig.save();
        source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Min Recast set to §a" + val + " ticks"));
        return 1;
    }

    private int setMaxRecast(FabricClientCommandSource source, int val) {
        AutoFishConfig.INSTANCE.maxRecastDelay = val;
        AutoFishConfig.save();
        source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Max Recast set to §a" + val + " ticks"));
        return 1;
    }

    private int setRecast(FabricClientCommandSource source, int min, int max) {
        AutoFishConfig.INSTANCE.minRecastDelay = min;
        AutoFishConfig.INSTANCE.maxRecastDelay = max;
        AutoFishConfig.save();
        source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Recast set to §a" + min + " - " + max + " ticks"));
        return 1;
    }

    private int setJump(FabricClientCommandSource source, boolean state) {
        AutoFishConfig.INSTANCE.jumpMovement = state;
        if (state && AutoFishConfig.INSTANCE.randomMovement) {
            AutoFishConfig.INSTANCE.randomMovement = false;
            source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Movement turned §coff§7 due to mutual exclusivity."));
        }
        AutoFishConfig.save();
        source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Jump Movement " + (state ? "§aEnabled" : "§cDisabled")));
        return 1;
    }

    private int setMovement(FabricClientCommandSource source, boolean state) {
        AutoFishConfig.INSTANCE.randomMovement = state;
        if (state && AutoFishConfig.INSTANCE.jumpMovement) {
            AutoFishConfig.INSTANCE.jumpMovement = false;
            source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Jump turned §coff§7 due to mutual exclusivity."));
        }
        AutoFishConfig.save();
        source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Random Movement " + (state ? "§aEnabled" : "§cDisabled")));
        return 1;
    }

    private int setMythical(FabricClientCommandSource source, boolean state) {
        AutoFishConfig.INSTANCE.catchMythical = state;
        AutoFishConfig.save();
        source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Catch Mythical " + (state ? "§aEnabled" : "§cDisabled")));
        return 1;
    }
    
    private int setTrackManual(FabricClientCommandSource source, boolean state) {
        AutoFishConfig.INSTANCE.trackManualFishing = state;
        AutoFishConfig.save();
        source.sendFeedback(Text.literal("§8[§bAutoFish§8] §7Track Manual Fishing " + (state ? "§aEnabled" : "§cDisabled")));
        return 1;
    }

    public void setState(FishingState newState) {
        if (this.debugMode && this.state != newState) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§8[§bAutoFish Debug§8] §7State: §e" + newState.name()), false);
            }
        }
        this.state = newState;
    }

    public static void onSoundDetected(String soundName, float pitch, float volume, double soundX, double soundY, double soundZ) {
        if (INSTANCE == null || !INSTANCE.enabled || INSTANCE.state != FishingState.WATCHING || INSTANCE.soundCooldown > 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (soundName.contains("block.note_block.pling") && pitch == 1.0f) {
            double distSq = client.player.squaredDistanceTo(soundX, soundY, soundZ);
            if (distSq < 1.0) {
                INSTANCE.soundCooldown = 40; 
                INSTANCE.setState(FishingState.REEL_WAIT);
                INSTANCE.tickDelay = INSTANCE.rand(AutoFishConfig.INSTANCE.minReactionTime, AutoFishConfig.INSTANCE.maxReactionTime);
            }
        }
    }

    private boolean isHypixel(MinecraftClient client) {
        if (client.isInSingleplayer()) return false;
        if (client.world != null && client.world.getScoreboard() != null) {
            for (net.minecraft.scoreboard.ScoreboardObjective obj : client.world.getScoreboard().getObjectives()) {
                if (obj.getDisplayName().getString().toLowerCase().contains("hypixel") || obj.getDisplayName().getString().toLowerCase().contains("skyblock")) return true;
            }
        }
        ServerInfo server = client.getCurrentServerEntry();
        if (server != null && server.address != null) {
            String ip = server.address.toLowerCase();
            if (ip.contains("hypixel") || ip.contains("catgirls.xyz")) return true;
        }
        return false;
    }

    private void tick(MinecraftClient client) {
        handleKeybinds(client);
        
        if (soundCooldown > 0) soundCooldown--;
        
        ClientPlayerEntity player = client.player;
        if (player == null) {
            if (enabled) resetAntiAfk(client);
            return;
        }

        boolean holdingRod = isHoldingFishingRod(player);
        if (enabled && holdingRod && !wasHoldingRod) {
            if (player.fishHook != null) {
                setState(FishingState.SETTLING);
                tickDelay = rand(SETTLE_MIN, SETTLE_MAX);
                settlingTimeout = 60;
            } else {
                setState(FishingState.IDLE);
                tickDelay = 5;
            }
        }
        wasHoldingRod = holdingRod;

        if (!enabled) {
            resetAntiAfk(client);
            return;
        }

        if (client.currentScreen != null && !(client.currentScreen instanceof ChatScreen)) {
            resetAntiAfk(client);
            return;
        }

        handleAntiAfk(client);

        FishingBobberEntity bobber = player.fishHook;
        if (bobber != null) {
            if (bobber.getId() != bobberId) {
                bobberId = bobber.getId();
                lastBobberX = bobber.getX(); lastBobberY = bobber.getY(); lastBobberZ = bobber.getZ();
                bobberSpeedSq = 0;
            } else {
                double dx = bobber.getX() - lastBobberX; double dz = bobber.getZ() - lastBobberZ;
                bobberSpeedSq = dx * dx + dz * dz; 
                lastBobberX = bobber.getX(); lastBobberY = bobber.getY(); lastBobberZ = bobber.getZ();
            }
        } else {
            bobberId = -1; bobberSpeedSq = 0;
        }

        if (tickDelay > 0) {
            tickDelay--; return;
        }

        if (!holdingRod) return;

        switch (state) {
            case IDLE -> {
                if (bobber == null || bobber.isRemoved()) useRod(client, player);
                setState(FishingState.SETTLING);
                tickDelay = rand(SETTLE_MIN, SETTLE_MAX);
                settlingTimeout = 60;
            }
            case SETTLING -> {
                if (bobber == null || bobber.isRemoved()) {
                    setState(FishingState.IDLE);
                    tickDelay = 10;
                    return;
                }
                if (bobberSpeedSq < 0.005 && Math.abs(bobber.getVelocity().getY()) < 0.1) {
                    setState(FishingState.WATCHING);
                } else {
                    settlingTimeout--;
                    if (settlingTimeout <= 0) setState(FishingState.WATCHING);
                }
            }
            case WATCHING -> {
                if (bobber == null) {
                    setState(FishingState.IDLE);
                    tickDelay = rand(AutoFishConfig.INSTANCE.minRecastDelay, AutoFishConfig.INSTANCE.maxRecastDelay);
                    return;
                }
                if (AutoFishConfig.INSTANCE.catchMythical && bobberSpeedSq > 0.005) {
                    setState(FishingState.MYTHICAL_WAITING);
                    mythicalReactionTimer = rand(4, 6);
                    return;
                }
                if (!isHypixel(client) && bobber.getVelocity().getY() <= -0.04) {
                    setState(FishingState.REEL_WAIT);
                    tickDelay = rand(AutoFishConfig.INSTANCE.minReactionTime, AutoFishConfig.INSTANCE.maxReactionTime);
                }
            }
            case REEL_WAIT -> {
                useRod(client, player); 
                setState(FishingState.RECAST_WAIT);
                tickDelay = rand(AutoFishConfig.INSTANCE.minRecastDelay, AutoFishConfig.INSTANCE.maxRecastDelay);
            }
            case RECAST_WAIT -> {
                useRod(client, player); 
                setState(FishingState.SETTLING);
                tickDelay = rand(SETTLE_MIN, SETTLE_MAX);
                settlingTimeout = 60;
                
                if (random.nextInt(8) == 0) postCatchDelay = rand(15, 40);
            }
            case MYTHICAL_WAITING -> {
                if (bobber == null || bobber.isRemoved()) {
                    setState(FishingState.IDLE);
                    tickDelay = 15;
                    if (random.nextInt(8) == 0) postCatchDelay = rand(15, 40);
                    return;
                }
                if (bobberSpeedSq > 0.0005) mythicalReactionTimer = rand(4, 6); 
                else {
                    mythicalReactionTimer--; 
                    if (mythicalReactionTimer <= 0) {
                        setState(FishingState.MYTHICAL_REELING);
                        mythicalReactionTimer = rand(3, 5); mythicalClickTimer = 0;
                    }
                }
            }
            case MYTHICAL_REELING -> {
                if (bobber == null || bobber.isRemoved()) {
                    setState(FishingState.IDLE);
                    tickDelay = 15;
                    if (random.nextInt(8) == 0) postCatchDelay = rand(15, 40);
                    return;
                }
                if (bobberSpeedSq <= 0.0005) mythicalReactionTimer = rand(3, 5); 
                else {
                    mythicalReactionTimer--; 
                    if (mythicalReactionTimer <= 0) {
                        setState(FishingState.MYTHICAL_WAITING);
                        mythicalReactionTimer = rand(4, 6); return;
                    }
                }
                mythicalClickTimer--;
                if (mythicalClickTimer <= 0) {
                    useRod(client, player);
                    player.setYaw(player.getYaw() + (random.nextFloat() - 0.5f) * 1.0f);
                    player.setPitch(player.getPitch() + (random.nextFloat() - 0.5f) * 1.0f);
                    mythicalClickTimer = rand(2, 3);
                }
            }
        }
    }

    private void handleAntiAfk(MinecraftClient client) {
        if (postCatchDelay > 0) {
            postCatchDelay--;
            if (postCatchDelay == 0) {
                if (AutoFishConfig.INSTANCE.randomMovement) {
                    p1Ticks = rand(6, 10);
                    p2Ticks = rand(12, 20);
                    p3Ticks = rand(6, 10);
                    movementTicksLeft = p1Ticks + p2Ticks + p3Ticks;
                } else if (AutoFishConfig.INSTANCE.jumpMovement) {
                    jumpTicksLeft = 2; // Jump for exactly 2 ticks
                }
            }
        }

        // Handle walking
        if (movementTicksLeft > 0) {
            isForcingMovement = true;
            if (movementTicksLeft > p2Ticks + p3Ticks) {
                client.options.leftKey.setPressed(true); client.options.rightKey.setPressed(false);
            } else if (movementTicksLeft > p3Ticks) {
                client.options.leftKey.setPressed(false); client.options.rightKey.setPressed(true);
            } else {
                client.options.rightKey.setPressed(false); client.options.leftKey.setPressed(true);
            }
            movementTicksLeft--;
        } else if (isForcingMovement) {
            client.options.leftKey.setPressed(false); client.options.rightKey.setPressed(false);
            isForcingMovement = false;
        }

        // Handle jumping
        if (jumpTicksLeft > 0) {
            isForcingJump = true;
            client.options.jumpKey.setPressed(true);
            jumpTicksLeft--;
        } else if (isForcingJump) {
            client.options.jumpKey.setPressed(false);
            isForcingJump = false;
        }
    }

    private void resetAntiAfk(MinecraftClient client) {
        if (isForcingMovement) {
            client.options.leftKey.setPressed(false); client.options.rightKey.setPressed(false);
            movementTicksLeft = 0; isForcingMovement = false;
        }
        if (isForcingJump) {
            client.options.jumpKey.setPressed(false);
            jumpTicksLeft = 0; isForcingJump = false;
        }
        postCatchDelay = 0;
    }

    private void handleKeybinds(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            setEnabled(!enabled, client, null);
        }
        while (configKey.wasPressed()) client.setScreen(AutoFishScreen.createConfigScreen(client.currentScreen));
    }

    private void useRod(MinecraftClient client, ClientPlayerEntity player) {
        Hand hand = player.getMainHandStack().getItem() instanceof FishingRodItem ? Hand.MAIN_HAND : Hand.OFF_HAND;
        client.interactionManager.interactItem(player, hand);
    }

    private boolean isHoldingFishingRod(ClientPlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof FishingRodItem || player.getOffHandStack().getItem() instanceof FishingRodItem;
    }

    private int rand(int min, int max) {
        return min >= max ? min : min + random.nextInt(max - min + 1);
    }
}

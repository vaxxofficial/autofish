package com.autofish;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.*;

public class AutoFishScreen {

    private static final List<String> MYTHICAL_ORDER = Arrays.asList(
        "ember of helios", "dust of selene", "shadow of nyx", "heart of aphrodite",
        "spark of zeus", "spirit of demeter", "automaton of daedalus", "wrath of hades"
    );

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("AutoFish Settings"));

        builder.setSavingRunnable(() -> {
            AutoFishConfig.save();
        });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // --- GENERAL TAB ---
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Mod Enabled"), AutoFishClient.INSTANCE.enabled)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Turn the AutoFish mod on or off."))
                .setSaveConsumer(newValue -> {
                    if (AutoFishClient.INSTANCE.enabled != newValue) {
                        AutoFishClient.INSTANCE.setEnabled(newValue, MinecraftClient.getInstance(), null);
                    }
                })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Random Movement"), AutoFishConfig.INSTANCE.randomMovement)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Randomly walk to avoid AFK. Turns off Jump if enabled."))
                .setSaveConsumer(newValue -> {
                    AutoFishConfig.INSTANCE.randomMovement = newValue;
                    if (newValue) AutoFishConfig.INSTANCE.jumpMovement = false;
                })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Jump Movement"), AutoFishConfig.INSTANCE.jumpMovement)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Randomly jump to avoid AFK. Turns off Walking if enabled."))
                .setSaveConsumer(newValue -> {
                    AutoFishConfig.INSTANCE.jumpMovement = newValue;
                    if (newValue) AutoFishConfig.INSTANCE.randomMovement = false;
                })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Catch Mythical"), AutoFishConfig.INSTANCE.catchMythical)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Attempt to fight and catch mythical fish."))
                .setSaveConsumer(newValue -> AutoFishConfig.INSTANCE.catchMythical = newValue)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Text.literal("Min Reaction (ticks)"), AutoFishConfig.INSTANCE.minReactionTime, 0, 20)
                .setDefaultValue(1)
                .setSaveConsumer(newValue -> AutoFishConfig.INSTANCE.minReactionTime = newValue)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Text.literal("Max Reaction (ticks)"), AutoFishConfig.INSTANCE.maxReactionTime, 0, 20)
                .setDefaultValue(3)
                .setSaveConsumer(newValue -> AutoFishConfig.INSTANCE.maxReactionTime = newValue)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Text.literal("Min Recast (ticks)"), AutoFishConfig.INSTANCE.minRecastDelay, 0, 60)
                .setDefaultValue(1)
                .setSaveConsumer(newValue -> AutoFishConfig.INSTANCE.minRecastDelay = newValue)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Text.literal("Max Recast (ticks)"), AutoFishConfig.INSTANCE.maxRecastDelay, 0, 60)
                .setDefaultValue(4)
                .setSaveConsumer(newValue -> AutoFishConfig.INSTANCE.maxRecastDelay = newValue)
                .build());

        // --- STATISTICS TAB ---
        ConfigCategory stats = builder.getOrCreateCategory(Text.literal("Statistics"));

        stats.addEntry(entryBuilder.startTextDescription(Text.literal("§l--- Session Statistics ---")).build());
        stats.addEntry(entryBuilder.startTextDescription(Text.literal("§7Total Catches: §f" + AutoFishStats.INSTANCE.sessionCaught)).build());
        stats.addEntry(entryBuilder.startTextDescription(Text.literal("§7Mythicals: §f" + AutoFishStats.INSTANCE.sessionMythicals)).build());
        buildCategorizedStats(stats, entryBuilder, AutoFishStats.INSTANCE.sessionItems, AutoFishStats.INSTANCE.sessionItemValues, AutoFishStats.INSTANCE.sessionMythicalWeights);

        stats.addEntry(entryBuilder.startTextDescription(Text.literal(" ")).build()); // Spacer

        stats.addEntry(entryBuilder.startTextDescription(Text.literal("§l--- Lifetime Statistics ---")).build());
        stats.addEntry(entryBuilder.startTextDescription(Text.literal("§7Total Catches: §f" + AutoFishStats.INSTANCE.lifetimeCaught)).build());
        stats.addEntry(entryBuilder.startTextDescription(Text.literal("§7Mythicals: §f" + AutoFishStats.INSTANCE.lifetimeMythicals)).build());
        buildCategorizedStats(stats, entryBuilder, AutoFishStats.INSTANCE.lifetimeItems, AutoFishStats.INSTANCE.lifetimeItemValues, AutoFishStats.INSTANCE.lifetimeMythicalWeights);

        return builder.build();
    }

    private static void buildCategorizedStats(ConfigCategory categoryUI, ConfigEntryBuilder entryBuilder, Map<String, Integer> items, Map<String, Integer> values, Map<String, Integer> weights) {
        if (items.isEmpty() && weights.isEmpty()) {
            categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("§8  (No items caught yet)")).build());
            return;
        }

        buildEnvironmentSection(categoryUI, entryBuilder, "water", items, values, false);
        buildEnvironmentSection(categoryUI, entryBuilder, "lava", items, values, false);
        buildEnvironmentSection(categoryUI, entryBuilder, "ice", items, values, false);
        buildCoinsSection(categoryUI, entryBuilder, items, values);
        buildEnvironmentSection(categoryUI, entryBuilder, "creatures", items, values, true);
        buildMythicalSection(categoryUI, entryBuilder, weights);
    }

    private static void buildEnvironmentSection(ConfigCategory categoryUI, ConfigEntryBuilder entryBuilder, String targetEnv, Map<String, Integer> items, Map<String, Integer> values, boolean onlyCreatures) {
        Map<String, Map<String, Integer>> categorized = new HashMap<>();
        int totalEnvCount = 0;

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String name = entry.getKey();
            int count = entry.getValue();
            AutoFishTracker.CategoryData data = AutoFishTracker.ITEM_DATA.get(name);
            
            if (data == null) continue;
            if (onlyCreatures && !data.category.equals("creatures")) continue;
            if (!onlyCreatures && data.category.equals("creatures")) continue;
            if (!onlyCreatures && data.category.equals("mythical")) continue;
            
            boolean matchesEnv = data.environment.contains(targetEnv) || data.environment.equals("global");
            if (onlyCreatures) matchesEnv = true;

            if (matchesEnv) {
                categorized.putIfAbsent(data.category, new HashMap<>());
                categorized.get(data.category).put(name, count);
                totalEnvCount += count;
            }
        }

        if (totalEnvCount == 0) return;

        if (!onlyCreatures) categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("§e" + targetEnv + " -")).build());
        else categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("§ecreatures -")).build());

        for (String cat : new String[]{"fish", "junk", "treasure", "plants", "creatures"}) {
            if (categorized.containsKey(cat)) {
                Map<String, Integer> catItems = categorized.get(cat);
                int catTotal = catItems.values().stream().mapToInt(Integer::intValue).sum();
                categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("  §b" + cat + ": §7(" + catTotal + " total)")).build());
                
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(catItems.entrySet());
                sorted.sort((a, b) -> {
                    String nameA = a.getKey();
                    String nameB = b.getKey();
                    AutoFishTracker.CategoryData dataA = AutoFishTracker.ITEM_DATA.get(nameA);
                    AutoFishTracker.CategoryData dataB = AutoFishTracker.ITEM_DATA.get(nameB);

                    boolean isGlobalA = dataA != null && dataA.environment.equals("global");
                    boolean isGlobalB = dataB != null && dataB.environment.equals("global");
                    boolean isSharedA = dataA != null && dataA.environment.equals("water/ice");
                    boolean isSharedB = dataB != null && dataB.environment.equals("water/ice");

                    if (isGlobalA && !isGlobalB) return 1;
                    if (!isGlobalA && isGlobalB) return -1;
                    if (isSharedA && !isSharedB) return 1;
                    if (!isSharedA && isSharedB) return -1;

                    return b.getValue().compareTo(a.getValue());
                });
                
                for (Map.Entry<String, Integer> item : sorted) {
                    String name = item.getKey();
                    int count = item.getValue();
                    
                    String suffix = "";
                    AutoFishTracker.CategoryData itemData = AutoFishTracker.ITEM_DATA.get(name);
                    String color = "§f";
                    if (itemData != null) {
                        if (itemData.environment.equals("water/ice")) suffix = " §7(water & ice)";
                        else if (itemData.environment.equals("global")) suffix = " §7(water, lava, & ice)";
                        color = getColor(name, itemData.category, itemData.rarity);
                    }
                    
                    String formattedName = formatItemName(name, itemData != null ? itemData.category : "");
                    
                    if (values != null && values.containsKey(name)) {
                        categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("    §f" + count + "x - " + values.get(name) + " " + color + formattedName + suffix)).build());
                    } else {
                        categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("    §f" + count + "x " + color + formattedName + suffix)).build());
                    }
                }
            }
        }
    }

    private static void buildCoinsSection(ConfigCategory categoryUI, ConfigEntryBuilder entryBuilder, Map<String, Integer> items, Map<String, Integer> values) {
        boolean found = false;
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(items.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        for (Map.Entry<String, Integer> entry : sorted) {
            String name = entry.getKey();
            if (name.endsWith("coins") && !name.equals("game coins")) {
                if (!found) {
                    categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("§ecoins breakdown -")).build());
                    found = true;
                }
                int count = entry.getValue();
                int val = values.getOrDefault(name, 0);
                String formattedName = formatItemName(name, "treasure");
                String color = getColor(name, "treasure", null);
                categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("  §f" + count + "x - " + val + " " + color + formattedName + " §7(water, lava, & ice)")).build());
            }
        }
    }

    private static void buildMythicalSection(ConfigCategory categoryUI, ConfigEntryBuilder entryBuilder, Map<String, Integer> weights) {
        int totalMythicals = 0;
        Map<String, Map<String, Integer>> rarityWeights = new HashMap<>();
        Map<String, Integer> rarityTotals = new HashMap<>();

        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            String weightStr = entry.getKey(); 
            int count = entry.getValue();
            
            String[] parts = weightStr.split("kg ", 2);
            if (parts.length < 2) continue;
            String name = parts[1];
            
            AutoFishTracker.CategoryData data = AutoFishTracker.ITEM_DATA.get(name);
            if (data != null && data.category.equals("mythical")) {
                totalMythicals += count;
                String rarity = data.rarity;
                
                rarityWeights.putIfAbsent(rarity, new HashMap<>());
                rarityWeights.get(rarity).put(weightStr, count);
                rarityTotals.put(rarity, rarityTotals.getOrDefault(rarity, 0) + count);
            }
        }

        if (totalMythicals == 0) return;
        categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("§emythical fish - §7(" + totalMythicals + " total)")).build());

        for (String rarity : new String[]{"common", "uncommon", "rare", "ultra rare"}) {
            if (rarityWeights.containsKey(rarity)) {
                int rTotal = rarityTotals.get(rarity);
                categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("  §b" + rarity + ": §7(" + rTotal + " total)")).build());
                
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(rarityWeights.get(rarity).entrySet());
                sorted.sort((a, b) -> {
                    String[] partsA = a.getKey().split("kg ", 2);
                    String[] partsB = b.getKey().split("kg ", 2);
                    
                    if (partsA.length < 2 || partsB.length < 2) return b.getValue().compareTo(a.getValue());
                    
                    String nameA = partsA[1];
                    String nameB = partsB[1];
                    
                    int indexA = MYTHICAL_ORDER.indexOf(nameA);
                    int indexB = MYTHICAL_ORDER.indexOf(nameB);
                    
                    if (indexA != indexB && indexA != -1 && indexB != -1) return Integer.compare(indexA, indexB);
                    
                    try {
                        int weightA = Integer.parseInt(partsA[0]);
                        int weightB = Integer.parseInt(partsB[0]);
                        return Integer.compare(weightB, weightA);
                    } catch (NumberFormatException e) {
                        return b.getValue().compareTo(a.getValue());
                    }
                });
                
                for (Map.Entry<String, Integer> item : sorted) {
                    String[] parts = item.getKey().split("kg ", 2);
                    if (parts.length == 2) {
                        String weight = parts[0] + "kg ";
                        String name = parts[1];
                        String color = getColor(name, "mythical", rarity);
                        String formattedName = formatItemName(name, "mythical");
                        categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("    §f" + item.getValue() + "x " + color + weight + formattedName)).build());
                    } else {
                        categoryUI.addEntry(entryBuilder.startTextDescription(Text.literal("    §f" + item.getValue() + "x " + item.getKey())).build());
                    }
                }
            }
        }
    }

    private static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder result = new StringBuilder();
        boolean nextTitleCase = true;
        for (char c : text.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toUpperCase(c);
                nextTitleCase = false;
            }
            result.append(c);
        }
        return result.toString();
    }

    private static String formatItemName(String name, String category) {
        if ("mythical".equals(category) || name.endsWith("coins") || name.endsWith("experience")) {
            return toTitleCase(name);
        }
        return name; 
    }

    private static String getColor(String name, String category, String rarity) {
        if ("fish".equals(category)) return "§e";
        if ("junk".equals(category)) return "§c";
        if ("plants".equals(category)) return "§2";
        if ("creatures".equals(category)) return "§b";
        if ("mythical".equals(category)) {
            if ("common".equals(rarity)) return "§e";
            if ("uncommon".equals(rarity)) return "§a";
            if ("rare".equals(rarity)) return "§b";
            if ("ultra rare".equals(rarity)) return "§d";
        }
        if ("treasure".equals(category)) {
            if (name.endsWith("coins")) return "§6";
            if (name.contains("event experience")) return "§e";
            if (name.contains("hypixel experience")) return "§3";
            return "§a";
        }
        return "§f";
    }
}

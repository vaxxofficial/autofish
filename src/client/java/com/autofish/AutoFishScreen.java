package com.autofish;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoFishScreen {

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
        buildStatEntries(stats, entryBuilder, AutoFishStats.INSTANCE.sessionItems);

        stats.addEntry(entryBuilder.startTextDescription(Text.literal(" ")).build()); // Spacer

        stats.addEntry(entryBuilder.startTextDescription(Text.literal("§l--- Lifetime Statistics ---")).build());
        stats.addEntry(entryBuilder.startTextDescription(Text.literal("§7Total Catches: §f" + AutoFishStats.INSTANCE.lifetimeCaught)).build());
        stats.addEntry(entryBuilder.startTextDescription(Text.literal("§7Mythicals: §f" + AutoFishStats.INSTANCE.lifetimeMythicals)).build());
        buildStatEntries(stats, entryBuilder, AutoFishStats.INSTANCE.lifetimeItems);

        return builder.build();
    }

    private static void buildStatEntries(ConfigCategory category, ConfigEntryBuilder entryBuilder, Map<String, Integer> items) {
        if (items.isEmpty()) {
            category.addEntry(entryBuilder.startTextDescription(Text.literal("§8  (No items caught yet)")).build());
            return;
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(items.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : sorted) {
            String name = entry.getKey();
            int count = entry.getValue();
            category.addEntry(entryBuilder.startTextDescription(Text.literal("  §f" + count + "x §7" + toTitleCase(name))).build());
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
}

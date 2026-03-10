package com.autofish;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class AutoFishScreen {

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("AutoFish Settings"));

        builder.setSavingRunnable(() -> {
            AutoFishConfig.save();
        });

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // New Mod Toggle Entry
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
                    if (newValue) AutoFishConfig.INSTANCE.jumpMovement = false; // Mutually exclusive
                })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Jump Movement"), AutoFishConfig.INSTANCE.jumpMovement)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Randomly jump to avoid AFK. Turns off Walking if enabled."))
                .setSaveConsumer(newValue -> {
                    AutoFishConfig.INSTANCE.jumpMovement = newValue;
                    if (newValue) AutoFishConfig.INSTANCE.randomMovement = false; // Mutually exclusive
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

        return builder.build();
    }
}

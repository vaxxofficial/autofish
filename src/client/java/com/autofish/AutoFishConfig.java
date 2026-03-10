package com.autofish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AutoFishConfig {
    public static final AutoFishConfig INSTANCE = new AutoFishConfig();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "autofish.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int minReactionTime = 1;
    public int maxReactionTime = 3;
    public int minRecastDelay = 1;
    public int maxRecastDelay = 4;
    public boolean randomMovement = false;
    public boolean jumpMovement = false;
    public boolean catchMythical = false;
    public boolean trackManualFishing = false;

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                AutoFishConfig config = GSON.fromJson(reader, AutoFishConfig.class);
                INSTANCE.minReactionTime = config.minReactionTime;
                INSTANCE.maxReactionTime = config.maxReactionTime;
                INSTANCE.minRecastDelay = config.minRecastDelay;
                INSTANCE.maxRecastDelay = config.maxRecastDelay;
                INSTANCE.randomMovement = config.randomMovement;
                INSTANCE.jumpMovement = config.jumpMovement;
                INSTANCE.catchMythical = config.catchMythical;
                INSTANCE.trackManualFishing = config.trackManualFishing;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        if (INSTANCE.minReactionTime > INSTANCE.maxReactionTime) {
            INSTANCE.minReactionTime = INSTANCE.maxReactionTime;
        }
        if (INSTANCE.minRecastDelay > INSTANCE.maxRecastDelay) {
            INSTANCE.minRecastDelay = INSTANCE.maxRecastDelay;
        }
        
        // Safety check: ensure both aren't saved as true manually in the JSON
        if (INSTANCE.randomMovement && INSTANCE.jumpMovement) {
            INSTANCE.jumpMovement = false; 
        }

        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

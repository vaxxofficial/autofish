package com.autofish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AutoFishStats {
    public static final AutoFishStats INSTANCE = new AutoFishStats();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "autofish_stats.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int lifetimeCaught = 0;
    public int lifetimeMythicals = 0;
    public Map<String, Integer> lifetimeItems = new HashMap<>();
    public Map<String, Integer> lifetimeMythicalWeights = new HashMap<>();

    public transient int sessionCaught = 0;
    public transient int sessionMythicals = 0;
    public transient Map<String, Integer> sessionItems = new HashMap<>();
    public transient Map<String, Integer> sessionMythicalWeights = new HashMap<>();

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                AutoFishStats data = GSON.fromJson(reader, AutoFishStats.class);
                if (data != null) {
                    INSTANCE.lifetimeCaught = data.lifetimeCaught;
                    INSTANCE.lifetimeMythicals = data.lifetimeMythicals;
                    if (data.lifetimeItems != null) INSTANCE.lifetimeItems = data.lifetimeItems;
                    if (data.lifetimeMythicalWeights != null) INSTANCE.lifetimeMythicalWeights = data.lifetimeMythicalWeights;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addMythical(String name, int weight) {
        sessionCaught++; lifetimeCaught++;
        sessionMythicals++; lifetimeMythicals++;
        
        String weightKey = weight + "kg " + name;
        sessionMythicalWeights.put(weightKey, sessionMythicalWeights.getOrDefault(weightKey, 0) + 1);
        lifetimeMythicalWeights.put(weightKey, lifetimeMythicalWeights.getOrDefault(weightKey, 0) + 1);
        
        save();
    }

    public void addItem(String name) {
        sessionCaught++; lifetimeCaught++;
        sessionItems.put(name, sessionItems.getOrDefault(name, 0) + 1);
        lifetimeItems.put(name, lifetimeItems.getOrDefault(name, 0) + 1);
        save();
    }
}

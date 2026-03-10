package com.autofish;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoFishTracker {
    public static Map<String, CategoryData> ITEM_DATA = new HashMap<>();

    public static class CategoryData {
        public String category;
        public String rarity;
        public String environment;
    }

    public static void loadCategories() {
        try (InputStream is = AutoFishTracker.class.getResourceAsStream("/autofish_categories.json")) {
            if (is != null) {
                ITEM_DATA = new Gson().fromJson(new InputStreamReader(is), new TypeToken<Map<String, CategoryData>>(){}.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onMessage(String text) {
        // Only track stats if the mod is actively running
        if (AutoFishClient.INSTANCE == null || !AutoFishClient.INSTANCE.enabled) return;

        // Strip color codes to make matching text easier
        String plain = text.replaceAll("§.", "");
        
        Matcher m5 = Pattern.compile("You caught (?:an? )?(\\d+)kg (.*)!").matcher(plain);
        if (m5.matches()) {
            AutoFishStats.INSTANCE.addMythical(m5.group(2).toLowerCase(), Integer.parseInt(m5.group(1)));
            return;
        }
        
        Matcher m2 = Pattern.compile("You caught (?:an? )?(.*), that's a treasure!").matcher(plain);
        if (m2.matches()) {
            AutoFishStats.INSTANCE.addItem(m2.group(1).toLowerCase());
            return;
        }
        
        Matcher m4 = Pattern.compile("Oh no, you caught (?:an? )?(.*)!").matcher(plain);
        if (m4.matches()) {
            AutoFishStats.INSTANCE.addItem(m4.group(1).toLowerCase());
            return;
        }
        
        // This single check now handles both "You caught a (fish)!" and "You caught (network treasure)!"
        Matcher m1 = Pattern.compile("You caught (?:an? )?(.*)!").matcher(plain);
        if (m1.matches()) {
            AutoFishStats.INSTANCE.addItem(m1.group(1).toLowerCase());
            return;
        }
    }
}

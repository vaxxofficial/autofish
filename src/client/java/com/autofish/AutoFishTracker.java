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
        if (AutoFishClient.INSTANCE == null) return;
        
        String plain = text.replaceAll("§.", "");
        String lower = plain.toLowerCase();
        
        // Immediately check for Limbo messages
        if (lower.contains("limbo") && (lower.contains("spawned in") || lower.contains("put in"))) {
            AutoFishClient.INSTANCE.triggerFailsafe("You were sent to Limbo!");
        }
        
        if (!AutoFishClient.INSTANCE.enabled && !AutoFishConfig.INSTANCE.trackManualFishing) return;
        
        Matcher mCurr = Pattern.compile("You caught (?:an? )?([\\d,]+) (.*)!").matcher(plain);
        if (mCurr.matches()) {
            String specificType = mCurr.group(2).toLowerCase();
            if (specificType.endsWith("coins") || specificType.endsWith("experience")) {
                int amount = Integer.parseInt(mCurr.group(1).replace(",", ""));
                String baseType = "game coins";
                if (specificType.endsWith("experience")) {
                    if (specificType.equals("hypixel experience")) baseType = "hypixel experience";
                    else if (specificType.equals("guild experience")) baseType = "guild experience";
                    else baseType = "event experience";
                }
                AutoFishStats.INSTANCE.addCurrency(baseType, specificType, amount);
                return;
            }
        }
        
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
        
        Matcher m1 = Pattern.compile("You caught (?:an? )?(.*)!").matcher(plain);
        if (m1.matches()) {
            AutoFishStats.INSTANCE.addItem(m1.group(1).toLowerCase());
            return;
        }
    }
}

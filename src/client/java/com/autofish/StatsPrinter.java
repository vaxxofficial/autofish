package com.autofish;

import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import java.util.*;

public class StatsPrinter {

    public static void print(FabricClientCommandSource source, String period, String filter) {
        period = period.toLowerCase();
        filter = filter.toLowerCase();

        if (!period.equals("lifetime") && !period.equals("session")) {
            source.sendFeedback(Text.literal("§cInvalid period. Use 'lifetime' or 'session'."));
            return;
        }

        boolean isLifetime = period.equals("lifetime");
        Map<String, Integer> items = isLifetime ? AutoFishStats.INSTANCE.lifetimeItems : AutoFishStats.INSTANCE.sessionItems;
        Map<String, Integer> weights = isLifetime ? AutoFishStats.INSTANCE.lifetimeMythicalWeights : AutoFishStats.INSTANCE.sessionMythicalWeights;

        source.sendFeedback(Text.literal("§8[§bAutoFish Stats: " + period.toUpperCase() + "§8]"));

        if (filter.equals("mythical")) {
            printMythical(source, weights);
        } else if (filter.equals("creatures")) {
            printEnvironment(source, items, "creatures", true);
        } else {
            if (filter.equals("all") || filter.equals("water")) printEnvironment(source, items, "water", false);
            if (filter.equals("all") || filter.equals("lava")) printEnvironment(source, items, "lava", false);
            if (filter.equals("all") || filter.equals("ice")) printEnvironment(source, items, "ice", false);
        }
    }

    private static void printEnvironment(FabricClientCommandSource source, Map<String, Integer> items, String targetEnv, boolean onlyCreatures) {
        if (!onlyCreatures) source.sendFeedback(Text.literal("§e" + targetEnv + " -"));
        else source.sendFeedback(Text.literal("§ecreatures -"));

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
            
            // This contains() check handles your Water/Ice overlapping requirement perfectly!
            boolean matchesEnv = data.environment.contains(targetEnv) || data.environment.equals("global");
            if (onlyCreatures) matchesEnv = true;

            if (matchesEnv) {
                categorized.putIfAbsent(data.category, new HashMap<>());
                categorized.get(data.category).put(name, count);
                totalEnvCount += count;
            }
        }

        if (totalEnvCount == 0) {
            source.sendFeedback(Text.literal("  §7(No items caught yet)"));
            return;
        }

        for (String cat : new String[]{"fish", "junk", "treasure", "plants", "creatures"}) {
            if (categorized.containsKey(cat)) {
                Map<String, Integer> catItems = categorized.get(cat);
                int catTotal = catItems.values().stream().mapToInt(Integer::intValue).sum();
                source.sendFeedback(Text.literal("  §b" + cat + ": §7(" + catTotal + " total)"));
                
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(catItems.entrySet());
                sorted.sort((a, b) -> b.getValue().compareTo(a.getValue())); 
                
                for (Map.Entry<String, Integer> item : sorted) {
                    source.sendFeedback(Text.literal("    §f" + item.getValue() + "x " + item.getKey()));
                }
            }
        }
    }

    private static void printMythical(FabricClientCommandSource source, Map<String, Integer> weights) {
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

        source.sendFeedback(Text.literal("§emythical fish - §7(" + totalMythicals + " total)"));
        if (totalMythicals == 0) return;

        for (String rarity : new String[]{"common", "uncommon", "rare", "ultra rare"}) {
            if (rarityWeights.containsKey(rarity)) {
                int rTotal = rarityTotals.get(rarity);
                source.sendFeedback(Text.literal("  §b" + rarity + ": §7(" + rTotal + " total)"));
                
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(rarityWeights.get(rarity).entrySet());
                sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                
                for (Map.Entry<String, Integer> item : sorted) {
                    source.sendFeedback(Text.literal("    §f" + item.getValue() + "x " + item.getKey()));
                }
            }
        }
    }
}

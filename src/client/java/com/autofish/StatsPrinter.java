package com.autofish;

import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import java.util.*;

public class StatsPrinter {

    private static final List<String> MYTHICAL_ORDER = Arrays.asList(
        "ember of helios", "dust of selene", "shadow of nyx", "heart of aphrodite",
        "spark of zeus", "spirit of demeter", "automaton of daedalus", "wrath of hades"
    );
    
    private static final List<String> VALID_FILTERS = Arrays.asList(
        "all", "water", "lava", "ice", "mythical", "creatures", "coins"
    );

    public static void print(FabricClientCommandSource source, String period, String filter) {
        period = period.toLowerCase();
        filter = filter.toLowerCase();

        if (!period.equals("lifetime") && !period.equals("session")) {
            source.sendFeedback(Text.literal("§cInvalid period. Use 'lifetime' or 'session'."));
            return;
        }
        
        if (!VALID_FILTERS.contains(filter)) {
            source.sendFeedback(Text.literal("§cInvalid category. Use 'all', 'water', 'lava', 'ice', 'mythical', 'creatures', or 'coins'."));
            return;
        }

        boolean isLifetime = period.equals("lifetime");
        Map<String, Integer> items = isLifetime ? AutoFishStats.INSTANCE.lifetimeItems : AutoFishStats.INSTANCE.sessionItems;
        Map<String, Integer> values = isLifetime ? AutoFishStats.INSTANCE.lifetimeItemValues : AutoFishStats.INSTANCE.sessionItemValues;
        Map<String, Integer> weights = isLifetime ? AutoFishStats.INSTANCE.lifetimeMythicalWeights : AutoFishStats.INSTANCE.sessionMythicalWeights;

        source.sendFeedback(Text.literal("§8[§bAutoFish Stats: " + period.toUpperCase() + "§8]"));

        if (filter.equals("mythical")) {
            printMythical(source, weights);
        } else if (filter.equals("creatures")) {
            printEnvironment(source, items, values, "creatures", true);
        } else if (filter.equals("coins")) {
            printCoins(source, items, values);
        } else {
            if (filter.equals("all") || filter.equals("water")) printEnvironment(source, items, values, "water", false);
            if (filter.equals("all") || filter.equals("lava")) printEnvironment(source, items, values, "lava", false);
            if (filter.equals("all") || filter.equals("ice")) printEnvironment(source, items, values, "ice", false);
        }
    }

    private static void printEnvironment(FabricClientCommandSource source, Map<String, Integer> items, Map<String, Integer> values, String targetEnv, boolean onlyCreatures) {
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
                sorted.sort((a, b) -> {
                    String nameA = a.getKey();
                    String nameB = b.getKey();
                    AutoFishTracker.CategoryData dataA = AutoFishTracker.ITEM_DATA.get(nameA);
                    AutoFishTracker.CategoryData dataB = AutoFishTracker.ITEM_DATA.get(nameB);

                    boolean isGlobalA = dataA != null && dataA.environment.equals("global");
                    boolean isGlobalB = dataB != null && dataB.environment.equals("global");
                    
                    boolean isSharedA = dataA != null && dataA.environment.equals("water/ice");
                    boolean isSharedB = dataB != null && dataB.environment.equals("water/ice");

                    // Globals at the very bottom
                    if (isGlobalA && !isGlobalB) return 1;
                    if (!isGlobalA && isGlobalB) return -1;
                    
                    // Shared at the bottom, but above globals
                    if (isSharedA && !isSharedB) return 1;
                    if (!isSharedA && isSharedB) return -1;

                    // Otherwise sort by quantity descending
                    return b.getValue().compareTo(a.getValue());
                });
                
                for (Map.Entry<String, Integer> item : sorted) {
                    String name = item.getKey();
                    int count = item.getValue();
                    
                    String suffix = "";
                    AutoFishTracker.CategoryData itemData = AutoFishTracker.ITEM_DATA.get(name);
                    if (itemData != null) {
                        if (itemData.environment.equals("water/ice")) suffix = " §7(water & ice)";
                        else if (itemData.environment.equals("global")) suffix = " §7(water, lava, & ice)";
                    }
                    
                    if (values != null && values.containsKey(name)) {
                        source.sendFeedback(Text.literal("    §f" + count + "x - " + values.get(name) + " " + name + suffix));
                    } else {
                        source.sendFeedback(Text.literal("    §f" + count + "x " + name + suffix));
                    }
                }
            }
        }
    }

    private static void printCoins(FabricClientCommandSource source, Map<String, Integer> items, Map<String, Integer> values) {
        source.sendFeedback(Text.literal("§ecoins breakdown -"));
        boolean found = false;
        
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(items.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        for (Map.Entry<String, Integer> entry : sorted) {
            String name = entry.getKey();
            if (name.endsWith("coins") && !name.equals("game coins")) {
                int count = entry.getValue();
                int val = values.getOrDefault(name, 0);
                source.sendFeedback(Text.literal("  §f" + count + "x - " + val + " " + name + " §7(water, lava, & ice)"));
                found = true;
            }
        }
        
        if (!found) source.sendFeedback(Text.literal("  §7(No coins caught yet)"));
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
                sorted.sort((a, b) -> {
                    String[] partsA = a.getKey().split("kg ", 2);
                    String[] partsB = b.getKey().split("kg ", 2);
                    
                    if (partsA.length < 2 || partsB.length < 2) return b.getValue().compareTo(a.getValue());
                    
                    String nameA = partsA[1];
                    String nameB = partsB[1];
                    
                    int indexA = MYTHICAL_ORDER.indexOf(nameA);
                    int indexB = MYTHICAL_ORDER.indexOf(nameB);
                    
                    if (indexA != indexB && indexA != -1 && indexB != -1) {
                        return Integer.compare(indexA, indexB);
                    }
                    
                    try {
                        int weightA = Integer.parseInt(partsA[0]);
                        int weightB = Integer.parseInt(partsB[0]);
                        return Integer.compare(weightB, weightA);
                    } catch (NumberFormatException e) {
                        return b.getValue().compareTo(a.getValue());
                    }
                });
                
                for (Map.Entry<String, Integer> item : sorted) {
                    source.sendFeedback(Text.literal("    §f" + item.getValue() + "x " + item.getKey()));
                }
            }
        }
    }
}

package com.autofish;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

public class AutoFishHud {

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            // Only draw the HUD if the mod is running
            if (AutoFishClient.INSTANCE == null || !AutoFishClient.INSTANCE.enabled) return;
            
            MinecraftClient client = MinecraftClient.getInstance();
            
            // The modern, safe way to check if the F3 menu is open
            if (client.getDebugHud().shouldShowDebugHud()) return;

            TextRenderer renderer = client.textRenderer;
            
            // Set starting X and Y coordinates (top-left corner with a small 5-pixel margin)
            int x = 5;
            int y = 5;
            int color = 0xFFFFFF; // Pure white text

            // Draw the lines of text with a standard drop shadow
            drawContext.drawTextWithShadow(renderer, "§bAutoFish §a[ON]", x, y, color);
            drawContext.drawTextWithShadow(renderer, "§7State: §f" + AutoFishClient.INSTANCE.state.name(), x, y + 10, color);
            drawContext.drawTextWithShadow(renderer, "§7Session Catches: §f" + AutoFishStats.INSTANCE.sessionCaught, x, y + 20, color);
            
            // Only draw the mythical tracker if we actually have mythicals
            if (AutoFishStats.INSTANCE.sessionMythicals > 0) {
                drawContext.drawTextWithShadow(renderer, "§7Mythicals: §d" + AutoFishStats.INSTANCE.sessionMythicals, x, y + 30, color);
            }
        });
    }
}

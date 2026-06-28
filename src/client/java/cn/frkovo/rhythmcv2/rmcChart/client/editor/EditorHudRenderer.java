package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class EditorHudRenderer {
    private static final int HUD_PADDING = 8;

    private EditorHudRenderer() {
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }

        String shortcutText = "F GUI | R Play | T Stop | Shift+Left -10s | Shift+Right +10s";
        String progressText = RmcChartClient.getEditorState().progressText();

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        int progressWidth = client.textRenderer.getWidth(progressText);
        int shortcutWidth = client.textRenderer.getWidth(shortcutText);
        int progressBoxWidth = progressWidth + 18;
        int shortcutBoxWidth = shortcutWidth + 12;
        int progressBoxX = (width - progressBoxWidth) / 2;
        int shortcutBoxX = width - shortcutBoxWidth - HUD_PADDING;

        int progressY = 6;
        context.fill(progressBoxX - 4, progressY - 3, progressBoxX + progressBoxWidth, progressY + 12, 0x99000000);
        context.drawTextWithShadow(client.textRenderer, progressText, progressBoxX + 5, progressY, 0xFF8FE1B2);

        int shortcutY = height - 18;
        context.fill(shortcutBoxX - 6, shortcutY - 6, width - HUD_PADDING, height - HUD_PADDING, 0x88000000);
        context.drawTextWithShadow(client.textRenderer, shortcutText, shortcutBoxX, shortcutY, 0xFFE9F2FA);
    }
}

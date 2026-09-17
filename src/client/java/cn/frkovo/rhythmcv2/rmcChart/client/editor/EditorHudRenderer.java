package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.EditorTool;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.world.EditorCameraController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class EditorHudRenderer {
    private static final int HUD_PADDING = 8;

    private EditorHudRenderer() {
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null || !RmcChartClient.isEditorSessionActive()) {
            return;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        drawToolLabels(context, client, width, height);
        drawCameraLabel(context, client, width);

        if (client.currentScreen instanceof ChartEditorScreen) {
            return;
        }

        String shortcutText = "F editor | 8 Test | 9 Timeline | B camera | Left/Right seek";
        String progressText = RmcChartClient.getEditorState().progressText();
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

    private static void drawCameraLabel(DrawContext context, MinecraftClient client, int width) {
        if (!EditorCameraController.isDetached()) {
            return;
        }
        String text = "Camera: " + EditorCameraController.mode().label() + " (B to cycle)";
        int textWidth = client.textRenderer.getWidth(text);
        int boxWidth = textWidth + 12;
        int boxX = HUD_PADDING;
        int y = 6;
        context.fill(boxX - 4, y - 3, boxX + boxWidth, y + 12, 0x99000000);
        context.drawTextWithShadow(client.textRenderer, text, boxX + 2, y, 0xFFFFC46B);
    }

    private static void drawToolLabels(DrawContext context, MinecraftClient client, int width, int height) {
        int hotbarLeft = width / 2 - 91;
        int labelY = height - 32;
        EditorTool activeTool = RmcChartClient.getEditorState().activeTool();
        for (EditorTool tool : EditorTool.values()) {
            int centerX = hotbarLeft + 10 + tool.hotbarSlot() * 20;
            int color = tool == activeTool ? 0xFF7BC8FF : 0xFFE9F2FA;
            context.drawCenteredTextWithShadow(client.textRenderer, tool.shortLabel(), centerX, labelY, color);
        }
    }
}

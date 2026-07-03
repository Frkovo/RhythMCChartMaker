package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

final class SceneMapOverlayUi {
    record Button(String label, int x, int y, int width, int height) {
    }

    record Model(int x, int y, int width, int height, String titleLine, String detailLine, List<Button> buttons) {
    }

    private SceneMapOverlayUi() {
    }

    static void draw(DrawContext context, TextRenderer textRenderer, Model model, int panelColor, int borderColor, int textColor, int detailColor) {
        context.fill(model.x(), model.y(), model.x() + model.width(), model.y() + model.height(), panelColor);
        drawOutline(context, model.x(), model.y(), model.width(), model.height(), borderColor);
        context.drawText(textRenderer, Text.literal(model.titleLine()), model.x() + 6, model.y() + 6, textColor, false);
        if (model.detailLine() != null && !model.detailLine().isBlank()) {
            context.drawText(textRenderer, Text.literal(model.detailLine()), model.x() + 6, model.y() + 12, detailColor, false);
        }
        for (Button button : model.buttons()) {
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), 0x66303A44);
            drawOutline(context, button.x(), button.y(), button.width(), button.height(), borderColor);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(button.label()), button.x() + button.width() / 2, button.y() + 4, textColor);
        }
    }

    static int hitTest(Model model, double mouseX, double mouseY) {
        for (int index = 0; index < model.buttons().size(); index++) {
            Button button = model.buttons().get(index);
            if (mouseX >= button.x() && mouseX <= button.x() + button.width() && mouseY >= button.y() && mouseY <= button.y() + button.height()) {
                return index;
            }
        }
        return -1;
    }

    private static void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}

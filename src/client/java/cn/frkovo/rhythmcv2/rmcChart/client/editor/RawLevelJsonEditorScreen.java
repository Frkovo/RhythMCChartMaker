package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.chart.io.ChartProjectIo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

public class RawLevelJsonEditorScreen extends Screen {
    private static final int PADDING = 12;
    private static final int LINE_HEIGHT = 11;
    private static final int MAX_DISPLAY_LINES = 40;

    private final ChartEditorState state;
    private final Screen parent;
    private String jsonText;
    private String errorText;
    private int scrollOffset;

    public RawLevelJsonEditorScreen(ChartEditorState state, Screen parent) {
        super(Text.literal("Raw Level JSON Editor"));
        this.state = state;
        this.parent = parent;
        this.errorText = "";
        this.scrollOffset = 0;
        regenerateJson();
    }

    private void regenerateJson() {
        try {
            jsonText = ChartProjectIo.toLevelJson(state.level());
            errorText = "";
        } catch (Exception exception) {
            jsonText = "{}";
            errorText = "Failed to serialize: " + exception.getMessage();
        }
    }

    @Override
    protected void init() {
        clearChildren();
        int buttonY = height - 28;
        int buttonWidth = 100;
        int gap = 6;
        int x = PADDING;

        addDrawableChild(ButtonWidget.builder(Text.literal("Copy JSON"), b -> copyJsonToClipboard())
                .dimensions(x, buttonY, buttonWidth, 18).build());
        x += buttonWidth + gap;

        addDrawableChild(ButtonWidget.builder(Text.literal("Apply from Clipboard"), b -> applyFromClipboard())
                .dimensions(x, buttonY, 150, 18).build());
        x += 150 + gap;

        addDrawableChild(ButtonWidget.builder(Text.literal("Reload"), b -> regenerateJson())
                .dimensions(x, buttonY, buttonWidth, 18).build());
        x += buttonWidth + gap;

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> backToParent())
                .dimensions(x, buttonY, buttonWidth, 18).build());
    }

    private void copyJsonToClipboard() {
        try {
            StringSelection selection = new StringSelection(jsonText);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
            errorText = "Copied to clipboard";
        } catch (Exception exception) {
            errorText = "Copy failed: " + exception.getMessage();
        }
    }

    private void applyFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String clipboardText = (String) clipboard.getData(DataFlavor.stringFlavor);
            if (clipboardText == null || clipboardText.isBlank()) {
                errorText = "Clipboard is empty";
                return;
            }
            JsonElement parsed = JsonParser.parseString(clipboardText);
            if (!parsed.isJsonObject()) {
                errorText = "Clipboard does not contain a JSON object";
                return;
            }
            JsonObject levelObject = parsed.getAsJsonObject();
            state.applyLevelJson(levelObject);
            regenerateJson();
            errorText = "Applied and re-uploaded. Use Preview > Chart to sync to server.";
            RmcChartClient.markEditorSessionDirty();
        } catch (Exception exception) {
            errorText = "Apply failed: " + exception.getMessage();
        }
    }

    private void backToParent() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int contentX = PADDING;
        int contentY = PADDING + 14;
        int contentWidth = width - PADDING * 2;
        int contentHeight = height - 60 - PADDING;

        context.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, 0xF0101318);
        context.fill(contentX, contentY, contentX + contentWidth, contentY + 1, 0xFF48596A);
        context.fill(contentX, contentY, contentX + 1, contentY + contentHeight, 0xFF48596A);
        context.fill(contentX + contentWidth - 1, contentY, contentX + contentWidth, contentY + contentHeight, 0xFF48596A);
        context.fill(contentX, contentY + contentHeight - 1, contentX + contentWidth, contentY + contentHeight, 0xFF48596A);

        context.drawText(textRenderer, Text.literal("Raw Level JSON (read-only display, edit via clipboard)"),
                contentX + 4, PADDING, 0xFF7BC8FF, false);

        String[] lines = jsonText.split("\n");
        int maxLines = Math.min(lines.length, MAX_DISPLAY_LINES);
        context.enableScissor(contentX + 2, contentY + 2, contentX + contentWidth - 2, contentY + contentHeight - 2);
        for (int i = 0; i < maxLines; i++) {
            int lineIndex = i + scrollOffset;
            if (lineIndex >= lines.length) break;
            int y = contentY + 4 + i * LINE_HEIGHT;
            if (y > contentY + contentHeight - LINE_HEIGHT) break;
            String line = lines[lineIndex];
            if (line.length() > 120) {
                line = line.substring(0, 117) + "...";
            }
            context.drawText(textRenderer, Text.literal(line), contentX + 6, y, 0xFFE9F2FA, false);
        }
        context.disableScissor();

        int statusY = contentY + contentHeight + 4;
        int errorColor = errorText.startsWith("Applied") || errorText.startsWith("Copied") ? 0xFF8FE1B2 : 0xFFFFD37A;
        context.drawText(textRenderer, Text.literal(errorText), contentX, statusY, errorColor, false);

        context.drawText(textRenderer, Text.literal("Lines " + (scrollOffset + 1) + "-" + Math.min(scrollOffset + maxLines, lines.length) + " / " + lines.length),
                width - 120, statusY, 0xFF6F8394, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int) verticalAmount * 3;
        scrollOffset = Math.max(0, scrollOffset);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        boolean ctrlDown = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
        if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
            backToParent();
            return true;
        }
        if (ctrlDown && keyCode == InputUtil.GLFW_KEY_C) {
            copyJsonToClipboard();
            return true;
        }
        if (ctrlDown && (keyCode == InputUtil.GLFW_KEY_ENTER || keyCode == InputUtil.GLFW_KEY_V)) {
            applyFromClipboard();
            return true;
        }
        if (ctrlDown && keyCode == InputUtil.GLFW_KEY_R) {
            regenerateJson();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

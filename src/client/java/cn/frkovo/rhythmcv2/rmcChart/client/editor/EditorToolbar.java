package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

class EditorToolbar {
    private final ChartEditorScreen screen;
    final List<ToolbarActionButton> toolbarActionButtons = new ArrayList<>();
    final List<ButtonWidget> toolbarMenuTabs = new ArrayList<>();

    EditorToolbar(ChartEditorScreen screen) {
        this.screen = screen;
    }

    void clearToolbar() {
        toolbarActionButtons.clear();
        toolbarMenuTabs.clear();
    }

    void addTopButtons(int y) {
        ToolbarMenu[] menus = ToolbarMenu.values();
        int tabX = 10;
        for (ToolbarMenu menu : menus) {
            ButtonWidget tab = ButtonWidget.builder(Text.literal(menu.label), b -> {
                screen.activeToolbarMenu = screen.activeToolbarMenu == menu ? ToolbarMenu.FILE : menu;
                updateToolbarButtons();
            }).dimensions(tabX, y, 70, 18).build();
            screen.publicAddDrawableChild(tab);
            toolbarMenuTabs.add(tab);
            tabX += 74;
        }
        int actionX = tabX + 10;
        int actionY = y - 1;
        createToolbarAction(ToolbarMenu.FILE, actionX, actionY, 60, "New", b -> screen.actions.newProject());
        createToolbarAction(ToolbarMenu.FILE, actionX + 64, actionY, 60, "Open", b -> screen.actions.openProjectBrowser());
        createToolbarAction(ToolbarMenu.FILE, actionX + 128, actionY, 60, "Save", b -> screen.actions.saveProject());
        createToolbarAction(ToolbarMenu.FILE, actionX + 192, actionY, 60, "Audio", b -> screen.actions.reloadAudio());
    }

    void createToolbarAction(ToolbarMenu menu, int x, int y, int width, String label, ButtonWidget.PressAction action) {
        createToolbarAction(menu, x, y, width, label, action, false);
    }

    void createToolbarAction(ToolbarMenu menu, int x, int y, int width, String label, ButtonWidget.PressAction action, boolean worldOnly) {
        ButtonWidget button = ButtonWidget.builder(Text.literal(label), action).dimensions(x, y, width, 18).build();
        screen.publicAddDrawableChild(button);
        toolbarActionButtons.add(new ToolbarActionButton(menu, button, worldOnly));
    }

    void updateToolbarButtons() {
        MinecraftClient client = MinecraftClient.getInstance();
        for (ToolbarActionButton actionButton : toolbarActionButtons) {
            boolean visible = actionButton.menu == screen.activeToolbarMenu && (!actionButton.worldOnly || (client != null && client.world != null));
            actionButton.button.visible = visible;
            actionButton.button.active = visible;
        }
    }

    void addDifficultyButtons(int y) {
        ChartDifficulty[] difficulties = ChartDifficulty.values();
        int buttonWidth = (screen.publicWidth() - 74) / difficulties.length;
        for (int i = 0; i < difficulties.length; i++) {
            ChartDifficulty difficulty = difficulties[i];
            int x = 74 + i * buttonWidth;
            ButtonWidget button = ButtonWidget.builder(Text.literal(difficulty.displayName()), b -> {
                screen.state.setActiveDifficulty(difficulty);
                screen.propertyPanel.populateFieldsFromSelection();
                screen.propertyPanel.layoutPropertyFields();
            }).dimensions(x, y, buttonWidth - 2, 18).build();
            screen.publicAddDrawableChild(button);
        }
    }
}

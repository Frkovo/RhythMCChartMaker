package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-bar widgets: project actions (New/Open/Save/Audio) on the left and a
 * compact difficulty segmented control right after them. No menu-tab system;
 * every button is a real, always-visible action.
 */
class EditorToolbar {
    private static final int BUTTON_HEIGHT = 18;
    private static final int ACTION_WIDTH = 56;
    private static final int BUTTON_GAP = 4;
    private static final int SECTION_GAP = 14;
    private static final int MIN_DIFFICULTY_WIDTH = 46;
    private static final int MAX_DIFFICULTY_WIDTH = 72;

    private final ChartEditorScreen screen;
    private final List<DifficultyButton> difficultyButtons = new ArrayList<>();

    EditorToolbar(ChartEditorScreen screen) {
        this.screen = screen;
    }

    void clearToolbar() {
        difficultyButtons.clear();
    }

    /** @return the x position where the difficulty selector may start */
    int addTopButtons(int y) {
        int x = ChartEditorScreen.OUTER_PADDING;
        x = addAction(x, y, "New", b -> screen.actions.openNewProjectWizard());
        x = addAction(x, y, "Open", b -> screen.actions.openProjectBrowser());
        x = addAction(x, y, "Save", b -> screen.actions.saveProject());
        x = addAction(x, y, "Audio", b -> screen.actions.reloadAudio());
        return x + SECTION_GAP;
    }

    private int addAction(int x, int y, String label, ButtonWidget.PressAction action) {
        screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal(label), action)
                .dimensions(x, y, ACTION_WIDTH, BUTTON_HEIGHT).build());
        return x + ACTION_WIDTH + BUTTON_GAP;
    }

    void addDifficultyButtons(int y, int startX, int endX) {
        ChartDifficulty[] difficulties = ChartDifficulty.values();
        int gaps = BUTTON_GAP * (difficulties.length - 1);
        int buttonWidth = EditorUtils.clamp((endX - startX - gaps) / difficulties.length, MIN_DIFFICULTY_WIDTH, MAX_DIFFICULTY_WIDTH);
        int x = startX;
        for (ChartDifficulty difficulty : difficulties) {
            ButtonWidget button = ButtonWidget.builder(Text.literal(difficulty.displayName()), b -> {
                screen.state.setActiveDifficulty(difficulty);
                screen.propertyPanel.populateFieldsFromSelection();
                screen.propertyPanel.layoutPropertyFields();
                refreshDifficultyButtons();
            }).dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build();
            screen.publicAddDrawableChild(button);
            difficultyButtons.add(new DifficultyButton(difficulty, button));
            x += buttonWidth + BUTTON_GAP;
        }
        refreshDifficultyButtons();
    }

    void refreshDifficultyButtons() {
        for (DifficultyButton entry : difficultyButtons) {
            boolean active = screen.state.activeDifficulty() == entry.difficulty();
            entry.button().setMessage(Text.literal(entry.difficulty().displayName())
                    .formatted(active ? Formatting.AQUA : Formatting.GRAY));
        }
    }

    private record DifficultyButton(ChartDifficulty difficulty, ButtonWidget button) {
    }
}

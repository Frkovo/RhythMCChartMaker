package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartCompatibilityValidator;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartMath;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.EasingFunctions;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.LevelData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.MetaData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.SongManifestData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.Vec3Data;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ChartEditorScreen extends Screen {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int OUTER_PADDING = 10;
    private static final int PANEL_GAP = 8;
    private static final int TOP_BAR_HEIGHT = 86;
    private static final int STATUS_BAR_HEIGHT = 20;
    private static final int MIN_LEFT_PANEL_WIDTH = 220;
    private static final int MAX_LEFT_PANEL_WIDTH = 280;
    private static final int MIN_RIGHT_PANEL_WIDTH = 300;
    private static final int MAX_RIGHT_PANEL_WIDTH = 380;
    private static final int MIN_PREVIEW_HEIGHT = 132;
    private static final int MAX_PREVIEW_HEIGHT = 210;
    private static final int BASE_ROW_HEIGHT = 24;
    private static final int TRACK_HEADER_HEIGHT = 24;
    private static final int NOTE_LANE_HEIGHT = 58;
    private static final int EVENT_LANE_HEIGHT = 30;
    private static final int TIMELINE_LABEL_WIDTH = 132;
    private static final int TIMELINE_RULER_HEIGHT = 22;
    private static final int TIMELINE_AUDIO_STRIP_HEIGHT = 42;
    private static final int TIMELINE_ZOOM_BAR_WIDTH = 126;
    private static final int TIMELINE_ZOOM_BAR_HEIGHT = 8;
    private static final int TRACK_BAR_HEIGHT = 26;
    private static final int MENU_TAB_WIDTH = 70;
    private static final int EVENT_DRAG_WIDTH = 14;
    private static final int EVENT_CELL_WIDTH = 30;
    private static final int EVENT_EASE_WIDTH = 72;
    private static final int EVENT_COPY_WIDTH = 22;
    private static final int EVENT_REMOVE_WIDTH = 18;
    private static final int SMOOTHING_POPUP_WIDTH = 360;
    private static final int SMOOTHING_POPUP_HEIGHT = 210;
    private static final double NOTE_LANE_DEFAULT_RANGE = 2.0;
    private static final double NOTE_BEAT_EPSILON = 1.0E-5;
    private static final int UI_BG = 0xF0101318;
    private static final int UI_TOP_BG = 0xF2182028;
    private static final int UI_PANEL = 0xD71B222B;
    private static final int UI_PANEL_ALT = 0xC4161C23;
    private static final int UI_PANEL_STRONG = 0xEA202A34;
    private static final int UI_BORDER = 0xFF48596A;
    private static final int UI_BORDER_SOFT = 0x7748596A;
    private static final int UI_TEXT = 0xFFE9F2FA;
    private static final int UI_MUTED = 0xFF9CB1C2;
    private static final int UI_DIM = 0xFF6F8394;
    private static final int UI_ACCENT = 0xFF7BC8FF;
    private static final int UI_GREEN = 0xFF8FE1B2;
    private static final int UI_WARN = 0xFFFFD37A;

    private final ChartEditorState state;
    private final List<LabeledField> songFields = new ArrayList<>();
    private final List<LabeledField> metaFields = new ArrayList<>();
    private final List<LabeledField> trackFields = new ArrayList<>();
    private final List<LabeledField> noteFields = new ArrayList<>();
    private final List<LabeledField> effectFields = new ArrayList<>();
    private final List<LabeledField> bpmFields = new ArrayList<>();

    private TextFieldWidget pathField;
    private ButtonWidget applyButton;
    private ButtonWidget resetButton;
    private TextFieldWidget smoothingDivisionField;
    private ButtonWidget smoothingKindButton;
    private ButtonWidget smoothingNoteTypeButton;
    private ButtonWidget smoothingFillButton;
    private ButtonWidget smoothingApplyButton;
    private ButtonWidget smoothingCancelButton;

    private EditorSelection lastSelection = null;
    private DragMode dragMode = DragMode.NONE;
    private int propertyScroll = 0;
    private int trackScrollY = 0;
    private SelectedEventHandle selectedEventHandle;
    private SelectedEventClip selectedEventClip;
    private NoteAxis activeNoteAxis = NoteAxis.X;
    private final LinkedHashSet<SelectedNote> selectedNotes = new LinkedHashSet<>();
    private final LinkedHashSet<SelectedEventClip> selectedEventClips = new LinkedHashSet<>();
    private SelectionBox selectionBox;
    private boolean selectionBoxAdditive;
    private double snapGuideBeat = Double.NaN;
    private final Map<SelectedNote, NoteDragSnapshot> noteDragSnapshots = new HashMap<>();
    private final Map<SelectedEventClip, EventClipDragSnapshot> eventClipDragSnapshots = new HashMap<>();
    private final Set<String> collapsedPropertySections = new LinkedHashSet<>();
    private final List<PropertySectionHeader> propertySectionHeaders = new ArrayList<>();
    private final List<TrackEventEditor> trackEventEditors = new ArrayList<>();
    private final List<ToolbarActionButton> toolbarActionButtons = new ArrayList<>();
    private final List<ButtonWidget> toolbarMenuTabs = new ArrayList<>();
    private final LinkedHashSet<TrackEventRow> selectedTrackEventRows = new LinkedHashSet<>();
    private final Deque<ChartEditorState.EditorSnapshot> undoSnapshots = new ArrayDeque<>();
    private final Deque<ChartEditorState.EditorSnapshot> redoSnapshots = new ArrayDeque<>();
    private double dragAnchorBeat;
    private double dragAnchorAxisValue;
    private SelectedNote draggingHoldLengthNote;
    private double dragAnchorHoldLength;
    private TrackEventEditor draggingTrackEventEditor;
    private int draggingTrackEventSourceIndex = -1;
    private int draggingTrackEventTargetIndex = -1;
    private double draggingTrackEventMouseY;
    private TrackEventRow easingPopupRow;
    private int easingPopupX;
    private int easingPopupY;
    private TextFieldWidget easingPopupSearchField;
    private SelectionBox trackEventSelectionBox;
    private boolean trackEventSelectionAdditive;
    private NoteClipboard noteClipboard;
    private int lastSnapshotRevision = -1;
    private boolean applyingHistorySnapshot;
    private ToolbarMenu activeToolbarMenu = ToolbarMenu.FILE;
    private boolean smoothingPopupOpen;
    private boolean smoothingUseCatmull = true;
    private boolean smoothingUseCircle = false;
    private EasingType smoothingEasingType = EasingType.LINEAR;
    private NoteType smoothingNoteType = NoteType.TAP;
    private boolean smoothingFillEnabled = true;

    public ChartEditorScreen(ChartEditorState state) {
        super(Text.literal("RhythMC Chart Maker"));
        this.state = state;
    }

    private int leftPanelWidth() {
        return clamp((int) Math.round(width * 0.19), MIN_LEFT_PANEL_WIDTH, MAX_LEFT_PANEL_WIDTH);
    }

    private int rightPanelWidth() {
        return clamp((int) Math.round(width * 0.27), MIN_RIGHT_PANEL_WIDTH, MAX_RIGHT_PANEL_WIDTH);
    }

    private int rightPanelInnerWidth() {
        return rightPanelWidth() - 16;
    }

    private int rightPanelX() {
        return width - rightPanelWidth() - OUTER_PADDING;
    }

    private int contentTopY() {
        return TOP_BAR_HEIGHT;
    }

    private int previewHeight() {
        return clamp((int) Math.round(height * 0.24), MIN_PREVIEW_HEIGHT, MAX_PREVIEW_HEIGHT);
    }

    private EditorLayout editorLayout() {
        int topY = contentTopY();
        int leftX = OUTER_PADDING;
        int leftWidth = leftPanelWidth();
        int rightWidth = rightPanelWidth();
        int rightX = width - rightWidth - OUTER_PADDING;
        int centerX = leftX + leftWidth + PANEL_GAP;
        int centerWidth = Math.max(96, rightX - centerX - PANEL_GAP);
        int panelHeight = Math.max(96, height - topY - STATUS_BAR_HEIGHT - OUTER_PADDING);
        int previewHeight = Math.min(previewHeight(), Math.max(72, panelHeight / 2));
        int timelineY = topY + previewHeight + PANEL_GAP;
        int timelineHeight = Math.max(TIMELINE_RULER_HEIGHT + TIMELINE_AUDIO_STRIP_HEIGHT + 48, height - timelineY - TRACK_BAR_HEIGHT - STATUS_BAR_HEIGHT - OUTER_PADDING - 4);
        int trackBarY = timelineY + timelineHeight + 4;
        return new EditorLayout(leftX, topY, leftWidth, rightX, rightWidth, centerX, centerWidth, panelHeight, previewHeight, timelineY, timelineHeight, trackBarY);
    }

    @Override
    protected void init() {
        clearChildren();
        songFields.clear();
        metaFields.clear();
        trackFields.clear();
        noteFields.clear();
        effectFields.clear();
        bpmFields.clear();
        trackEventEditors.clear();
        toolbarActionButtons.clear();
        toolbarMenuTabs.clear();
        propertySectionHeaders.clear();
        selectedTrackEventRows.clear();
        trackEventSelectionBox = null;
        easingPopupRow = null;

        int topY = 8;
        pathField = addDrawableChild(new TextFieldWidget(textRenderer, 112, topY, Math.max(120, width - 392), 18, Text.literal("Project Path")));
        pathField.setMaxLength(512);
        pathField.setText(state.project().projectPath().toString());
        createSmoothingPopupWidgets();

        int buttonY = 34;
        addTopButtons(buttonY);
        addDifficultyButtons(58);
        createPropertyFields();
        populateFieldsFromSelection();
        layoutPropertyFields();
        if (undoSnapshots.isEmpty()) {
            resetHistorySnapshots();
        } else {
            lastSnapshotRevision = state.revision();
        }
    }

    @Override
    public void tick() {
        super.tick();
        state.tick();
        captureHistorySnapshotIfNeeded();
        if (!state.selection().equals(lastSelection)) {
            propertyScroll = 0;
            selectedTrackEventRows.clear();
            easingPopupRow = null;
            if (easingPopupSearchField != null) {
                easingPopupSearchField.setText("");
                easingPopupSearchField.visible = false;
                easingPopupSearchField.active = false;
                easingPopupSearchField.setFocused(false);
            }
            populateFieldsFromSelection();
            layoutPropertyFields();
            lastSelection = state.selection();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        EditorLayout layout = editorLayout();

        drawEditorChrome(context);
        drawLeftPanel(context, layout.leftX(), layout.topY(), layout.leftWidth(), layout.panelHeight(), mouseX, mouseY);
        drawPreviewPanel(context, layout.centerX(), layout.topY(), layout.centerWidth(), layout.previewHeight());
        drawTimeline(context, layout.centerX(), layout.timelineY(), layout.centerWidth(), layout.timelineHeight(), mouseX, mouseY);
        drawTrackBar(context, layout.centerX(), layout.trackBarY(), layout.centerWidth(), TRACK_BAR_HEIGHT, mouseX, mouseY);
        drawPropertyPanel(context, layout.rightX(), layout.topY(), layout.rightWidth(), layout.panelHeight());
        drawStatusBar(context);
        drawSmoothingPopupBackground(context);

        super.render(context, mouseX, mouseY, delta);
        drawTrackEventEasingPopup(context, mouseX, mouseY);
        drawSmoothingPopupText(context);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (smoothingPopupOpen) {
            if (isInside(mouseX, mouseY, smoothingPopupX(), smoothingPopupY(), SMOOTHING_POPUP_WIDTH, SMOOTHING_POPUP_HEIGHT)) {
                return super.mouseClicked(click, doubleClick) || true;
            }
            return true;
        }
        if (handleTrackEventEasingPopupClick(mouseX, mouseY)) {
            return true;
        }
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        EditorLayout layout = editorLayout();

        if (isInside(mouseX, mouseY, layout.leftX(), layout.topY(), layout.leftWidth(), layout.panelHeight())) {
            if (handleLeftPanelClick(mouseX, mouseY, layout.leftX(), layout.topY())) {
                return true;
            }
        }
        if (isInside(mouseX, mouseY, layout.centerX(), layout.topY(), layout.centerWidth(), layout.previewHeight())) {
            if (handlePreviewClick(click, layout.centerX(), layout.topY(), layout.centerWidth(), layout.previewHeight())) {
                return true;
            }
        }
        if (isInside(mouseX, mouseY, layout.centerX(), layout.timelineY(), layout.centerWidth(), layout.timelineHeight())) {
            if (handleTimelineClick(click, layout.centerX(), layout.timelineY(), layout.centerWidth(), layout.timelineHeight())) {
                return true;
            }
        }
        if (isInside(mouseX, mouseY, layout.centerX(), layout.trackBarY(), layout.centerWidth(), TRACK_BAR_HEIGHT)) {
            if (handleTrackBarClick(mouseX, mouseY, layout.centerX(), layout.trackBarY(), layout.centerWidth(), TRACK_BAR_HEIGHT)) {
                return true;
            }
        }
        if (isInside(mouseX, mouseY, layout.rightX(), layout.topY(), layout.rightWidth(), layout.panelHeight())) {
            if (handlePropertyPanelClick(click, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragMode != DragMode.NONE) {
            if (dragMode == DragMode.WORLD_SELECTION) {
                if (client != null && client.world != null) {
                    RmcChartClient.getWorldLauncher().dragSelection(deltaX, deltaY, click.button(), isShiftDown());
                    populateFieldsFromSelection();
                }
            } else if (dragMode == DragMode.TRACK_EVENT_BOX_SELECT) {
                trackEventSelectionBox = trackEventSelectionBox == null
                        ? new SelectionBox((int) click.x(), (int) click.y(), (int) click.x(), (int) click.y())
                        : new SelectionBox(trackEventSelectionBox.startX(), trackEventSelectionBox.startY(), (int) click.x(), (int) click.y());
            } else if (dragMode == DragMode.TRACK_EVENT_ROW) {
                draggingTrackEventMouseY = click.y();
                updateDraggedTrackEventTarget(click.y());
            } else {
                double mouseX = click.x();
                double mouseY = click.y();
                handleTimelineDrag(mouseX, mouseY);
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragMode == DragMode.BOX_SELECT && selectionBox != null) {
            applySelectionBox();
        }
        if (dragMode == DragMode.TRACK_EVENT_BOX_SELECT && trackEventSelectionBox != null) {
            applyTrackEventSelectionBox();
        }
        if (dragMode == DragMode.TRACK_EVENT_ROW) {
            applyDraggedTrackEventReorder();
        }
        dragMode = DragMode.NONE;
        selectionBox = null;
        snapGuideBeat = Double.NaN;
        noteDragSnapshots.clear();
        eventClipDragSnapshots.clear();
        draggingTrackEventEditor = null;
        draggingTrackEventSourceIndex = -1;
        draggingTrackEventTargetIndex = -1;
        draggingHoldLengthNote = null;
        dragAnchorHoldLength = 0.0;
        trackEventSelectionBox = null;
        captureHistorySnapshotIfNeeded();
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        EditorLayout layout = editorLayout();
        if (isInside(mouseX, mouseY, layout.rightX(), layout.topY(), layout.rightWidth(), layout.panelHeight())) {
            propertyScroll += (int) Math.round(verticalAmount * 16.0);
            propertyScroll = clamp(propertyScroll, -1600, 0);
            layoutPropertyFields();
            return true;
        }
        if (isInside(mouseX, mouseY, layout.centerX(), layout.timelineY(), layout.centerWidth(), layout.timelineHeight())) {
            if (isControlDown()) {
                state.zoom(verticalAmount > 0 ? 0.9 : 1.1);
                state.setStatus("Timeline zoom: " + format(state.beatsPerScreen()) + " beats");
            } else if (isShiftDown()) {
                state.scrollWindow(-verticalAmount * (state.beatsPerScreen() / 8.0));
            } else {
                int contentHeight = layout.timelineHeight() - TIMELINE_RULER_HEIGHT - TIMELINE_AUDIO_STRIP_HEIGHT;
                int maxScroll = Math.max(0, totalTimelineLaneHeight() - contentHeight);
                trackScrollY += (int) Math.round(verticalAmount * 32.0);
                trackScrollY = clamp(trackScrollY, 0, maxScroll);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        if (smoothingPopupOpen) {
            if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
                closeSmoothingPopup();
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_ENTER || keyCode == InputUtil.GLFW_KEY_KP_ENTER) {
                applySmoothingFromPopup();
                return true;
            }
            if (super.keyPressed(keyInput)) {
                return true;
            }
            return true;
        }
        if (isControlDown() && keyCode == InputUtil.GLFW_KEY_3) {
            openSmoothingPopup();
            return true;
        }
        if (super.keyPressed(keyInput)) {
            return true;
        }
        if (isControlDown()) {
            if (keyCode == InputUtil.GLFW_KEY_Z) {
                if (isShiftDown()) {
                    redoEditorChange();
                } else {
                    undoEditorChange();
                }
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_Y) {
                redoEditorChange();
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_A) {
                selectAllNotes();
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_C) {
                copySelectedNotesToClipboard();
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_X) {
                cutSelectedNotesToClipboard();
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_V) {
                pasteClipboardNotes();
                return true;
            }
        }
        if (isAltDown() && hasSelectedNotes()) {
            if (keyCode == InputUtil.GLFW_KEY_LEFT) {
                moveSelectedNotesByShortcut(-noteShortcutBeatStep(), 0);
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_RIGHT) {
                moveSelectedNotesByShortcut(noteShortcutBeatStep(), 0);
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_UP) {
                moveSelectedNotesByShortcut(0.0, -1);
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_DOWN) {
                moveSelectedNotesByShortcut(0.0, 1);
                return true;
            }
        }
        if (!isControlDown() && !isAltDown()) {
            if (getFocused() instanceof TextFieldWidget) {
                return false;
            }
            if (keyCode == InputUtil.GLFW_KEY_1) {
                createNoteFromShortcut(NoteType.TAP);
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_2) {
                createNoteFromShortcut(NoteType.LOOK);
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_3) {
                createNoteFromShortcut(NoteType.HOLD);
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_4) {
                createNoteFromShortcut(NoteType.DODGE);
                return true;
            }
        }
        if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
            clearTimelineSelections();
            if (state.selection().kind() == EditorSelection.Kind.NOTE) {
                state.setSelection(EditorSelection.track(state.selection().track()));
            }
            return true;
        }
        if (keyCode == InputUtil.GLFW_KEY_SPACE) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (RmcChartClient.getWorldLauncher().isEditorWorldActive(client)) {
                RmcChartClient.getWorldLauncher().toggleWorldPlayback();
            } else {
                state.togglePlayback();
            }
            return true;
        }
        if (keyCode == InputUtil.GLFW_KEY_DELETE || keyCode == InputUtil.GLFW_KEY_BACKSPACE) {
            deleteCurrentSelection();
            return true;
        }
        if (keyCode == InputUtil.GLFW_KEY_RIGHT) {
            state.seekToBeat(state.playheadBeat() + 0.25);
            return true;
        }
        if (keyCode == InputUtil.GLFW_KEY_LEFT) {
            state.seekToBeat(state.playheadBeat() - 0.25);
            return true;
        }
        if (isControlDown() && keyCode == InputUtil.GLFW_KEY_S) {
            saveProject();
            return true;
        }
        if (isControlDown() && keyCode == InputUtil.GLFW_KEY_O) {
            loadProject();
            return true;
        }
        return false;
    }

    private void deleteCurrentSelection() {
        if (deleteSelectedTrackEventRows(null)) {
            return;
        }
        int deletedNotes = deleteSelectedNotes();
        int deletedClips = deleteSelectedEventClips();
        if (deletedNotes == 0 && deletedClips == 0) {
            state.deleteSelection();
            clearTimelineSelections();
            return;
        }
        if (state.selection().note() != null && !state.selection().track().notes().contains(state.selection().note())) {
            state.setSelection(EditorSelection.track(state.selection().track()));
        }
        clearTimelineSelections();
        state.sortCurrentLevel();
        state.markDirty();
        state.setStatus("Deleted " + deletedNotes + " note(s), " + deletedClips + " event clip(s)");
        populateFieldsFromSelection();
        captureHistorySnapshotIfNeeded();
    }

    private void resetHistorySnapshots() {
        undoSnapshots.clear();
        redoSnapshots.clear();
        undoSnapshots.addLast(state.snapshot());
        lastSnapshotRevision = state.revision();
    }

    private void captureHistorySnapshotIfNeeded() {
        if (applyingHistorySnapshot || lastSnapshotRevision == state.revision()) {
            return;
        }
        undoSnapshots.addLast(state.snapshot());
        while (undoSnapshots.size() > 128) {
            undoSnapshots.removeFirst();
        }
        redoSnapshots.clear();
        lastSnapshotRevision = state.revision();
    }

    private void undoEditorChange() {
        captureHistorySnapshotIfNeeded();
        if (undoSnapshots.size() <= 1) {
            state.setStatus("Nothing to undo");
            return;
        }
        ChartEditorState.EditorSnapshot current = undoSnapshots.removeLast();
        redoSnapshots.addLast(current);
        applyHistorySnapshot(undoSnapshots.getLast(), "Undo");
    }

    private void redoEditorChange() {
        if (redoSnapshots.isEmpty()) {
            state.setStatus("Nothing to redo");
            return;
        }
        ChartEditorState.EditorSnapshot next = redoSnapshots.removeLast();
        undoSnapshots.addLast(next);
        applyHistorySnapshot(next, "Redo");
    }

    private void applyHistorySnapshot(ChartEditorState.EditorSnapshot snapshot, String label) {
        applyingHistorySnapshot = true;
        state.restoreSnapshot(snapshot);
        applyingHistorySnapshot = false;
        lastSnapshotRevision = state.revision();
        clearTimelineSelections();
        if (state.selection().note() != null && state.selection().track() != null) {
            selectedNotes.add(new SelectedNote(state.selection().track(), state.selection().note()));
        }
        populateFieldsFromSelection();
        layoutPropertyFields();
        state.setStatus(label);
    }

    private boolean hasSelectedNotes() {
        return !currentSelectedNotes().isEmpty();
    }

    private List<SelectedNote> currentSelectedNotes() {
        if (!selectedNotes.isEmpty()) {
            return new ArrayList<>(selectedNotes);
        }
        if (state.selection().track() != null && state.selection().note() != null) {
            return new ArrayList<>(List.of(new SelectedNote(state.selection().track(), state.selection().note())));
        }
        return new ArrayList<>();
    }

    private void selectAllNotes() {
        selectedEventClips.clear();
        List<SelectedNote> notes = new ArrayList<>();
        for (TrackData track : state.tracks()) {
            for (NoteData note : track.notes()) {
                notes.add(new SelectedNote(track, note));
            }
        }
        if (notes.isEmpty()) {
            state.setStatus("No notes to select");
            return;
        }
        replaceSelectedNotes(notes);
        state.setSelection(EditorSelection.note(notes.getFirst().track(), notes.getFirst().note()));
        state.setStatus("Selected " + notes.size() + " note(s)");
    }

    private void copySelectedNotesToClipboard() {
        List<SelectedNote> notes = currentSelectedNotes();
        if (notes.isEmpty()) {
            state.setStatus("No selected notes to copy");
            return;
        }
        notes.sort(Comparator.comparingInt((SelectedNote note) -> state.trackIndex(note.track())).thenComparingDouble(note -> note.note().beat()));
        int baseTrackIndex = state.trackIndex(notes.getFirst().track());
        double baseBeat = notes.stream().mapToDouble(note -> note.note().beat()).min().orElse(state.playheadBeat());
        List<NoteClipboardEntry> entries = new ArrayList<>();
        for (SelectedNote selectedNote : notes) {
            NoteData note = selectedNote.note();
            entries.add(new NoteClipboardEntry(
                    state.trackIndex(selectedNote.track()) - baseTrackIndex,
                    note.beat() - baseBeat,
                    note.noteType(),
                    note.pos().copy(),
                    note.scale().copy(),
                    note.rotation().copy(),
                    note.holdGroup(),
                    note.holdLengthBeats()
            ));
        }
        noteClipboard = new NoteClipboard(entries, baseTrackIndex, baseBeat);
        state.setStatus("Copied " + entries.size() + " note(s)");
    }

    private void cutSelectedNotesToClipboard() {
        List<SelectedNote> notes = currentSelectedNotes();
        if (notes.isEmpty()) {
            state.setStatus("No selected notes to cut");
            return;
        }
        copySelectedNotesToClipboard();
        int deleted = deleteSelectedNotes();
        if (deleted <= 0) {
            return;
        }
        clearTimelineSelections();
        selectedEventClips.clear();
        state.sortCurrentLevel();
        state.markDirty();
        if (state.selectedTrack() != null) {
            state.setSelection(EditorSelection.track(state.selectedTrack()));
        }
        populateFieldsFromSelection();
        captureHistorySnapshotIfNeeded();
        state.setStatus("Cut " + deleted + " note(s)");
    }

    private void pasteClipboardNotes() {
        if (noteClipboard == null || noteClipboard.entries().isEmpty()) {
            state.setStatus("Clipboard has no notes");
            return;
        }
        selectedEventClips.clear();
        int anchorTrackIndex = state.selectedTrack() == null ? noteClipboard.baseTrackIndex() : state.trackIndex(state.selectedTrack());
        double anchorBeat = state.playheadBeat();
        List<SelectedNote> pastedNotes = new ArrayList<>();
        for (NoteClipboardEntry entry : noteClipboard.entries()) {
            int targetTrackIndex = clamp(anchorTrackIndex + entry.trackOffset(), 0, Math.max(0, state.tracks().size() - 1));
            TrackData track = state.tracks().get(targetTrackIndex);
            NoteData note = new NoteData(entry.noteType(), anchorBeat + entry.beatOffset(), entry.pos().copy(), entry.scale().copy(), entry.rotation().copy(), entry.holdGroup(), entry.holdLengthBeats());
            track.notes().add(note);
            pastedNotes.add(new SelectedNote(track, note));
        }
        state.sortCurrentLevel();
        state.markDirty();
        replaceSelectedNotes(pastedNotes);
        if (!pastedNotes.isEmpty()) {
            state.setSelection(EditorSelection.note(pastedNotes.getFirst().track(), pastedNotes.getFirst().note()));
        }
        populateFieldsFromSelection();
        captureHistorySnapshotIfNeeded();
        state.setStatus("Pasted " + pastedNotes.size() + " note(s)");
    }

    private void moveSelectedNotesByShortcut(double beatDelta, int trackDelta) {
        List<SelectedNote> notes = currentSelectedNotes();
        if (notes.isEmpty()) {
            return;
        }
        LinkedHashSet<SelectedNote> movedNotes = new LinkedHashSet<>();
        for (SelectedNote selectedNote : notes) {
            TrackData sourceTrack = selectedNote.track();
            int targetTrackIndex = clamp(state.trackIndex(sourceTrack) + trackDelta, 0, Math.max(0, state.tracks().size() - 1));
            TrackData targetTrack = state.tracks().get(targetTrackIndex);
            NoteData note = selectedNote.note();
            note.setBeat(Math.max(-64.0, note.beat() + beatDelta));
            if (sourceTrack != targetTrack) {
                sourceTrack.notes().remove(note);
                targetTrack.notes().add(note);
            }
            movedNotes.add(new SelectedNote(targetTrack, note));
        }
        state.sortCurrentLevel();
        state.markDirty();
        replaceSelectedNotes(new ArrayList<>(movedNotes));
        if (!movedNotes.isEmpty()) {
            SelectedNote primary = movedNotes.getFirst();
            state.setSelection(EditorSelection.note(primary.track(), primary.note()));
        }
        populateFieldsFromSelection();
        captureHistorySnapshotIfNeeded();
        state.setStatus("Moved " + movedNotes.size() + " note(s)");
    }

    private double noteShortcutBeatStep() {
        return isShiftDown() ? 1.0 : 0.25;
    }

    private void openSmoothingPopup() {
        List<SelectedNote> notes = currentSelectedNotes();
        if (notes.size() < 2) {
            state.setStatus("Select at least 2 notes before smoothing");
            return;
        }
        smoothingNoteType = notes.getFirst().note().noteType();
        TrackData track = notes.getFirst().track();
        smoothingDivisionField.setText(Integer.toString(track == null ? 16 : track.beatDivision()));
        smoothingKindButton.setMessage(Text.literal("Curve: " + smoothingCurveLabel()));
        smoothingNoteTypeButton.setMessage(Text.literal("Note: " + shortNoteLabel(smoothingNoteType)));
        smoothingFillButton.setMessage(Text.literal(fillSmoothingLabel()));
        positionSmoothingPopupWidgets();
        smoothingPopupOpen = true;
        setSmoothingPopupWidgetsVisible(true);
        smoothingDivisionField.setFocused(true);
        state.setStatus("Smoothing: choose curve, note type, and fill division");
    }

    private void closeSmoothingPopup() {
        smoothingPopupOpen = false;
        setSmoothingPopupWidgetsVisible(false);
        smoothingDivisionField.setFocused(false);
    }

    private void applySmoothingFromPopup() {
        List<SelectedNote> notes = currentSelectedNotes();
        if (notes.size() < 2) {
            state.setStatus("Smoothing needs at least 2 selected notes");
            closeSmoothingPopup();
            return;
        }
        int division = clamp(parseIntOrDefault(smoothingDivisionField.getText(), 16), 1, 256);
        double step = 4.0 / division;
        Map<TrackData, List<SelectedNote>> byTrack = new LinkedHashMap<>();
        for (SelectedNote selectedNote : notes) {
            byTrack.computeIfAbsent(selectedNote.track(), ignored -> new ArrayList<>()).add(selectedNote);
        }

        LinkedHashSet<SelectedNote> smoothedSelection = new LinkedHashSet<>();
        int changed = 0;
        int filled = 0;
        for (Map.Entry<TrackData, List<SelectedNote>> entry : byTrack.entrySet()) {
            TrackData track = entry.getKey();
            List<SelectedNote> group = entry.getValue();
            group.sort(Comparator.comparingDouble(selectedNote -> selectedNote.note().beat()));
            if (track == null || group.size() < 2) {
                continue;
            }
            if (smoothingUseCircle) {
                applyCircleSelectedNotePositions(group);
            } else {
                relaxSelectedNotePositions(group);
            }
            for (SelectedNote selectedNote : group) {
                applySmoothingNoteType(selectedNote.note(), step);
                smoothedSelection.add(selectedNote);
                changed++;
            }
            if (smoothingFillEnabled) {
                double startBeat = group.getFirst().note().beat();
                double endBeat = group.getLast().note().beat();
                for (double beat = Math.ceil(startBeat / step) * step; beat <= endBeat + NOTE_BEAT_EPSILON; beat += step) {
                    if (isAnchorBeat(group, beat) || findNoteAtBeat(track, beat) != null) {
                        continue;
                    }
                    NoteData note = sampleSmoothedNote(group, beat, step);
                    track.notes().add(note);
                    smoothedSelection.add(new SelectedNote(track, note));
                    filled++;
                }
            }
        }
        if (changed == 0 && filled == 0) {
            state.setStatus("Smoothing skipped: each track needs at least 2 selected notes");
            closeSmoothingPopup();
            return;
        }
        state.sortCurrentLevel();
        state.markDirty();
        replaceSelectedNotes(new ArrayList<>(smoothedSelection));
        if (!smoothedSelection.isEmpty()) {
            SelectedNote primary = smoothedSelection.getFirst();
            state.setSelection(EditorSelection.note(primary.track(), primary.note()));
        }
        populateFieldsFromSelection();
        captureHistorySnapshotIfNeeded();
        closeSmoothingPopup();
        state.setStatus("Smoothed " + changed + " note(s), filled " + filled + " note(s) at 1/" + division);
    }

    private void relaxSelectedNotePositions(List<SelectedNote> group) {
        if (group.size() < 3) {
            return;
        }
        double strength = smoothingRelaxStrength();
        List<Vec3Data> relaxed = new ArrayList<>();
        for (int index = 0; index < group.size(); index++) {
            NoteData note = group.get(index).note();
            if (index == 0 || index == group.size() - 1) {
                relaxed.add(note.pos().copy());
                continue;
            }
            Vec3Data previous = group.get(index - 1).note().pos();
            Vec3Data current = note.pos();
            Vec3Data next = group.get(index + 1).note().pos();
            relaxed.add(new Vec3Data(
                    current.x() * (1.0 - strength) + (previous.x() + next.x()) * 0.5 * strength,
                    current.y() * (1.0 - strength) + (previous.y() + next.y()) * 0.5 * strength,
                    current.z() * (1.0 - strength) + (previous.z() + next.z()) * 0.5 * strength
            ));
        }
        for (int index = 1; index < group.size() - 1; index++) {
            Vec3Data pos = relaxed.get(index);
            NoteData note = group.get(index).note();
            note.pos().set(pos.x(), pos.y(), smoothingNoteType == NoteType.HOLD ? -1.0 : pos.z());
        }
    }

    private NoteData sampleSmoothedNote(List<SelectedNote> anchors, double beat, double step) {
        int segment = findSmoothingSegment(anchors, beat);
        NoteData left = anchors.get(segment).note();
        NoteData right = anchors.get(segment + 1).note();
        double span = Math.max(NOTE_BEAT_EPSILON, right.beat() - left.beat());
        double t = Math.max(0.0, Math.min(1.0, (beat - left.beat()) / span));
        double easedT = smoothingEase(t);
        Vec3Data pos = smoothingUseCircle
                ? sampleCirclePosition(anchors, buildSmoothingCircle(anchors), beat)
                : smoothingUseCatmull ? sampleCatmullPosition(anchors, segment, t) : lerpVec(left.pos(), right.pos(), easedT);
        Vec3Data scale = lerpVec(left.scale(), right.scale(), easedT);
        Vec3Data rotation = lerpVec(left.rotation(), right.rotation(), easedT);
        if (smoothingNoteType == NoteType.HOLD) {
            pos.set(pos.x(), pos.y(), -1.0);
        }
        NoteData note = new NoteData(smoothingNoteType, beat, pos, scale, rotation, -1, smoothingNoteType == NoteType.HOLD ? step : 0.0);
        applySmoothingNoteType(note, step);
        return note;
    }

    private int findSmoothingSegment(List<SelectedNote> anchors, double beat) {
        for (int index = 0; index < anchors.size() - 1; index++) {
            if (beat <= anchors.get(index + 1).note().beat() + NOTE_BEAT_EPSILON) {
                return index;
            }
        }
        return Math.max(0, anchors.size() - 2);
    }

    private Vec3Data sampleCatmullPosition(List<SelectedNote> anchors, int segment, double t) {
        Vec3Data p0 = anchors.get(Math.max(0, segment - 1)).note().pos();
        Vec3Data p1 = anchors.get(segment).note().pos();
        Vec3Data p2 = anchors.get(segment + 1).note().pos();
        Vec3Data p3 = anchors.get(Math.min(anchors.size() - 1, segment + 2)).note().pos();
        return new Vec3Data(
                catmull(p0.x(), p1.x(), p2.x(), p3.x(), t),
                catmull(p0.y(), p1.y(), p2.y(), p3.y(), t),
                catmull(p0.z(), p1.z(), p2.z(), p3.z(), t)
        );
    }

    private void applyCircleSelectedNotePositions(List<SelectedNote> group) {
        SmoothingCircle circle = buildSmoothingCircle(group);
        for (SelectedNote selectedNote : group) {
            Vec3Data pos = sampleCirclePosition(group, circle, selectedNote.note().beat());
            selectedNote.note().pos().set(pos.x(), pos.y(), smoothingNoteType == NoteType.HOLD ? -1.0 : pos.z());
        }
    }

    private SmoothingCircle buildSmoothingCircle(List<SelectedNote> anchors) {
        double centerX = 0.0;
        double centerY = 0.0;
        for (SelectedNote anchor : anchors) {
            centerX += anchor.note().pos().x();
            centerY += anchor.note().pos().y();
        }
        centerX /= Math.max(1, anchors.size());
        centerY /= Math.max(1, anchors.size());

        double radius = 0.0;
        double maxSpread = 0.0;
        for (SelectedNote anchor : anchors) {
            double dx = anchor.note().pos().x() - centerX;
            double dy = anchor.note().pos().y() - centerY;
            radius += Math.hypot(dx, dy);
            maxSpread = Math.max(maxSpread, Math.max(Math.abs(dx), Math.abs(dy)));
        }
        radius /= Math.max(1, anchors.size());
        radius = Math.max(radius, Math.max(1.0, maxSpread));

        Vec3Data first = anchors.getFirst().note().pos();
        double startAngle = Math.atan2(first.y() - centerY, first.x() - centerX);
        if (Double.isNaN(startAngle)) {
            startAngle = -Math.PI / 2.0;
        }
        double direction = circleSignedArea(anchors, centerX, centerY) < 0.0 ? -1.0 : 1.0;
        return new SmoothingCircle(centerX, centerY, radius, startAngle, direction * Math.PI * 2.0);
    }

    private double circleSignedArea(List<SelectedNote> anchors, double centerX, double centerY) {
        if (anchors.size() < 3) {
            return 1.0;
        }
        double area = 0.0;
        for (int index = 0; index < anchors.size(); index++) {
            Vec3Data left = anchors.get(index).note().pos();
            Vec3Data right = anchors.get((index + 1) % anchors.size()).note().pos();
            area += (left.x() - centerX) * (right.y() - centerY) - (right.x() - centerX) * (left.y() - centerY);
        }
        return area == 0.0 ? 1.0 : area;
    }

    private Vec3Data sampleCirclePosition(List<SelectedNote> anchors, SmoothingCircle circle, double beat) {
        int segment = findSmoothingSegment(anchors, beat);
        NoteData left = anchors.get(segment).note();
        NoteData right = anchors.get(segment + 1).note();
        double segmentSpan = Math.max(NOTE_BEAT_EPSILON, right.beat() - left.beat());
        double segmentT = Math.max(0.0, Math.min(1.0, (beat - left.beat()) / segmentSpan));
        double fullSpan = Math.max(NOTE_BEAT_EPSILON, anchors.getLast().note().beat() - anchors.getFirst().note().beat());
        double progress = Math.max(0.0, Math.min(1.0, (beat - anchors.getFirst().note().beat()) / fullSpan));
        double angle = circle.startAngle() + circle.sweep() * progress;
        return new Vec3Data(
                circle.centerX() + Math.cos(angle) * circle.radius(),
                circle.centerY() + Math.sin(angle) * circle.radius(),
                lerp(left.pos().z(), right.pos().z(), segmentT)
        );
    }

    private void applySmoothingNoteType(NoteData note, double step) {
        note.setNoteType(smoothingNoteType);
        if (smoothingNoteType == NoteType.HOLD) {
            note.pos().set(note.pos().x(), note.pos().y(), -1.0);
            note.setHoldLengthBeats(Math.max(step, note.holdLengthBeats()));
        } else {
            note.setHoldLengthBeats(0.0);
        }
    }

    private boolean isAnchorBeat(List<SelectedNote> anchors, double beat) {
        for (SelectedNote anchor : anchors) {
            if (Math.abs(anchor.note().beat() - beat) < NOTE_BEAT_EPSILON) {
                return true;
            }
        }
        return false;
    }

    private NoteData findNoteAtBeat(TrackData track, double beat) {
        for (NoteData note : track.notes()) {
            if (Math.abs(note.beat() - beat) < NOTE_BEAT_EPSILON) {
                return note;
            }
        }
        return null;
    }

    private Vec3Data lerpVec(Vec3Data left, Vec3Data right, double t) {
        return new Vec3Data(lerp(left.x(), right.x(), t), lerp(left.y(), right.y(), t), lerp(left.z(), right.z(), t));
    }

    private double lerp(double left, double right, double t) {
        return left + (right - left) * t;
    }

    private double smoothingEase(double t) {
        return smoothingUseCatmull || smoothingUseCircle ? t : EasingFunctions.getEase(t, smoothingEasingType);
    }

    private double smoothingRelaxStrength() {
        if (smoothingUseCircle) {
            return 0.0;
        }
        if (smoothingUseCatmull) {
            return 0.36;
        }
        String name = smoothingEasingType.name();
        if (name.contains("ELASTIC") || name.contains("BOUNCE") || name.contains("BACK")) {
            return 0.34;
        }
        if (name.contains("IN_OUT")) {
            return 0.30;
        }
        return smoothingEasingType == EasingType.LINEAR ? 0.18 : 0.24;
    }

    private String smoothingCurveLabel() {
        if (smoothingUseCatmull) {
            return "Catmull";
        }
        return smoothingUseCircle ? "Circle" : trackEaseDisplayLabel(smoothingEasingType);
    }

    private void advanceSmoothingCurve() {
        if (smoothingUseCatmull) {
            smoothingUseCatmull = false;
            smoothingUseCircle = true;
            return;
        }
        if (smoothingUseCircle) {
            smoothingUseCircle = false;
            smoothingEasingType = EasingType.LINEAR;
            return;
        }
        EasingType[] values = EasingType.values();
        int nextIndex = smoothingEasingType.ordinal() + 1;
        if (nextIndex >= values.length) {
            smoothingUseCatmull = true;
            smoothingUseCircle = false;
            smoothingEasingType = EasingType.LINEAR;
            return;
        }
        smoothingEasingType = values[nextIndex];
    }

    private double catmull(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2.0 * p1) + (-p0 + p2) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return parseInt(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private String fillSmoothingLabel() {
        return smoothingFillEnabled ? "Fill: On" : "Fill: Off";
    }

    private int deleteSelectedNotes() {
        int deleted = 0;
        for (SelectedNote selectedNote : new ArrayList<>(selectedNotes)) {
            if (selectedNote.track().notes().remove(selectedNote.note())) {
                deleted++;
            }
        }
        return deleted;
    }

    private int deleteSelectedEventClips() {
        int deleted = 0;
        List<SelectedEventClip> clips = new ArrayList<>(selectedEventClips);
        clips.sort(Comparator.comparingInt((SelectedEventClip clip) -> clip.track().id())
                .thenComparingInt(clip -> clip.eventType().ordinal())
                .thenComparing(SelectedEventClip::eventIndex)
                .reversed());
        for (SelectedEventClip clip : clips) {
            List<NumEventData> events = eventsForLane(clip.track(), clip.eventType());
            NumEventData event = eventForClip(clip);
            if (event != null && events.remove(event)) {
                deleted++;
            }
        }
        return deleted;
    }

    private void clearTimelineSelections() {
        selectedNotes.clear();
        selectedEventClips.clear();
        selectedEventClip = null;
        selectedEventHandle = null;
    }

    private void createNoteFromShortcut(NoteType noteType) {
        TrackData track = state.selectedTrack();
        if (track == null) {
            track = state.addTrack();
        }
        double beat = snapBeat(state.playheadBeat(), track);
        NoteData note = state.addNote(track, beat);
        note.setNoteType(noteType);
        if (noteType == NoteType.HOLD) {
            note.pos().set(note.pos().x(), note.pos().y(), -1.0);
            note.setHoldLengthBeats(trackGridStep(track));
        } else {
            note.setHoldLengthBeats(0.0);
        }
        replaceSelectedNotes(List.of(new SelectedNote(track, note)));
        populateFieldsFromSelection();
        layoutPropertyFields();
        state.setStatus("Created " + shortNoteLabel(noteType) + " on Track " + track.id() + " at " + subdivisionLabel(beat, track));
        captureHistorySnapshotIfNeeded();
    }

    private void addTopButtons(int y) {
        toolbarMenuTabs.clear();
        toolbarActionButtons.clear();

        int tabX = OUTER_PADDING;
        for (ToolbarMenu menu : ToolbarMenu.values()) {
            ButtonWidget tab = addDrawableChild(ButtonWidget.builder(Text.literal(menu.label), b -> {
                        activeToolbarMenu = menu;
                        updateToolbarButtons();
                    })
                    .dimensions(tabX, y, MENU_TAB_WIDTH, 18)
                    .build());
            toolbarMenuTabs.add(tab);
            tabX += MENU_TAB_WIDTH + 4;
        }

        int actionX = tabX + 10;
        int actionY = y;
        createToolbarAction(ToolbarMenu.FILE, actionX, actionY, 48, "New", b -> newProject(), false);
        createToolbarAction(ToolbarMenu.FILE, actionX + 52, actionY, 48, "Load", b -> loadProject(), false);
        createToolbarAction(ToolbarMenu.FILE, actionX + 104, actionY, 48, "Save", b -> saveProject(), false);
        createToolbarAction(ToolbarMenu.FILE, actionX + 156, actionY, 56, "Audio", b -> reloadAudio(), false);
        createToolbarAction(ToolbarMenu.FILE, actionX + 216, actionY, 54, "Title", b -> MinecraftClient.getInstance().disconnect(new TitleScreen(), false), true);

        createToolbarAction(ToolbarMenu.EDIT, actionX, actionY, 54, "+Track", b -> state.addTrack(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 58, actionY, 50, "+Note", b -> state.addNote(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 112, actionY, 42, "+Fx", b -> state.addEffect(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 158, actionY, 48, "+BPM", b -> state.addBpm(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 210, actionY, 44, "Del", b -> deleteCurrentSelection(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 258, actionY, 48, "Copy", b -> copySelectedNotesToClipboard(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 310, actionY, 40, "Cut", b -> cutSelectedNotesToClipboard(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 354, actionY, 52, "Paste", b -> pasteClipboardNotes(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 410, actionY, 48, "Undo", b -> undoEditorChange(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 462, actionY, 48, "Redo", b -> redoEditorChange(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 514, actionY, 44, "Play", b -> togglePlaybackFromToolbar(), false);
        createToolbarAction(ToolbarMenu.EDIT, actionX + 562, actionY, 44, "Stop", b -> stopPlaybackFromToolbar(), false);

        createToolbarAction(ToolbarMenu.OPTIONS, actionX, actionY, 48, "Song", b -> selectSong(), false);
        createToolbarAction(ToolbarMenu.OPTIONS, actionX + 52, actionY, 48, "Meta", b -> selectMeta(), false);
        createToolbarAction(ToolbarMenu.OPTIONS, actionX + 104, actionY, 48, "Sync", b -> syncWorldDisplays(), true);
        createToolbarAction(ToolbarMenu.OPTIONS, actionX + 156, actionY, 48, "Pick", b -> pickWorldDisplay(), true);
        createToolbarAction(ToolbarMenu.OPTIONS, actionX + 208, actionY, 32, "In", b -> state.markPlaybackStartAtPlayhead(), false);
        createToolbarAction(ToolbarMenu.OPTIONS, actionX + 244, actionY, 36, "Out", b -> state.markPlaybackEndAtPlayhead(), false);
        createToolbarAction(ToolbarMenu.OPTIONS, actionX + 284, actionY, 46, "NoOut", b -> state.clearPlaybackEndBeat(), false);
        createToolbarAction(ToolbarMenu.OPTIONS, actionX + 334, actionY, 52, "Range", b -> state.playPreviewRange(), false);
        createToolbarAction(ToolbarMenu.OPTIONS, actionX + 390, actionY, 54, "1Track", b -> state.toggleShowOnlySelectedTrack(), false);
        createToolbarAction(ToolbarMenu.OPTIONS, actionX + 448, actionY, 54, "Auto", b -> toggleWorldAutoPlay(), true);
        createToolbarAction(ToolbarMenu.OPTIONS, actionX + 506, actionY, 54, "Check", b -> runCompatibilityCheck(), false);

        updateToolbarButtons();
    }

    private void runCompatibilityCheck() {
        ChartCompatibilityValidator.Report report = ChartCompatibilityValidator.validate(state.level());
        if (report.ok()) {
            state.setStatus(report.summary());
            return;
        }
        ChartCompatibilityValidator.Issue first = report.issues().getFirst();
        state.setStatus(report.summary() + " | " + first.location() + ": " + first.message());
    }

    private void addDifficultyButtons(int y) {
        int x = OUTER_PADDING;
        for (ChartDifficulty difficulty : ChartDifficulty.values()) {
            int finalX = x;
            addDrawableChild(ButtonWidget.builder(Text.literal(difficulty.displayName()), b -> {
                state.setActiveDifficulty(difficulty);
                pathField.setText(state.project().projectPath().toString());
                layoutPropertyFields();
            }).dimensions(finalX, y, 64, 18).build());
            x += 68;
        }
    }

    private void createSmoothingPopupWidgets() {
        int popupX = smoothingPopupX();
        int popupY = smoothingPopupY();
        smoothingKindButton = addDrawableChild(ButtonWidget.builder(Text.literal("Curve: " + smoothingCurveLabel()), b -> {
            advanceSmoothingCurve();
            smoothingKindButton.setMessage(Text.literal("Curve: " + smoothingCurveLabel()));
        }).dimensions(popupX + 18, popupY + 54, 324, 18).build());
        smoothingNoteTypeButton = addDrawableChild(ButtonWidget.builder(Text.literal("Note: " + shortNoteLabel(smoothingNoteType)), b -> {
            smoothingNoteType = nextNoteType(smoothingNoteType);
            smoothingNoteTypeButton.setMessage(Text.literal("Note: " + shortNoteLabel(smoothingNoteType)));
        }).dimensions(popupX + 18, popupY + 94, 98, 18).build());
        smoothingFillButton = addDrawableChild(ButtonWidget.builder(Text.literal(fillSmoothingLabel()), b -> {
            smoothingFillEnabled = !smoothingFillEnabled;
            smoothingFillButton.setMessage(Text.literal(fillSmoothingLabel()));
        }).dimensions(popupX + 128, popupY + 94, 98, 18).build());
        smoothingDivisionField = addDrawableChild(new TextFieldWidget(textRenderer, popupX + 238, popupY + 94, 104, 18, Text.literal("Division")));
        smoothingDivisionField.setMaxLength(4);
        smoothingDivisionField.setText("16");
        smoothingApplyButton = addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> applySmoothingFromPopup())
                .dimensions(popupX + 176, popupY + 174, 76, 20).build());
        smoothingCancelButton = addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> closeSmoothingPopup())
                .dimensions(popupX + 264, popupY + 174, 78, 20).build());
        setSmoothingPopupWidgetsVisible(false);
    }

    private void setSmoothingPopupWidgetsVisible(boolean visible) {
        if (smoothingKindButton == null) {
            return;
        }
        smoothingKindButton.visible = visible;
        smoothingKindButton.active = visible;
        smoothingNoteTypeButton.visible = visible;
        smoothingNoteTypeButton.active = visible;
        smoothingFillButton.visible = visible;
        smoothingFillButton.active = visible;
        smoothingDivisionField.visible = visible;
        smoothingDivisionField.active = visible;
        smoothingApplyButton.visible = visible;
        smoothingApplyButton.active = visible;
        smoothingCancelButton.visible = visible;
        smoothingCancelButton.active = visible;
    }

    private void positionSmoothingPopupWidgets() {
        int popupX = smoothingPopupX();
        int popupY = smoothingPopupY();
        smoothingKindButton.setPosition(popupX + 18, popupY + 54);
        smoothingNoteTypeButton.setPosition(popupX + 18, popupY + 94);
        smoothingFillButton.setPosition(popupX + 128, popupY + 94);
        smoothingDivisionField.setPosition(popupX + 238, popupY + 94);
        smoothingApplyButton.setPosition(popupX + 176, popupY + 174);
        smoothingCancelButton.setPosition(popupX + 264, popupY + 174);
    }

    private int addButton(int x, int y, int width, String label, ButtonWidget.PressAction action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), action).dimensions(x, y, width, 18).build());
        return x + width;
    }

    private void createToolbarAction(ToolbarMenu menu, int x, int y, int width, String label, ButtonWidget.PressAction action, boolean worldOnly) {
        ButtonWidget button = addDrawableChild(ButtonWidget.builder(Text.literal(label), action).dimensions(x, y, width, 18).build());
        toolbarActionButtons.add(new ToolbarActionButton(menu, button, worldOnly));
    }

    private void updateToolbarButtons() {
        ToolbarMenu[] menus = ToolbarMenu.values();
        for (int index = 0; index < toolbarMenuTabs.size() && index < menus.length; index++) {
            ToolbarMenu menu = menus[index];
            toolbarMenuTabs.get(index).setMessage(Text.literal((menu == activeToolbarMenu ? "> " : "") + menu.label));
        }
        for (ToolbarActionButton actionButton : toolbarActionButtons) {
            boolean visible = actionButton.menu() == activeToolbarMenu && (!actionButton.worldOnly() || (client != null && client.world != null));
            actionButton.button().visible = visible;
            actionButton.button().active = visible;
        }
    }

    private void createPropertyFields() {
        EditorLayout layout = editorLayout();
        int rightX = layout.rightX() + 10;
        int fieldWidth = rightPanelInnerWidth();

        songFields.add(field(rightX, fieldWidth, "Name"));
        songFields.add(field(rightX, fieldWidth, "Composer"));
        songFields.add(field(rightX, fieldWidth, "Icon"));
        songFields.add(field(rightX, fieldWidth, "Alias"));
        songFields.add(field(rightX, fieldWidth, "Length(ms)"));
        songFields.add(field(rightX, fieldWidth, "SHA1"));
        songFields.add(field(rightX, fieldWidth, "Description"));
        songFields.add(field(rightX, fieldWidth, "Song ID"));
        songFields.add(field(rightX, fieldWidth, "Version"));
        songFields.add(field(rightX, fieldWidth, "Comments(csv)"));
        songFields.add(field(rightX, fieldWidth, "PlayerAlias(csv)"));
        songFields.add(field(rightX, fieldWidth, "Tags(csv)"));
        songFields.add(field(rightX, fieldWidth, "UnlockSong Entries"));
        songFields.add(field(rightX, fieldWidth, "UnlockWorld Entries"));
        songFields.add(field(rightX, fieldWidth, "UnlockNether Entries"));
        songFields.add(field(rightX, fieldWidth, "UnlockVoid Entries"));

        metaFields.add(field(rightX, fieldWidth, "UID"));
        metaFields.add(field(rightX, fieldWidth, "Initial Arena"));
        metaFields.add(field(rightX, fieldWidth, "Offset(ms)"));
        metaFields.add(field(rightX, fieldWidth, "Level"));
        metaFields.add(field(rightX, fieldWidth, "Charters(csv)"));
        metaFields.add(field(rightX, fieldWidth, "Comments(csv)"));

        trackFields.add(field(rightX, fieldWidth, "Track ID"));
        trackFields.add(field(rightX, fieldWidth, "Grid Division"));

        noteFields.add(field(rightX, fieldWidth, "Beat"));
        noteFields.add(field(rightX, fieldWidth, "Type"));
        noteFields.add(field(rightX, fieldWidth, "Track ID"));
        noteFields.add(field(rightX, fieldWidth, "Pos X"));
        noteFields.add(field(rightX, fieldWidth, "Pos Y"));
        noteFields.add(field(rightX, fieldWidth, "Pos Z"));
        noteFields.add(field(rightX, fieldWidth, "Scale X"));
        noteFields.add(field(rightX, fieldWidth, "Scale Y"));
        noteFields.add(field(rightX, fieldWidth, "Scale Z"));
        noteFields.add(field(rightX, fieldWidth, "Rot X"));
        noteFields.add(field(rightX, fieldWidth, "Rot Y"));
        noteFields.add(field(rightX, fieldWidth, "Rot Z"));
        noteFields.add(field(rightX, fieldWidth, "Hold Group"));
        noteFields.add(field(rightX, fieldWidth, "Hold Length"));

        effectFields.add(field(rightX, fieldWidth, "Beat"));
        effectFields.add(field(rightX, fieldWidth, "Type"));
        effectFields.add(field(rightX, fieldWidth, "Effect ID"));
        effectFields.add(field(rightX, fieldWidth, "Text / Value"));
        effectFields.add(field(rightX, fieldWidth, "Color"));
        effectFields.add(field(rightX, fieldWidth, "Track ID"));
        effectFields.add(field(rightX, fieldWidth, "Pos(x,y,z)"));
        effectFields.add(field(rightX, fieldWidth, "Scale(x,y,z)"));
        effectFields.add(field(rightX, fieldWidth, "Rot(x,y,z)"));
        effectFields.add(field(rightX, fieldWidth, "Arena / Target"));
        effectFields.add(field(rightX, fieldWidth, "Mode / State"));
        effectFields.add(field(rightX, fieldWidth, "Extra(KV optional)"));

        applyInputHints();

        bpmFields.add(field(rightX, fieldWidth, "Beat"));
        bpmFields.add(field(rightX, fieldWidth, "BPM"));

        applyButton = addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> applyFieldsToSelection())
                .dimensions(rightX, height - 42, 86, 18).build());
        resetButton = addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> populateFieldsFromSelection())
                .dimensions(rightX + 92, height - 42, 86, 18).build());
        easingPopupSearchField = addDrawableChild(new TextFieldWidget(textRenderer, rightX, 0, 188, 18, Text.literal("Search easing")));
        easingPopupSearchField.setMaxLength(64);
        easingPopupSearchField.visible = false;
        easingPopupSearchField.active = false;
        createTrackEventEditors(rightX, fieldWidth);
    }

    private void createTrackEventEditors(int rightX, int fieldWidth) {
        for (EventLaneType eventType : EventLaneType.values()) {
            ButtonWidget addButton = addDrawableChild(ButtonWidget.builder(Text.literal("Set"), b -> addTrackEventRow(eventType))
                    .dimensions(rightX + fieldWidth - 48, 0, 48, 18).build());
            ButtonWidget duplicateButton = addDrawableChild(ButtonWidget.builder(Text.literal("Copy"), b -> duplicateSelectedTrackEventRows(eventType))
                    .dimensions(rightX + fieldWidth - 80, 0, 28, 18).build());
            ButtonWidget deleteButton = addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> deleteSelectedTrackEventRows(eventType))
                    .dimensions(rightX + fieldWidth - 116, 0, 36, 18).build());
            addButton.visible = false;
            addButton.active = false;
            duplicateButton.visible = false;
            duplicateButton.active = false;
            deleteButton.visible = false;
            deleteButton.active = false;
            trackEventEditors.add(new TrackEventEditor(eventType, trackEventSection(eventType), addButton, duplicateButton, deleteButton, new ArrayList<>()));
        }
    }

    private boolean isSingleTrackEventLane() {
        return true;
    }

    private LabeledField field(int x, int width, String label) {
        TextFieldWidget widget = addDrawableChild(new TextFieldWidget(textRenderer, x, 0, width, 18, Text.literal(label)));
        widget.setMaxLength(4096);
        return new LabeledField(label, widget);
    }

    private void applyInputHints() {
        setFieldHint(songFields, 0, "Song list title");
        setFieldHint(songFields, 1, "Artist / composer");
        setFieldHint(songFields, 2, "Material icon key");
        setFieldHint(songFields, 3, "Short song code");
        setFieldHint(songFields, 4, "Audio length ms");
        setFieldHint(songFields, 5, "Respack SHA1");
        setFieldHint(songFields, 6, "Song blurb");
        setFieldHint(songFields, 7, "Unique song id");
        setFieldHint(songFields, 8, "Manifest version");
        setFieldHint(songFields, 9, "CSV list");
        setFieldHint(songFields, 10, "CSV list");
        setFieldHint(songFields, 11, "CSV list");
        setFieldHint(songFields, 12, "Rows: key=value,...");
        setFieldHint(songFields, 13, "Rows: key=value,...");
        setFieldHint(songFields, 14, "Rows: key=value,...");
        setFieldHint(songFields, 15, "Rows: key=value,...");

        setFieldHint(metaFields, 0, "Difficulty uid");
        setFieldHint(metaFields, 1, "Starting arena id");
        setFieldHint(metaFields, 2, "Audio offset ms");
        setFieldHint(metaFields, 3, "Displayed level");
        setFieldHint(metaFields, 4, "CSV list");
        setFieldHint(metaFields, 5, "CSV list");

        setFieldHint(trackFields, 0, "Track id");
        setFieldHint(trackFields, 1, "4 quarter, 8 eighth, 16 sixteenth");

        setFieldHint(noteFields, 0, "Beat position");
        setFieldHint(noteFields, 1, "Tap / Look / Hold / Dodge");
        setFieldHint(noteFields, 2, "Target track id");
        setFieldHint(noteFields, 3, "Center X");
        setFieldHint(noteFields, 4, "Center Y");
        setFieldHint(noteFields, 5, "Center Z");
        setFieldHint(noteFields, 6, "Scale X");
        setFieldHint(noteFields, 7, "Scale Y");
        setFieldHint(noteFields, 8, "Scale Z");
        setFieldHint(noteFields, 9, "Rotation X");
        setFieldHint(noteFields, 10, "Rotation Y");
        setFieldHint(noteFields, 11, "Rotation Z");
        setFieldHint(noteFields, 12, "Hold link id");
        setFieldHint(noteFields, 13, "Hold length in beats, snapped to track grid");

        setFieldHint(effectFields, 0, "Beat position");
        setFieldHint(effectFields, 1, "Effect type");
        setFieldHint(effectFields, 2, "Effect / display id");
        setFieldHint(effectFields, 3, "Main text or value");
        setFieldHint(effectFields, 4, "Named color");
        setFieldHint(effectFields, 5, "Target track id");
        setFieldHint(effectFields, 6, "World x,y,z");
        setFieldHint(effectFields, 7, "Local x,y,z scale");
        setFieldHint(effectFields, 8, "Local x,y,z rot");
        setFieldHint(effectFields, 9, "Arena / target");
        setFieldHint(effectFields, 10, "Mode / state");
        setFieldHint(effectFields, 11, "Extra key=value");

        setFieldHint(bpmFields, 0, "Beat position");
        setFieldHint(bpmFields, 1, "Tempo value");

        assignFieldSections();
    }

    private void assignFieldSections() {
        setFieldSection(songFields, 0, 8, "Identity");
        setFieldSection(songFields, 9, 11, "Lists");
        setFieldSection(songFields, 12, 15, "Unlocks");

        setFieldSection(metaFields, 0, 3, "Timing");
        setFieldSection(metaFields, 4, 5, "Credits");

        setFieldSection(trackFields, 0, 1, "Identity");

        setFieldSection(noteFields, 0, 2, "Timing");
        setFieldSection(noteFields, 3, 5, "Position");
        setFieldSection(noteFields, 6, 8, "Scale");
        setFieldSection(noteFields, 9, 11, "Rotation");
        setFieldSection(noteFields, 12, 13, "Links");

        setFieldSection(effectFields, 0, 1, "Timing");
        setFieldSection(effectFields, 2, 5, "Identity");
        setFieldSection(effectFields, 6, 6, "Position");
        setFieldSection(effectFields, 7, 7, "Scale");
        setFieldSection(effectFields, 8, 8, "Rotation");
        setFieldSection(effectFields, 9, 10, "Target");
        setFieldSection(effectFields, 11, 11, "Advanced");

        setFieldSection(bpmFields, 0, 1, "Timing");
    }

    private void setFieldSection(List<LabeledField> fields, int start, int end, String section) {
        for (int index = start; index <= end && index < fields.size(); index++) {
            fields.get(index).section = section;
        }
    }

    private void setFieldHint(List<LabeledField> fields, int index, String hint) {
        if (index >= 0 && index < fields.size()) {
            fields.get(index).hint = hint;
        }
    }

    private void layoutPropertyFields() {
        EditorLayout layout = editorLayout();
        int panelX = layout.rightX() + 10;
        int viewportTop = layout.topY() + 48;
        int viewportBottom = layout.topY() + layout.panelHeight() - 50;
        int currentY = viewportTop + propertyScroll;
        String lastSection = null;
        propertySectionHeaders.clear();
        List<LabeledField> activeFields = activeFieldGroup();
        for (LabeledField field : allFields()) {
            boolean visible = activeFields.contains(field);
            field.widget.visible = false;
            field.widget.active = false;
            field.sectionY = -1;
            if (visible) {
                if (field.section != null && !field.section.equals(lastSection)) {
                    currentY += 10;
                    field.sectionY = currentY;
                    propertySectionHeaders.add(new PropertySectionHeader(propertySectionKey(state.selection().kind(), field.section), field.section, panelX, currentY, rightPanelInnerWidth(), 12));
                    currentY += 12;
                    lastSection = field.section;
                }
                if (isSectionCollapsed(propertySectionKey(state.selection().kind(), field.section))) {
                    continue;
                }
                field.widget.setPosition(panelX, currentY + 20);
                boolean insideViewport = field.widget.getY() >= viewportTop && field.widget.getY() + 18 <= viewportBottom;
                field.widget.visible = insideViewport;
                field.widget.active = insideViewport;
                currentY += 42;
            }
        }
        currentY = layoutTrackEventEditors(panelX, currentY);
        applyButton.setPosition(panelX, layout.topY() + layout.panelHeight() - 34);
        resetButton.setPosition(panelX + 92, height - 42);
        resetButton.setPosition(panelX + 92, layout.topY() + layout.panelHeight() - 34);
    }

    private void populateFieldsFromSelection() {
        SongManifestData manifest = state.project().manifest();
        set(songFields, 0, manifest.name());
        set(songFields, 1, manifest.composer());
        set(songFields, 2, manifest.icon());
        set(songFields, 3, manifest.alias());
        set(songFields, 4, Integer.toString(manifest.length()));
        set(songFields, 5, manifest.respackSha1());
        set(songFields, 6, manifest.description());
        set(songFields, 7, Integer.toString(manifest.songId()));
        set(songFields, 8, manifest.version());
        set(songFields, 9, joinCsv(manifest.comments()));
        set(songFields, 10, joinCsv(manifest.playerAlias()));
        set(songFields, 11, joinCsv(manifest.tags()));
        set(songFields, 12, serializeMapEntries(manifest.unlockSong()));
        set(songFields, 13, serializeMapEntries(manifest.unlockWorld()));
        set(songFields, 14, serializeMapEntries(manifest.unlockNether()));
        set(songFields, 15, serializeMapEntries(manifest.unlockVoid()));

        MetaData meta = state.level().meta();
        set(metaFields, 0, Integer.toString(meta.uid()));
        set(metaFields, 1, meta.initialArena());
        set(metaFields, 2, Long.toString(meta.offset()));
        set(metaFields, 3, format(meta.level()));
        set(metaFields, 4, joinCsv(meta.charters()));
        set(metaFields, 5, joinCsv(meta.comments()));

        if (state.selection().kind() == EditorSelection.Kind.TRACK && state.selection().track() != null) {
            TrackData track = state.selection().track();
            set(trackFields, 0, Integer.toString(track.id()));
            set(trackFields, 1, Integer.toString(track.beatDivision()));
            syncTrackEventEditors(track);
        } else {
            hideTrackEventEditors();
        }

        if (state.selection().note() != null) {
            NoteData note = state.selection().note();
            set(noteFields, 0, format(note.beat()));
            set(noteFields, 1, note.noteType().name());
            set(noteFields, 2, Integer.toString(state.selection().track().id()));
            set(noteFields, 3, format(note.pos().x()));
            set(noteFields, 4, format(note.pos().y()));
            set(noteFields, 5, format(note.pos().z()));
            set(noteFields, 6, format(note.scale().x()));
            set(noteFields, 7, format(note.scale().y()));
            set(noteFields, 8, format(note.scale().z()));
            set(noteFields, 9, format(note.rotation().x()));
            set(noteFields, 10, format(note.rotation().y()));
            set(noteFields, 11, format(note.rotation().z()));
            set(noteFields, 12, Integer.toString(note.holdGroup()));
            set(noteFields, 13, format(note.holdLengthBeats()));
        }

        if (state.selection().effect() != null) {
            EffectData effect = state.selection().effect();
            JsonObject properties = effect.properties() == null ? new JsonObject() : effect.properties();
            set(effectFields, 0, format(effect.beat()));
            set(effectFields, 1, effect.effectType().name());
            set(effectFields, 2, propertyString(properties, "id", propertyString(properties, "textId", "")));
            set(effectFields, 3, firstProperty(properties, "text", "content", "title", "message", "value"));
            set(effectFields, 4, propertyString(properties, "color", propertyString(properties, "glowColor", "")));
            set(effectFields, 5, firstProperty(properties, "trackId", "track", ""));
            set(effectFields, 6, serializeTriple(
                    getDouble(properties, "x", 0.0),
                    getDouble(properties, "y", 0.0),
                    getDouble(properties, "z", 0.0)
            ));
            set(effectFields, 7, serializeTriple(
                    getDouble(properties, "scaleX", 1.0),
                    getDouble(properties, "scaleY", 1.0),
                    getDouble(properties, "scaleZ", 1.0)
            ));
            set(effectFields, 8, serializeTriple(
                    getDouble(properties, "rotationX", 0.0),
                    getDouble(properties, "rotationY", 0.0),
                    getDouble(properties, "rotationZ", 0.0)
            ));
            set(effectFields, 9, firstProperty(properties, "arena", "target", "weather", "time"));
            set(effectFields, 10, firstProperty(properties, "mode", "state", "type", "action"));
            set(effectFields, 11, serializeExtraProperties(filterExtraEffectProperties(properties)));
        }

        if (state.selection().bpm() != null) {
            BpmPoint bpm = state.selection().bpm();
            set(bpmFields, 0, format(bpm.beat()));
            set(bpmFields, 1, format(bpm.bpm()));
        }
    }

    private void applyFieldsToSelection() {
        try {
            switch (state.selection().kind()) {
                case SONG -> applySongFields();
                case META -> applyMetaFields();
                case TRACK -> applyTrackFields();
                case NOTE -> applyNoteFields();
                case EFFECT -> applyEffectFields();
                case BPM -> applyBpmFields();
            }
            state.sortCurrentLevel();
            state.markDirty();
            state.setStatus("Applied changes");
            populateFieldsFromSelection();
        } catch (RuntimeException exception) {
            state.setStatus("Apply failed: " + exception.getMessage());
        }
    }

    private void applySongFields() {
        SongManifestData manifest = state.project().manifest();
        manifest.setName(get(songFields, 0));
        manifest.setComposer(get(songFields, 1));
        manifest.setIcon(get(songFields, 2));
        manifest.setAlias(get(songFields, 3));
        manifest.setLength(parseInt(get(songFields, 4)));
        manifest.setRespackSha1(get(songFields, 5));
        manifest.setDescription(get(songFields, 6));
        manifest.setSongId(parseInt(get(songFields, 7)));
        manifest.setVersion(get(songFields, 8));
        replaceStrings(manifest.comments(), splitCsv(get(songFields, 9)));
        replaceStrings(manifest.playerAlias(), splitCsv(get(songFields, 10)));
        replaceStrings(manifest.tags(), splitCsv(get(songFields, 11)));
        replaceMaps(manifest.unlockSong(), parseMapEntries(get(songFields, 12)));
        replaceMaps(manifest.unlockWorld(), parseMapEntries(get(songFields, 13)));
        replaceMaps(manifest.unlockNether(), parseMapEntries(get(songFields, 14)));
        replaceMaps(manifest.unlockVoid(), parseMapEntries(get(songFields, 15)));
    }

    private void applyMetaFields() {
        MetaData meta = state.level().meta();
        meta.setUid(parseInt(get(metaFields, 0)));
        meta.setInitialArena(get(metaFields, 1));
        meta.setOffset(parseLong(get(metaFields, 2)));
        meta.setLevel(parseDouble(get(metaFields, 3)));
        replaceStrings(meta.charters(), splitCsv(get(metaFields, 4)));
        replaceStrings(meta.comments(), splitCsv(get(metaFields, 5)));
        state.seekToBeat(state.playheadBeat());
    }

    private void applyTrackFields() {
        TrackData track = state.selection().track();
        track.setId(parseInt(get(trackFields, 0)));
        track.setBeatDivision(parseInt(get(trackFields, 1)));
        applyTrackEventEditors(track);
    }

    private void applyNoteFields() {
        NoteData note = state.selection().note();
        TrackData currentTrack = state.selection().track();
        note.setBeat(parseDouble(get(noteFields, 0)));
        note.setNoteType(NoteType.valueOf(get(noteFields, 1).trim().toUpperCase(Locale.ROOT)));
        double posZ = parseDouble(get(noteFields, 5));
        if (note.noteType() == NoteType.HOLD) {
            posZ = -1.0;
        }
        note.pos().set(parseDouble(get(noteFields, 3)), parseDouble(get(noteFields, 4)), posZ);
        note.scale().set(parseDouble(get(noteFields, 6)), parseDouble(get(noteFields, 7)), parseDouble(get(noteFields, 8)));
        note.rotation().set(parseDouble(get(noteFields, 9)), parseDouble(get(noteFields, 10)), parseDouble(get(noteFields, 11)));
        note.setHoldGroup(parseInt(get(noteFields, 12)));
        if (note.noteType() == NoteType.HOLD) {
            note.setHoldLengthBeats(snapHoldLength(parseDouble(get(noteFields, 13)), state.selection().track()));
        } else {
            note.setHoldLengthBeats(0.0);
        }
        int targetTrackId = parseInt(get(noteFields, 2));
        TrackData newTrack = state.level().tracks().stream().filter(track -> track.id() == targetTrackId).findFirst().orElse(currentTrack);
        state.moveSelectedNote(note.beat(), newTrack);
    }

    private void applyEffectFields() {
        EffectData effect = state.selection().effect();
        effect.setBeat(parseDouble(get(effectFields, 0)));
        effect.setEffectType(EffectType.fromName(get(effectFields, 1)));

        JsonObject properties = new JsonObject();
        putIfNotBlank(properties, "id", get(effectFields, 2));

        String textValue = get(effectFields, 3);
        if (!textValue.isBlank()) {
            switch (effect.effectType()) {
                case TITLE -> properties.addProperty("title", textValue);
                case MESSAGE -> properties.addProperty("message", textValue);
                case TEXT_DISPLAY, TEXT_DISPLAY_EFFECT -> {
                    properties.addProperty("text", textValue);
                    properties.addProperty("content", textValue);
                }
                case WEATHER -> properties.addProperty("weather", textValue);
                case TIME -> properties.addProperty("time", textValue);
                default -> properties.addProperty("value", textValue);
            }
        }

        putIfNotBlank(properties, "color", get(effectFields, 4));
        if (effect.effectType() == EffectType.GLOW_COLOR) {
            putIfNotBlank(properties, "glowColor", get(effectFields, 4));
        }

        if (!get(effectFields, 5).isBlank()) {
            putIntIfNotBlank(properties, "trackId", get(effectFields, 5));
            putIntIfNotBlank(properties, "track", get(effectFields, 5));
        }

        Vec3Data pos = parseOptionalVec(get(effectFields, 6), Vec3Data.zero());
        properties.addProperty("x", pos.x());
        properties.addProperty("y", pos.y());
        properties.addProperty("z", pos.z());

        Vec3Data scale = parseOptionalVec(get(effectFields, 7), Vec3Data.one());
        properties.addProperty("scaleX", scale.x());
        properties.addProperty("scaleY", scale.y());
        properties.addProperty("scaleZ", scale.z());

        Vec3Data rotation = parseOptionalVec(get(effectFields, 8), Vec3Data.zero());
        properties.addProperty("rotationX", rotation.x());
        properties.addProperty("rotationY", rotation.y());
        properties.addProperty("rotationZ", rotation.z());

        if (!get(effectFields, 9).isBlank()) {
            String targetValue = get(effectFields, 9);
            switch (effect.effectType()) {
                case ARENA -> properties.addProperty("arena", targetValue);
                case WEATHER -> properties.addProperty("target", targetValue);
                case TIME -> properties.addProperty("target", targetValue);
                default -> properties.addProperty("target", targetValue);
            }
        }

        if (!get(effectFields, 10).isBlank()) {
            String modeValue = get(effectFields, 10);
            switch (effect.effectType()) {
                case HIDE_NOTES -> properties.addProperty("state", modeValue);
                case MESSAGE -> properties.addProperty("action", modeValue);
                default -> properties.addProperty("mode", modeValue);
            }
        }

        String extraKv = get(effectFields, 11);
        if (!extraKv.isBlank()) {
            for (Map.Entry<String, String> entry : parseKeyValuePairs(extraKv).entrySet()) {
                properties.addProperty(entry.getKey(), entry.getValue());
            }
        }
        effect.setProperties(properties);
    }

    private void applyBpmFields() {
        BpmPoint bpm = state.selection().bpm();
        bpm.setBeat(parseDouble(get(bpmFields, 0)));
        bpm.setBpm(parseDouble(get(bpmFields, 1)));
        state.seekToBeat(state.playheadBeat());
    }

    private void drawEditorChrome(DrawContext context) {
        context.fill(0, 0, width, height, UI_BG);
        context.fill(0, 0, width, TOP_BAR_HEIGHT - 1, UI_TOP_BG);
        context.fill(0, TOP_BAR_HEIGHT - 1, width, TOP_BAR_HEIGHT, UI_BORDER);
        context.drawText(textRenderer, Text.literal("RhythMC Chart Maker"), OUTER_PADDING, 10, UI_TEXT, false);
        context.drawText(textRenderer, Text.literal(state.project().manifest().name()), Math.max(112, width - 270), 10, UI_MUTED, false);
        int pillX = Math.max(112, width - 430);
        drawPill(context, pillX, 58, 82, 18, "In " + format(state.playbackStartBeat()), UI_ACCENT);
        pillX += 86;
        drawPill(context, pillX, 58, 86, 18, state.hasPlaybackEndBeat() ? "Out " + format(state.playbackEndBeat()) : "Out --", UI_WARN);
        pillX += 90;
        drawPill(context, pillX, 58, 74, 18, state.showOnlySelectedTrack() ? "1 Track" : "All Tracks", state.showOnlySelectedTrack() ? UI_ACCENT : UI_DIM);
        pillX += 78;
        drawPill(context, pillX, 58, 76, 18, state.playing() ? "Playing" : "Paused", state.playing() ? UI_GREEN : UI_DIM);
        pillX += 80;
        drawPill(context, pillX, 58, 80, 18, format(state.playheadBeat()), UI_WARN);
    }

    private void drawStatusBar(DrawContext context) {
        int y = height - STATUS_BAR_HEIGHT;
        context.fill(0, y, width, height, 0xF0161C22);
        context.fill(0, y, width, y + 1, UI_BORDER_SOFT);
        String hint = statusHint();
        int hintWidth = Math.min(520, textRenderer.getWidth(hint) + 8);
        drawTrimmedText(context, state.statusMessage(), OUTER_PADDING, y + 6, Math.max(80, width - hintWidth - OUTER_PADDING * 3), UI_TEXT);
        drawTrimmedText(context, hint, Math.max(OUTER_PADDING, width - hintWidth - OUTER_PADDING), y + 6, hintWidth, UI_MUTED);
    }

    private String statusHint() {
        if (client != null && RmcChartClient.getWorldLauncher().isEditorWorldActive(client)) {
            String mode = RmcChartClient.getWorldLauncher().playbackEngine().isAutoPlay() ? "AutoPlay on" : "AutoPlay off";
            String scope = state.showOnlySelectedTrack() && state.selectedTrack() != null ? "Track " + state.selectedTrack().id() : "All tracks";
            return "P start/pause | O stop | End close | F GUI | I AutoPlay | " + scope + " | " + mode;
        }
        if (smoothingPopupOpen) {
            return "Enter apply | Esc close | Fill uses 1/x division";
        }
        if (hasSelectedNotes()) {
            return "Ctrl+Click/drag multi-select | Ctrl+3 smoothing | Alt+Arrows move";
        }
        return "1/2/3/4 create notes | Ctrl+Wheel zoom | Ctrl+A multi-select | Space play";
    }

    private void drawSmoothingPopupBackground(DrawContext context) {
        if (!smoothingPopupOpen) {
            return;
        }
        int popupX = smoothingPopupX();
        int popupY = smoothingPopupY();
        context.fill(0, 0, width, height, 0x99000000);
        context.fill(popupX, popupY, popupX + SMOOTHING_POPUP_WIDTH, popupY + SMOOTHING_POPUP_HEIGHT, 0xF01A222A);
        drawOutline(context, popupX, popupY, SMOOTHING_POPUP_WIDTH, SMOOTHING_POPUP_HEIGHT, UI_ACCENT);
        context.fill(popupX, popupY, popupX + SMOOTHING_POPUP_WIDTH, popupY + 32, UI_PANEL_STRONG);
    }

    private void drawSmoothingPopupText(DrawContext context) {
        if (!smoothingPopupOpen) {
            return;
        }
        int popupX = smoothingPopupX();
        int popupY = smoothingPopupY();
        context.drawText(textRenderer, Text.literal("Smoothing"), popupX + 14, popupY + 11, UI_TEXT, false);
        context.drawText(textRenderer, Text.literal(currentSelectedNotes().size() + " selected note(s)"), popupX + 222, popupY + 11, UI_MUTED, false);
        context.drawText(textRenderer, Text.literal("Curve Type"), popupX + 18, popupY + 42, UI_MUTED, false);
        context.drawText(textRenderer, Text.literal("Note Type"), popupX + 18, popupY + 82, UI_MUTED, false);
        context.drawText(textRenderer, Text.literal("Fill"), popupX + 128, popupY + 82, UI_MUTED, false);
        context.drawText(textRenderer, Text.literal("Division"), popupX + 238, popupY + 82, UI_MUTED, false);
        drawTrimmedText(context, "Curve cycles Catmull, Circle, plus every EasingType curve.", popupX + 18, popupY + 122, SMOOTHING_POPUP_WIDTH - 36, UI_DIM);
        drawTrimmedText(context, "Fill creates notes on the selected track's 1/x grid between anchors.", popupX + 18, popupY + 138, SMOOTHING_POPUP_WIDTH - 36, UI_DIM);
    }

    private int smoothingPopupX() {
        return Math.max(10, width / 2 - SMOOTHING_POPUP_WIDTH / 2);
    }

    private int smoothingPopupY() {
        return Math.max(10, height / 2 - SMOOTHING_POPUP_HEIGHT / 2);
    }

    private void drawPill(DrawContext context, int x, int y, int pillWidth, int pillHeight, String label, int color) {
        context.fill(x, y, x + pillWidth, y + pillHeight, 0x66232D36);
        context.fill(x, y, x + 2, y + pillHeight, color);
        drawOutline(context, x, y, pillWidth, pillHeight, UI_BORDER_SOFT);
        drawTrimmedText(context, label, x + 7, y + 5, pillWidth - 10, color);
    }

    private void drawLeftPanel(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, UI_PANEL);
        drawOutline(context, x, y, width, height, UI_BORDER);
        context.enableScissor(x, y, x + width, y + height);
        context.drawText(textRenderer, Text.literal("Scene Map"), x + 10, y + 9, UI_TEXT, false);
        context.drawText(textRenderer, Text.literal("Beat " + format(state.playheadBeat())), x + 88, y + 9, UI_MUTED, false);

        int mapX = x + 10;
        int mapY = y + 28;
        int mapSize = width - 20;
        drawCurrentFrameMap(context, mapX, mapY, mapSize, mapSize);

        int rowY = mapY + mapSize + 12;
        context.drawText(textRenderer, Text.literal("Tracks"), x + 10, rowY, UI_TEXT, false);
        context.drawText(textRenderer, Text.literal(state.activeDifficulty().displayName()), x + width - 74, rowY, UI_ACCENT, false);
        rowY += 14;
        for (TrackData track : state.tracks()) {
            boolean selected = state.selection().track() == track || (state.selection().kind() == EditorSelection.Kind.NOTE && state.selection().track() == track);
            int color = selected ? 0xCC315F91 : 0x66303A44;
            context.fill(x + 8, rowY, x + width - 8, rowY + 26, color);
            context.fill(x + 12, rowY + 6, x + 24, rowY + 20, selected ? UI_ACCENT : 0x8848596A);
            context.drawText(textRenderer, Text.literal(state.isTrackExpanded(track) ? "-" : "+"), x + 16, rowY + 9, UI_TEXT, false);
            drawTrimmedText(context, "Track " + track.id(), x + 32, rowY + 4, width - 82, UI_TEXT);
            drawTrimmedText(context, track.notes().size() + " notes | " + countTrackEvents(track) + " events", x + 32, rowY + 15, width - 48, UI_MUTED);
            rowY += 30;
        }
        rowY += 8;
        drawStatLine(context, x + 10, rowY, "Effects", Integer.toString(state.level().effects().size()), UI_WARN);
        rowY += 15;
        drawStatLine(context, x + 10, rowY, "BPM", Integer.toString(state.level().meta().bpms().size()), UI_ACCENT);
        rowY += 15;
        drawTrimmedText(context, "Audio: " + state.audioStatus(), x + 10, rowY, width - 20, UI_GREEN);
        rowY += 15;
        if (client != null && client.world != null) {
            drawTrimmedText(context, "World preview active", x + 10, rowY, width - 20, UI_WARN);
            rowY += 15;
        }
        context.disableScissor();
    }

    private void drawCurrentFrameMap(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, UI_PANEL_ALT);
        drawOutline(context, x, y, width, height, UI_BORDER_SOFT);
        context.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);

        int centerX = x + width / 2;
        int centerY = y + height / 2;
        context.fill(centerX - 1, y + 6, centerX + 1, y + height - 6, 0x445FBCD3);
        context.fill(x + 6, centerY - 1, x + width - 6, centerY + 1, 0x445FBCD3);
        context.drawText(textRenderer, Text.literal("Y"), centerX + 4, y + 6, UI_DIM, false);
        context.drawText(textRenderer, Text.literal("X"), x + width - 12, centerY + 4, UI_DIM, false);

        double beat = state.playheadBeat();
        double scale = Math.max(6.0, width / 22.0);

        for (TrackData track : state.visibleTracks()) {
            ChartEvaluator.TrackState trackState = ChartEvaluator.evaluateTrack(track, beat);

            int trackCenterX = centerX + (int) Math.round(trackState.xTransform() * scale);
            int trackCenterY = centerY - (int) Math.round(trackState.yTransform() * scale);
            context.fill(trackCenterX - 2, trackCenterY - 2, trackCenterX + 2, trackCenterY + 2, 0xFFFFFFFF);

            for (NoteData note : track.notes()) {
                ChartEvaluator.NoteState noteState = ChartEvaluator.evaluateNote(trackState, note);
                if (Math.abs(noteState.distanceToHitPlane()) > 24.0) {
                    continue;
                }

                int drawX = centerX + (int) Math.round(noteState.worldX() * scale);
                int drawY = centerY - (int) Math.round(noteState.worldY() * scale);
                int color = note == state.selection().note() ? 0xFFFFFFFF : noteColor(note.noteType());
                context.fill(drawX - 3, drawY - 3, drawX + 3, drawY + 3, color);
            }
        }

        for (EffectData effect : state.triggeredEffects()) {
            JsonObject properties = effect.properties();
            double fx = getDouble(properties, "x", 0.0);
            double fy = getDouble(properties, "y", 0.0);
            int drawX = centerX + (int) Math.round(fx * scale);
            int drawY = centerY - (int) Math.round(fy * scale);
            int color = effect == state.selection().effect() ? 0xFFFFF08A : 0xFFFFA040;
            context.fill(drawX - 2, drawY - 2, drawX + 2, drawY + 2, color);
            context.drawText(textRenderer, Text.literal(shortEffectLabel(effect.effectType())), drawX + 4, drawY - 4, color, false);
        }
        context.disableScissor();
    }

    private boolean handleCurrentFrameMapClick(double mouseX, double mouseY, int x, int y, int width, int height) {
        MapSelection best = null;
        for (MapSelection candidate : collectCurrentFrameMapSelections(x, y, width, height)) {
            double distance = squaredDistance(mouseX, mouseY, candidate.x, candidate.y);
            if (best == null || distance < best.distance) {
                best = new MapSelection(candidate.kind, candidate.track, candidate.note, candidate.effect, candidate.x, candidate.y, distance);
            }
        }
        if (best == null || best.distance > 196.0) {
            return false;
        }
        if (best.kind == EditorSelection.Kind.NOTE) {
            state.setSelection(EditorSelection.note(best.track, best.note));
            state.setStatus("Selected note from current-frame map");
        } else if (best.kind == EditorSelection.Kind.EFFECT) {
            state.setSelection(EditorSelection.effect(best.effect));
            state.setStatus("Selected effect from current-frame map");
        }
        return true;
    }

    private List<MapSelection> collectCurrentFrameMapSelections(int x, int y, int width, int height) {
        List<MapSelection> selections = new ArrayList<>();
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        double beat = state.playheadBeat();
        double scale = Math.max(6.0, width / 22.0);
        for (TrackData track : state.visibleTracks()) {
            ChartEvaluator.TrackState trackState = ChartEvaluator.evaluateTrack(track, beat);
            for (NoteData note : track.notes()) {
                ChartEvaluator.NoteState noteState = ChartEvaluator.evaluateNote(trackState, note);
                if (Math.abs(noteState.distanceToHitPlane()) > 24.0) {
                    continue;
                }
                selections.add(new MapSelection(
                        EditorSelection.Kind.NOTE,
                        track,
                        note,
                        null,
                        centerX + (int) Math.round(noteState.worldX() * scale),
                        centerY - (int) Math.round(noteState.worldY() * scale),
                        0.0
                ));
            }
        }
        for (EffectData effect : state.triggeredEffects()) {
            JsonObject properties = effect.properties();
            selections.add(new MapSelection(
                    EditorSelection.Kind.EFFECT,
                    null,
                    null,
                    effect,
                    centerX + (int) Math.round(getDouble(properties, "x", 0.0) * scale),
                    centerY - (int) Math.round(getDouble(properties, "y", 0.0) * scale),
                    0.0
            ));
        }
        return selections;
    }

    private void drawPreviewPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, UI_PANEL);
        drawOutline(context, x, y, width, height, UI_BORDER);
        context.drawText(textRenderer, Text.literal("Stage Preview"), x + 10, y + 9, UI_TEXT, false);
        context.drawText(textRenderer, Text.literal(stateAudioMillis()), x + width - 76, y + 9, UI_MUTED, false);
        if (state.showOnlySelectedTrack() && state.selectedTrack() != null) {
            drawTrimmedText(context, "Only Track " + state.selectedTrack().id(), x + 104, y + 9, width - 192, UI_ACCENT);
        }
        context.enableScissor(x + 1, y + 22, x + width - 1, y + height - 1);

        int sideWidth = width >= 520 ? 150 : 0;
        int stageX = x + 12;
        int stageY = y + 28;
        int stageWidth = Math.max(120, width - 24 - sideWidth);
        int stageHeight = Math.max(76, height - 40);
        int stageRight = stageX + stageWidth;
        int stageBottom = stageY + stageHeight;
        int centerX = stageX + stageWidth / 2;
        int hitY = stageBottom - 18;
        int horizonY = stageY + 16;

        context.fill(stageX, stageY, stageRight, stageBottom, 0xD112171D);
        drawOutline(context, stageX, stageY, stageWidth, stageHeight, 0x665FBCD3);
        drawPreviewDepthBand(context, stageX, stageY, stageWidth, stageHeight);
        drawLine(context, centerX, horizonY, stageX + 14, hitY, 0x334FC3F7, 1);
        drawLine(context, centerX, horizonY, stageRight - 14, hitY, 0x334FC3F7, 1);
        drawLine(context, centerX - 48, horizonY + 8, centerX - 92, hitY, 0x224FC3F7, 1);
        drawLine(context, centerX + 48, horizonY + 8, centerX + 92, hitY, 0x224FC3F7, 1);
        context.fill(stageX + 10, hitY - 1, stageRight - 10, hitY + 2, 0xAA8FE1B2);
        drawTrimmedText(context, "Hit plane", stageX + 14, hitY + 5, 72, UI_GREEN);
        drawTrimmedText(context, "Far", stageRight - 34, horizonY - 4, 28, UI_DIM);

        double beat = state.playheadBeat();
        int visibleNotes = 0;
        for (TrackData track : state.visibleTracks()) {
            ChartEvaluator.TrackState trackState = ChartEvaluator.evaluateTrack(track, beat);

            int anchorX = clamp(centerX + (int) Math.round(trackState.xTransform() * 18.0), stageX + 12, stageRight - 12);
            int anchorY = clamp(hitY - 7 - (int) Math.round(trackState.yTransform() * 10.0), stageY + 16, stageBottom - 8);
            int anchorColor = state.selectedTrack() == track ? UI_ACCENT : 0xFFB9C8D8;
            context.fill(anchorX - 3, anchorY - 3, anchorX + 3, anchorY + 3, anchorColor);
            drawTrimmedText(context, "T" + track.id(), anchorX + 5, anchorY - 4, 34, anchorColor);

            for (NoteData note : track.notes()) {
                ChartEvaluator.NoteState noteState = ChartEvaluator.evaluateNote(trackState, note);
                if (!noteState.isWithinTypeRange()) {
                    continue;
                }
                double stageDis = noteState.worldZ();
                double depth = Math.max(0.0, Math.min(1.0, (stageDis - note.noteType().zNear()) / Math.max(1.0, note.noteType().zFar() - note.noteType().zNear())));
                double perspective = 1.0 - depth * 0.42;
                int drawX = clamp(centerX + (int) Math.round(noteState.worldX() * 18.0 * perspective), stageX + 8, stageRight - 8);
                int drawY = clamp(hitY - (int) Math.round(depth * (hitY - horizonY)) - (int) Math.round(noteState.worldY() * 8.0 * perspective), stageY + 8, stageBottom - 8);
                int size = note == state.selection().note() ? 9 : Math.max(5, 9 - (int) Math.round(depth * 3.0));
                drawPreviewNoteGlyph(context, note, drawX, drawY, size, depth);
                visibleNotes++;
            }
        }

        int textY = stageY + 4;
        drawTrimmedText(context, "Audio: " + audioSummary(), stageX + 8, textY, stageWidth - 16, UI_GREEN);
        textY += 12;
        String outBeat = state.hasPlaybackEndBeat() ? format(state.playbackEndBeat()) : "--";
        drawTrimmedText(context, "Range: " + format(state.playbackStartBeat()) + " -> " + outBeat + "  |  visible " + visibleNotes, stageX + 8, textY, stageWidth - 16, UI_ACCENT);
        textY += 12;
        for (String line : activeTextDisplayLines()) {
            drawTrimmedText(context, line, stageX + 8, textY, stageWidth - 16, UI_WARN);
            textY += 10;
            if (textY > stageY + stageHeight - 14) {
                break;
            }
        }
        if (sideWidth > 0) {
            drawSchematicEditorOverlay(context, x + width - 150, y + 28, 138, Math.max(74, height - 38));
        }
        context.disableScissor();
    }

    private void drawPreviewDepthBand(DrawContext context, int x, int y, int width, int height) {
        int bottom = y + height - 18;
        int top = y + 16;
        for (int index = 0; index < 5; index++) {
            double progress = index / 4.0;
            int bandY = bottom - (int) Math.round(progress * (bottom - top));
            int bandWidth = (int) Math.round(width * (0.88 - progress * 0.42));
            int left = x + width / 2 - bandWidth / 2;
            int color = index == 0 ? 0x558FE1B2 : 0x225FBCD3;
            context.fill(left, bandY, left + bandWidth, bandY + 1, color);
            if (index > 0) {
                drawTrimmedText(context, "+" + index + " beat", left + 4, bandY - 8, 54, UI_DIM);
            }
        }
    }

    private void drawPreviewNoteGlyph(DrawContext context, NoteData note, int x, int y, int size, double depth) {
        int color = note == state.selection().note() ? 0xFFFFFFFF : noteColor(note.noteType());
        int dimColor = (color & 0x00FFFFFF) | (depth > 0.65 ? 0xAA000000 : 0xDD000000);
        if (note.noteType() == NoteType.HOLD) {
            int tail = Math.max(size + 4, (int) Math.round(note.holdLengthBeats() * 10.0));
            context.fill(x - 2, y, x + 2, y + tail, 0x9926A69A);
        }
        context.fill(x - size / 2, y - size / 2, x + size / 2, y + size / 2, dimColor);
        drawOutline(context, x - size / 2, y - size / 2, size, size, color);
        if (note.noteType() == NoteType.DODGE) {
            drawLine(context, x - size / 2, y - size / 2, x + size / 2, y + size / 2, 0xFFFFFFFF, 1);
            drawLine(context, x + size / 2, y - size / 2, x - size / 2, y + size / 2, 0xFFFFFFFF, 1);
        } else if (note.noteType() == NoteType.LOOK) {
            context.fill(x - size / 2 - 2, y, x + size / 2 + 2, y + 1, 0xFFFFFFFF);
            context.fill(x, y - size / 2 - 2, x + 1, y + size / 2 + 2, 0xFFFFFFFF);
        }
        drawTrimmedText(context, shortNoteLabel(note.noteType()), x + size / 2 + 3, y - 4, 34, color);
    }

    private void drawSchematicEditorOverlay(DrawContext context, int x, int y, int width, int height) {
        TrackData track = state.selectedTrack();
        if (track == null || width < 80 || height < 64) {
            return;
        }
        context.fill(x, y, x + width, y + height, 0xB8141A20);
        drawOutline(context, x, y, width, height, UI_BORDER_SOFT);
        context.drawText(textRenderer, Text.literal("Schematic"), x + 6, y + 5, UI_TEXT, false);
        context.drawText(textRenderer, Text.literal("Track " + track.id()), x + width - 48, y + 5, UI_MUTED, false);

        int gridX = x + 8;
        int gridY = y + 20;
        int gridWidth = width - 16;
        int gridHeight = height - 28;
        int cellWidth = Math.max(1, gridWidth / 3);
        int cellHeight = Math.max(1, gridHeight / 4);
        for (int gx = 0; gx < 3; gx++) {
            for (int gy = 0; gy < 4; gy++) {
                int left = gridX + gx * cellWidth;
                int top = gridY + (3 - gy) * cellHeight;
                context.fill(left, top, left + cellWidth - 1, top + cellHeight - 1, 0x55303A44);
                drawOutline(context, left, top, cellWidth - 1, cellHeight - 1, 0x335FBCD3);
            }
        }
        int centerX = gridX + cellWidth + cellWidth / 2;
        int centerY = gridY + cellHeight + cellHeight / 2;
        context.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, UI_ACCENT);

        double currentBeat = state.playheadBeat();
        double window = Math.max(2.0, state.beatsPerScreen() / 8.0);
        for (NoteData note : track.notes()) {
            double delta = Math.abs(note.beat() - currentBeat);
            if (delta > window) {
                continue;
            }
            int cellX = clamp((int) Math.round(note.pos().x()) + 1, 0, 2);
            int cellY = clamp((int) Math.round(note.pos().y()) + 1, 0, 3);
            int drawX = gridX + cellX * cellWidth + cellWidth / 2;
            int drawY = gridY + (3 - cellY) * cellHeight + cellHeight / 2;
            int size = note == state.selection().note() ? 5 : 3;
            int color = noteColor(note.noteType());
            context.fill(drawX - size, drawY - size, drawX + size, drawY + size, color);
        }
    }

    private void drawTimeline(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, UI_PANEL);
        drawOutline(context, x, y, width, height, UI_BORDER);

        int contentX = x + TIMELINE_LABEL_WIDTH;
        int contentWidth = Math.max(48, width - TIMELINE_LABEL_WIDTH);
        int contentY = y + TIMELINE_RULER_HEIGHT;
        int audioY = y + height - TIMELINE_AUDIO_STRIP_HEIGHT;
        int contentHeight = Math.max(48, audioY - contentY);
        int contentBottom = contentY + contentHeight;
        context.fill(x, y, x + width, contentY, UI_PANEL_STRONG);
        context.fill(x, contentY, contentX, contentBottom, UI_PANEL_ALT);
        context.fill(x, audioY, x + width, y + height, UI_PANEL_STRONG);
        context.fill(contentX, y, contentX + 1, y + height, UI_BORDER);
        drawOutline(context, contentX, y, contentWidth, TIMELINE_RULER_HEIGHT, UI_BORDER_SOFT);
        drawOutline(context, x, audioY, width, TIMELINE_AUDIO_STRIP_HEIGHT, UI_BORDER_SOFT);

        drawTimelineRuler(context, x, y, contentX, contentWidth);
        context.enableScissor(contentX + 1, contentY, contentX + contentWidth - 1, contentBottom);
        drawTimelineGrid(context, contentX, contentY, contentWidth, contentHeight);
        context.disableScissor();
        drawAudioStrip(context, x, audioY, width, TIMELINE_AUDIO_STRIP_HEIGHT, contentX, contentWidth);

        List<TimelineLaneLayout> layouts = buildTimelineLaneLayouts(contentY);
        for (int index = 0; index < layouts.size(); index++) {
            TimelineLaneLayout layout = layouts.get(index);
            int laneTop = layout.top();
            int laneBottom = laneTop + layout.height() - 1;
            if (laneTop >= contentBottom || laneBottom < contentY) {
                if (laneTop >= contentBottom) break;
                continue;
            }
            laneTop = Math.max(laneTop, contentY);
            laneBottom = Math.min(laneBottom, contentBottom);
            int fill = index % 2 == 0 ? 0x331F1F1F : 0x33272727;
            int gutterFill = index % 2 == 0 ? 0xCC1B232D : 0xCC202A35;
            context.fill(x, laneTop, x + width, laneBottom, fill);
            context.fill(x, laneTop, contentX, laneBottom, gutterFill);
        }

        context.enableScissor(x + 1, contentY, x + width - 1, contentBottom);
        for (TimelineLaneLayout layout : layouts) {
            int laneTop = layout.top();
            int laneBottom = laneTop + layout.height() - 1;
            if (laneTop >= contentBottom || laneBottom < contentY) {
                if (laneTop >= contentBottom) break;
                continue;
            }
            drawTimelineLane(context, layout, x, contentX, contentWidth);
        }
        context.disableScissor();

        if (Double.isFinite(snapGuideBeat)) {
            int snapX = beatToScreen(contentX, contentWidth, snapGuideBeat);
            context.enableScissor(contentX + 1, y, contentX + contentWidth - 1, contentBottom);
            context.fill(snapX, y, snapX + 1, contentBottom, 0xFF7CE8FF);
            context.disableScissor();
        }

        if (selectionBox != null) {
            int left = Math.max(contentX, Math.min(selectionBox.startX(), selectionBox.endX()));
            int right = Math.min(contentX + contentWidth, Math.max(selectionBox.startX(), selectionBox.endX()));
            int top = Math.max(contentY, Math.min(selectionBox.startY(), selectionBox.endY()));
            int bottom = Math.min(contentBottom, Math.max(selectionBox.startY(), selectionBox.endY()));
            if (right > left && bottom > top) {
                context.fill(left, top, right, bottom, 0x224FC3F7);
                drawOutline(context, left, top, right - left, bottom - top, 0xFF7CE8FF);
            }
        }

        int playheadX = beatToScreen(contentX, contentWidth, state.playheadBeat());
        context.enableScissor(contentX + 1, y, contentX + contentWidth - 1, y + height - 1);
        context.fill(playheadX, y, playheadX + 2, y + height, 0xFFFF4444);
        context.disableScissor();
        context.drawText(textRenderer, Text.literal("Timeline"), x + 10, y + 7, UI_TEXT, false);
    }

    private void drawTimelineRuler(DrawContext context, int x, int y, int contentX, int contentWidth) {
        TrackData rulerTrack = state.selectedTrack();
        double trackStep = rulerTrack == null ? timelineGridStep() : trackGridStep(rulerTrack);
        double gridStep = rulerTrack != null && contentWidth * trackStep / Math.max(1.0, state.beatsPerScreen()) >= 28.0
                ? trackStep
                : timelineGridStep();
        double startBeat = Math.floor(state.visibleStartBeat() / gridStep) * gridStep;
        double endBeat = state.visibleEndBeat() + gridStep;
        int previousLabelX = Integer.MIN_VALUE;
        context.enableScissor(contentX + 1, y, contentX + contentWidth - 1, y + TIMELINE_RULER_HEIGHT);
        for (double beat = startBeat; beat <= endBeat; beat += gridStep) {
            int lineX = beatToScreen(contentX, contentWidth, beat);
            int tickHeight = isMeasureBeat(beat) ? TIMELINE_RULER_HEIGHT - 3 : isWholeBeat(beat) ? TIMELINE_RULER_HEIGHT - 8 : TIMELINE_RULER_HEIGHT - 12;
            int tickColor = isMeasureBeat(beat) ? 0xC4FFFFFF : isWholeBeat(beat) ? 0x88CDE3F6 : 0x44678096;
            context.fill(lineX, y + TIMELINE_RULER_HEIGHT - tickHeight, lineX + 1, y + TIMELINE_RULER_HEIGHT, tickColor);
            if (lineX - previousLabelX > 42 && (isWholeBeat(beat) || rulerTrack != null)) {
                String label = rulerTrack == null || isWholeBeat(beat) ? rulerBeatLabel(beat) : subdivisionLabel(beat, rulerTrack);
                context.drawText(textRenderer, Text.literal(label), lineX + 3, y + 8, isWholeBeat(beat) ? 0xE7F4FF : 0x8EAABD, false);
                previousLabelX = lineX;
            }
        }
        for (BpmPoint bpm : state.level().meta().bpms()) {
            int markerX = beatToScreen(contentX, contentWidth, bpm.beat());
            context.fill(markerX, y, markerX + 1, y + TIMELINE_RULER_HEIGHT, 0xFF5DE1FF);
            int labelX = clamp(markerX + 4, contentX + 4, contentX + Math.max(4, contentWidth - 64));
            context.drawText(textRenderer, Text.literal(format(bpm.bpm()) + " BPM"), labelX, y + 2, 0xFF8EEFFF, false);
        }
        context.disableScissor();
        int zoomBarX = contentX + contentWidth - TIMELINE_ZOOM_BAR_WIDTH - 8;
        int zoomBarY = y + 7;
        context.drawText(textRenderer, Text.literal("Zoom"), zoomBarX - 32, y + 4, 0x9BC1D7, false);
        context.fill(zoomBarX, zoomBarY, zoomBarX + TIMELINE_ZOOM_BAR_WIDTH, zoomBarY + TIMELINE_ZOOM_BAR_HEIGHT, 0x66354757);
        int handleX = zoomBarX + (int) Math.round(((state.beatsPerScreen() - 4.0) / 124.0) * (TIMELINE_ZOOM_BAR_WIDTH - 8));
        context.fill(handleX, zoomBarY - 2, handleX + 8, zoomBarY + TIMELINE_ZOOM_BAR_HEIGHT + 2, 0xFF9AD9FF);
        context.drawText(textRenderer, Text.literal(format(state.beatsPerScreen()) + " beats"), zoomBarX - 88, y + 4, 0xCBE8F8, false);
    }

    private void drawTimelineGrid(DrawContext context, int contentX, int contentY, int contentWidth, int contentHeight) {
        double gridStep = timelineGridStep();
        double startBeat = Math.floor(state.visibleStartBeat() / gridStep) * gridStep;
        double endBeat = state.visibleEndBeat() + gridStep;
        for (double beat = startBeat; beat <= endBeat; beat += gridStep) {
            int lineX = beatToScreen(contentX, contentWidth, beat);
            int color = isMeasureBeat(beat) ? 0x48FFFFFF : isWholeBeat(beat) ? 0x245D7687 : 0x16303D47;
            context.fill(lineX, contentY, lineX + 1, contentY + contentHeight, color);
        }
        for (BpmPoint bpm : state.level().meta().bpms()) {
            int markerX = beatToScreen(contentX, contentWidth, bpm.beat());
            context.fill(markerX, contentY, markerX + 1, contentY + contentHeight, 0x7F5DE1FF);
        }
    }

    private boolean isInsideTimelineZoomBar(double mouseX, double mouseY, int contentX, int contentWidth, int rulerY) {
        int zoomBarX = contentX + contentWidth - TIMELINE_ZOOM_BAR_WIDTH - 8;
        int zoomBarY = rulerY + 7;
        return isInside(mouseX, mouseY, zoomBarX, zoomBarY - 2, TIMELINE_ZOOM_BAR_WIDTH, TIMELINE_ZOOM_BAR_HEIGHT + 4);
    }

    private void setTimelineZoomFromMouseX(double mouseX, int contentX, int contentWidth) {
        int zoomBarX = contentX + contentWidth - TIMELINE_ZOOM_BAR_WIDTH - 8;
        double progress = (mouseX - zoomBarX) / Math.max(1.0, TIMELINE_ZOOM_BAR_WIDTH);
        progress = Math.max(0.0, Math.min(1.0, progress));
        state.setBeatsPerScreen(4.0 + progress * 124.0);
        state.setStatus("Timeline zoom: " + format(state.beatsPerScreen()) + " beats");
    }

    private double timelineGridStep() {
        if (state.beatsPerScreen() <= 8.0) {
            return 0.25;
        }
        if (state.beatsPerScreen() <= 16.0) {
            return 0.5;
        }
        if (state.beatsPerScreen() <= 48.0) {
            return 1.0;
        }
        if (state.beatsPerScreen() <= 96.0) {
            return 2.0;
        }
        return 4.0;
    }

    private double trackGridStep(TrackData track) {
        int division = track == null ? 16 : track.beatDivision();
        return Math.max(1.0 / 64.0, 4.0 / Math.max(1, division));
    }

    private double snapHoldLength(double length, TrackData track) {
        double step = trackGridStep(track);
        return Math.max(step, Math.round(Math.max(0.0, length) / step) * step);
    }

    private boolean isWholeBeat(double beat) {
        return Math.abs(beat - Math.rint(beat)) < 1.0E-6;
    }

    private boolean isMeasureBeat(double beat) {
        return isWholeBeat(beat) && Math.floorMod((int) Math.round(beat), 4) == 0;
    }

    private String rulerBeatLabel(double beat) {
        int wholeBeat = (int) Math.round(beat);
        int measure = Math.floorDiv(wholeBeat, 4);
        int beatInMeasure = Math.floorMod(wholeBeat, 4) + 1;
        return measure + "." + beatInMeasure;
    }

    private String subdivisionLabel(double beat, TrackData track) {
        int beatIndex = (int) Math.floor(beat);
        double fraction = beat - beatIndex;
        int division = track == null ? 16 : track.beatDivision();
        int partsPerBeat = Math.max(1, division / 4);
        int part = clamp((int) Math.round(fraction * partsPerBeat), 0, partsPerBeat - 1) + 1;
        return "B" + beatIndex + " " + part + "/" + partsPerBeat;
    }

    private void drawWaveform(DrawContext context, int x, int y, int width, int height) {
        if (!state.audioAnalysis().hasWaveform()) {
            return;
        }

        float[] waveform = state.audioAnalysis().waveform();
        long lengthMillis = state.audioAnalysis().lengthMillis();
        int centerY = y + height / 2;
        double visibleStart = state.visibleStartBeat();
        double visibleEnd = state.visibleEndBeat();

        for (int px = 0; px < width; px++) {
            double beat = screenToBeat(x, width, x + px);
            if (beat < visibleStart - 0.25 || beat > visibleEnd + 0.25) {
                continue;
            }
            long timeMillis = state.timing().beatToMillis(beat);
            if (timeMillis < 0 || timeMillis > lengthMillis || lengthMillis <= 0) {
                continue;
            }
            double t = timeMillis / (double) lengthMillis;
            int idx = (int) Math.round(t * (waveform.length - 1));
            idx = Math.max(0, Math.min(waveform.length - 1, idx));
            int amplitude = Math.max(1, Math.round(waveform[idx] * (height / 2.6f)));
            context.fill(x + px, centerY - amplitude, x + px + 1, centerY + amplitude, 0x3355CC88);
        }
    }

    private void drawAudioStrip(DrawContext context, int x, int y, int width, int height, int contentX, int contentWidth) {
        context.fill(x, y, contentX, y + height, 0xE019232C);
        context.drawText(textRenderer, Text.literal("Audio"), x + 10, y + 8, UI_TEXT, false);
        drawTrimmedText(context, audioSummary(), x + 10, y + 22, TIMELINE_LABEL_WIDTH - 18, UI_MUTED);
        context.fill(contentX, y, contentX + contentWidth, y + height, 0xD3131A20);
        context.enableScissor(contentX + 1, y + 1, contentX + contentWidth - 1, y + height - 1);
        drawTimelineGrid(context, contentX, y, contentWidth, height);
        drawWaveform(context, contentX, y + 3, contentWidth, height - 6);
        context.disableScissor();
    }

    private void drawTrackBar(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, UI_PANEL_STRONG);
        drawOutline(context, x, y, width, height, UI_BORDER);

        int cursorX = x + 6;
        for (TrackData track : state.visibleTracks()) {
            int tabWidth = 88;
            boolean selected = state.selectedTrack() == track;
            int fill = selected ? 0xCC2F5F9D : 0x66303030;
            context.fill(cursorX, y + 4, cursorX + tabWidth, y + height - 4, fill);
            context.fill(cursorX + 4, y + 6, cursorX + 16, y + 18, 0x55000000);
            context.drawText(textRenderer, Text.literal(state.isTrackExpanded(track) ? "-" : "+"), cursorX + 8, y + 10, 0xFFFFFFFF, false);
            context.drawText(textRenderer, Text.literal("TrackID " + track.id()), cursorX + 20, y + 10, 0xFFFFFFFF, false);
            cursorX += tabWidth + 4;
        }

        int addWidth = 70;
        context.fill(x + width - addWidth - 6, y + 4, x + width - 6, y + height - 4, 0x66557733);
        context.drawText(textRenderer, Text.literal("+ Track"), x + width - addWidth + 8, y + 10, 0xFFFFFFFF, false);
    }

    private void drawPropertyPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, UI_PANEL);
        drawOutline(context, x, y, width, height, UI_BORDER);
        context.drawText(textRenderer, Text.literal("Inspector"), x + 10, y + 9, UI_ACCENT, false);
        drawTrimmedText(context, propertyTitle().getString(), x + 10, y + 21, width - 20, UI_TEXT);
        drawTrimmedText(context, selectedObjectSummary(), x + 10, y + 33, width - 20, UI_MUTED);
        context.enableScissor(x + 1, y + 47, x + width - 1, y + height - 42);
        for (LabeledField field : activeFieldGroup()) {
            if (field.sectionY >= 0 && field.section != null && !field.section.isBlank()) {
                drawPropertySectionHeader(context, propertySectionKey(state.selection().kind(), field.section), field.section, x + 8, field.sectionY, width - 16);
            }
            if (field.widget.visible) {
                context.drawText(textRenderer, Text.literal(displayLabel(field)), field.widget.getX(), field.widget.getY() - 18, UI_TEXT, false);
                if (field.hint != null && !field.hint.isBlank()) {
                    drawTrimmedText(context, field.hint, field.widget.getX(), field.widget.getY() - 9, rightPanelInnerWidth(), UI_DIM);
                }
            }
        }
        drawTrackEventEditors(context);
        context.disableScissor();
        context.fill(x + 1, y + height - 41, x + width - 1, y + height - 40, UI_BORDER_SOFT);
        drawTrimmedText(context, "Scroll position " + Math.abs(propertyScroll), x + 10, y + height - 18, width - 20, UI_DIM);
    }

    private boolean handlePropertyPanelClick(Click click, double mouseX, double mouseY) {
        for (PropertySectionHeader header : propertySectionHeaders) {
            if (isInside(mouseX, mouseY, header.x(), header.y(), header.width(), header.height())) {
                if (!collapsedPropertySections.add(header.key())) {
                    collapsedPropertySections.remove(header.key());
                }
                layoutPropertyFields();
                return true;
            }
        }
        if (state.selection().kind() == EditorSelection.Kind.TRACK) {
            for (TrackEventEditor editor : trackEventEditors) {
                for (int index = 0; index < editor.rows.size(); index++) {
                    TrackEventRow row = editor.rows.get(index);
                    if (!row.visible) {
                        continue;
                    }
                    if (isInside(mouseX, mouseY, row.dragX, row.y, row.rowRight - row.dragX, 18)) {
                        selectTrackEventRow(editor, row, isControlDown());
                        layoutPropertyFields();
                        return true;
                    }
                    if (isInside(mouseX, mouseY, row.dragX, row.y, EVENT_DRAG_WIDTH, 18)) {
                        draggingTrackEventEditor = editor;
                        draggingTrackEventSourceIndex = index;
                        draggingTrackEventTargetIndex = index;
                        draggingTrackEventMouseY = mouseY;
                        dragMode = DragMode.TRACK_EVENT_ROW;
                        return true;
                    }
                }
            }
            if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT && isInsideTrackEventEditorArea(mouseX, mouseY)) {
                trackEventSelectionBox = new SelectionBox((int) mouseX, (int) mouseY, (int) mouseX, (int) mouseY);
                trackEventSelectionAdditive = isControlDown();
                dragMode = DragMode.TRACK_EVENT_BOX_SELECT;
                closeTrackEventEasingPopup();
                return true;
            }
        }
        return false;
    }

    private boolean handleTrackEventEasingPopupClick(double mouseX, double mouseY) {
        if (easingPopupRow == null) {
            return false;
        }
        EasingPopupLayout layout = buildEasingPopupLayout();
        if (isInside(mouseX, mouseY, easingPopupSearchField.getX(), easingPopupSearchField.getY(), easingPopupSearchField.getWidth(), easingPopupSearchField.getHeight())) {
            return false;
        }
        if (!isInside(mouseX, mouseY, easingPopupX, easingPopupY, layout.width(), layout.height())) {
            closeTrackEventEasingPopup();
            return false;
        }
        for (EasingPopupEntry entry : layout.entries()) {
            if (isInside(mouseX, mouseY, entry.x(), entry.y(), entry.width(), entry.height())) {
                easingPopupRow.easingType = entry.easingType();
                easingPopupRow.easingButton.setMessage(Text.literal(trackEaseDisplayLabel(entry.easingType())));
                closeTrackEventEasingPopup();
                return true;
            }
        }
        return true;
    }

    private void drawTrackEventEasingPopup(DrawContext context, int mouseX, int mouseY) {
        if (easingPopupRow == null) {
            return;
        }
        EasingPopupLayout layout = buildEasingPopupLayout();
        context.getMatrices().pushMatrix();
        context.fill(easingPopupX, easingPopupY, easingPopupX + layout.width(), easingPopupY + layout.height(), 0xEE182129);
        drawOutline(context, easingPopupX, easingPopupY, layout.width(), layout.height(), 0xFF6B93B0);
        easingPopupSearchField.render(context, mouseX, mouseY, 0.0f);
        for (EasingPopupGroup group : layout.groups()) {
            context.drawText(textRenderer, Text.literal(group.title()), easingPopupX + 6, group.y(), 0x8FD6FF, false);
            context.fill(easingPopupX + 62, group.y() + 5, easingPopupX + layout.width() - 6, group.y() + 6, 0x335F86A1);
        }
        for (EasingPopupEntry entry : layout.entries()) {
            boolean hovered = mouseX >= entry.x() && mouseX < entry.x() + entry.width() && mouseY >= entry.y() && mouseY < entry.y() + entry.height();
            boolean selected = easingPopupRow.easingType == entry.easingType();
            if (hovered || selected) {
                context.fill(entry.x() - 1, entry.y(), entry.x() + entry.width(), entry.y() + entry.height(), selected ? 0x665AA8D8 : 0x334B6478);
            }
            context.drawText(textRenderer, Text.literal(entry.label()), entry.x(), entry.y() + 2, selected ? 0xFFFFFF : 0xC7DAE7, false);
        }
        if (layout.entries().isEmpty()) {
            context.drawText(textRenderer, Text.literal("No easing matches"), easingPopupX + 8, easingPopupSearchField.getY() + 26, 0xC7DAE7, false);
        }
        context.getMatrices().popMatrix();
    }

    private EasingPopupLayout buildEasingPopupLayout() {
        int popupWidth = rightPanelInnerWidth();
        int padding = 6;
        int optionHeight = 12;
        int searchHeight = 18;
        easingPopupX = rightPanelX() + 8;
        int searchY = easingPopupY + padding;
        easingPopupSearchField.setPosition(easingPopupX + padding, searchY);
        easingPopupSearchField.setWidth(popupWidth - padding * 2);
        easingPopupSearchField.visible = true;
        easingPopupSearchField.active = true;

        String filter = easingPopupSearchField.getText().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        Map<String, List<cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType>> groups = filteredEasingGroups(filter);
        List<EasingPopupGroup> groupLayouts = new ArrayList<>();
        List<EasingPopupEntry> entries = new ArrayList<>();
        int y = searchY + searchHeight + 8;
        for (Map.Entry<String, List<cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType>> group : groups.entrySet()) {
            if (group.getValue().isEmpty()) {
                continue;
            }
            groupLayouts.add(new EasingPopupGroup(group.getKey(), y));
            y += 12;
            for (int index = 0; index < group.getValue().size(); index++) {
                int column = index % 2;
                int row = index / 2;
                cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType easingType = group.getValue().get(index);
                int columnWidth = (popupWidth - padding * 2) / 2;
                int optionX = easingPopupX + padding + column * columnWidth;
                int optionY = y + row * optionHeight;
                int optionWidth = columnWidth - 4;
                entries.add(new EasingPopupEntry(easingType, shortTrackEasePopupLabel(easingType), optionX, optionY, optionWidth, optionHeight));
            }
            y += Math.max(1, (int) Math.ceil(group.getValue().size() / 2.0)) * optionHeight + 6;
        }
        if (entries.isEmpty()) {
            y += 12;
        }
        int popupHeight = Math.max(searchHeight + 16, y - easingPopupY);
        int clampedY = clamp(easingPopupY, 84, Math.max(84, height - popupHeight - 40));
        if (clampedY != easingPopupY) {
            easingPopupY = clampedY;
            return buildEasingPopupLayout();
        }
        return new EasingPopupLayout(popupWidth, popupHeight, groupLayouts, entries);
    }

    private Map<String, List<cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType>> filteredEasingGroups(String filter) {
        Map<String, List<cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType>> groups = new LinkedHashMap<>();
        groups.put("Basic", new ArrayList<>());
        groups.put("Ease In", new ArrayList<>());
        groups.put("Ease Out", new ArrayList<>());
        groups.put("Ease In/Out", new ArrayList<>());
        for (cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType easingType : cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.values()) {
            String label = trackEaseDisplayLabel(easingType).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
            if (!filter.isBlank() && !label.contains(filter)) {
                continue;
            }
            groups.get(trackEaseGroup(easingType)).add(easingType);
        }
        return groups;
    }

    private String trackEaseGroup(cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType easingType) {
        String name = easingType.name();
        if (name.equals("LINEAR")) {
            return "Basic";
        }
        if (name.startsWith("IN_OUT_")) {
            return "Ease In/Out";
        }
        if (name.startsWith("IN_")) {
            return "Ease In";
        }
        if (name.startsWith("OUT_")) {
            return "Ease Out";
        }
        return "Basic";
    }

    private boolean isInsideTrackEventEditorArea(double mouseX, double mouseY) {
        for (TrackEventEditor editor : trackEventEditors) {
            if (editor.titleY < 0) {
                continue;
            }
            int top = editor.columnsY;
            int bottom = editor.rows.stream().filter(row -> row.visible).mapToInt(row -> row.y + 18).max().orElse(editor.columnsY + 18);
            int left = editor.baseX;
            if (isInside(mouseX, mouseY, left, top, rightPanelWidth() - 24, Math.max(20, bottom - top + 4))) {
                return true;
            }
        }
        return false;
    }

    private void applyTrackEventSelectionBox() {
        int left = Math.min(trackEventSelectionBox.startX(), trackEventSelectionBox.endX());
        int right = Math.max(trackEventSelectionBox.startX(), trackEventSelectionBox.endX());
        int top = Math.min(trackEventSelectionBox.startY(), trackEventSelectionBox.endY());
        int bottom = Math.max(trackEventSelectionBox.startY(), trackEventSelectionBox.endY());
        LinkedHashSet<TrackEventRow> next = trackEventSelectionAdditive ? new LinkedHashSet<>(selectedTrackEventRows) : new LinkedHashSet<>();
        for (TrackEventEditor editor : trackEventEditors) {
            for (TrackEventRow row : editor.rows) {
                if (!row.visible) {
                    continue;
                }
                if (rectanglesIntersect(left, top, right, bottom, row.dragX, row.y, row.rowRight, row.y + 18)) {
                    next.add(row);
                }
            }
        }
        selectedTrackEventRows.clear();
        selectedTrackEventRows.addAll(next);
        if (selectedTrackEventRows.isEmpty() && !trackEventSelectionAdditive) {
            closeTrackEventEasingPopup();
        }
        layoutPropertyFields();
    }

    private void selectTrackEventRow(TrackEventEditor editor, TrackEventRow row, boolean additive) {
        if (!additive) {
            selectedTrackEventRows.clear();
            selectedTrackEventRows.add(row);
            return;
        }
        if (!selectedTrackEventRows.add(row)) {
            selectedTrackEventRows.remove(row);
        }
        if (selectedTrackEventRows.isEmpty()) {
            selectedTrackEventRows.add(row);
        }
    }

    private boolean hasSelectedTrackEventRows(TrackEventEditor editor) {
        for (TrackEventRow row : editor.rows) {
            if (selectedTrackEventRows.contains(row)) {
                return true;
            }
        }
        return false;
    }

    private void openTrackEventEasingPopup(TrackEventRow row) {
        if (row == null) {
            return;
        }
        easingPopupRow = row;
        easingPopupX = rightPanelX() + 8;
        easingPopupY = row.easingButton.getY() + 20;
        easingPopupSearchField.setText("");
        easingPopupSearchField.visible = true;
        easingPopupSearchField.active = true;
        easingPopupSearchField.setFocused(true);
    }

    private void closeTrackEventEasingPopup() {
        easingPopupRow = null;
        if (easingPopupSearchField != null) {
            easingPopupSearchField.visible = false;
            easingPopupSearchField.active = false;
            easingPopupSearchField.setFocused(false);
        }
    }

    private boolean deleteSelectedTrackEventRows(EventLaneType eventType) {
        TrackData track = state.selection().track();
        if (track == null) {
            return false;
        }
        boolean deletedAny = false;
        for (TrackEventEditor editor : trackEventEditors) {
            if (eventType != null && editor.eventType() != eventType) {
                continue;
            }
            boolean shouldClearLane = eventType != null || hasSelectedTrackEventRows(editor);
            if (!shouldClearLane) {
                continue;
            }
            List<NumEventData> events = eventsForLane(track, editor.eventType());
            if (!events.isEmpty()) {
                events.clear();
                deletedAny = true;
            }
            selectedTrackEventRows.removeAll(editor.rows);
        }
        if (deletedAny) {
            state.markDirty();
            state.setStatus("Cleared selected automation lane(s)");
            populateFieldsFromSelection();
            layoutPropertyFields();
        }
        return deletedAny;
    }

    private void duplicateSelectedTrackEventRows(EventLaneType eventType) {
        TrackData track = state.selection().track();
        if (track == null) {
            return;
        }
        for (TrackEventEditor editor : trackEventEditors) {
            if (editor.eventType() != eventType) {
                continue;
            }
            List<NumEventData> events = eventsForLane(track, editor.eventType());
            List<Integer> selectedIndices = new ArrayList<>();
            for (int index = 0; index < editor.rows.size(); index++) {
                if (selectedTrackEventRows.contains(editor.rows.get(index)) && index < events.size()) {
                    selectedIndices.add(index);
                }
            }
            if (selectedIndices.isEmpty()) {
                return;
            }
            state.setStatus(editor.eventType().label + " is a single-event lane; use Set or edit the card values");
            return;
        }
    }

    private String propertySectionKey(EditorSelection.Kind kind, String section) {
        return kind.name() + ":" + section;
    }

    private boolean isSectionCollapsed(String key) {
        return collapsedPropertySections.contains(key);
    }

    private void drawPropertySectionHeader(DrawContext context, String key, String title, int x, int y, int width) {
        boolean collapsed = isSectionCollapsed(key);
        context.drawText(textRenderer, Text.literal((collapsed ? "+ " : "- ") + title), x, y, 0x8FD6FF, false);
        context.fill(x + 64, y + 5, x + width, y + 6, 0x335F86A1);
    }

    private int layoutTrackEventEditors(int panelX, int currentY) {
        EditorLayout layout = editorLayout();
        int viewportTop = layout.topY() + 48;
        int viewportBottom = layout.topY() + layout.panelHeight() - 50;
        for (TrackEventEditor editor : trackEventEditors) {
            setTrackEventEditorVisible(editor, false);
        }
        if (state.selection().kind() != EditorSelection.Kind.TRACK || state.selection().track() == null) {
            return currentY;
        }
        String lastSection = null;
        for (TrackEventEditor editor : trackEventEditors) {
            if (!editor.section().equals(lastSection)) {
                currentY += 10;
                editor.sectionY = currentY;
                propertySectionHeaders.add(new PropertySectionHeader(propertySectionKey(EditorSelection.Kind.TRACK, editor.section()), editor.section(), panelX, currentY, rightPanelInnerWidth(), 12));
                currentY += 12;
                lastSection = editor.section();
            } else {
                editor.sectionY = -1;
            }
            editor.baseX = panelX;
            editor.titleY = -1;
            editor.columnsY = -1;
            boolean collapsed = isSectionCollapsed(propertySectionKey(EditorSelection.Kind.TRACK, editor.section()));
            if (collapsed) {
                setTrackEventEditorVisible(editor, false);
                continue;
            }
            editor.titleY = currentY;
            List<NumEventData> laneEvents = eventsForLane(state.selection().track(), editor.eventType());
            editor.addButton.visible = true;
            editor.addButton.active = true;
            editor.addButton.setPosition(panelX + rightPanelInnerWidth() - 92, currentY - 3);
            editor.duplicateButton.visible = false;
            editor.duplicateButton.active = false;
            editor.deleteButton.visible = true;
            editor.deleteButton.active = !laneEvents.isEmpty();
            editor.deleteButton.setPosition(panelX + rightPanelInnerWidth() - 42, currentY - 3);
            currentY += 14;
            editor.columnsY = currentY;
            currentY += 12;
            int rowY = currentY;
            if (!editor.rows.isEmpty()) {
                TrackEventRow row = editor.rows.getFirst();
                layoutTrackEventRow(panelX, rowY, row);
                boolean rowInsideViewport = rowY >= viewportTop && rowY + 18 <= viewportBottom;
                setTrackEventRowVisible(row, rowInsideViewport);
                rowY += 22;
            }
            currentY = rowY + 2;
        }
        return currentY;
    }

    private void drawTrackEventEditors(DrawContext context) {
        if (state.selection().kind() != EditorSelection.Kind.TRACK) {
            return;
        }
        for (TrackEventEditor editor : trackEventEditors) {
            if (editor.sectionY >= 0) {
                drawPropertySectionHeader(context, propertySectionKey(EditorSelection.Kind.TRACK, editor.section()), editor.section(), editor.baseX, editor.sectionY, rightPanelInnerWidth());
            }
            if (editor.titleY < 0) {
                continue;
            }
            int titleX = editor.baseX;
            TrackData track = state.selection().track();
            int eventCount = track == null ? 0 : eventsForLane(track, editor.eventType()).size();
            context.drawText(textRenderer, Text.literal(editor.eventType().label), titleX, editor.titleY, editor.eventType().color, false);
            String label = eventCount > 1 ? "Automation card  |  multiple saved events, Apply keeps card" : "Automation card  |  Start  End  From  To  Ease";
            context.drawText(textRenderer, Text.literal(label), titleX, editor.columnsY, eventCount > 1 ? UI_WARN : 0x7F97A8, false);
            for (int index = 0; index < editor.rows.size(); index++) {
                TrackEventRow row = editor.rows.get(index);
                if (!row.visible) {
                    continue;
                }
                context.fill(row.dragX, row.y, row.rowRight, row.y + 18, 0x18161D24);
                context.drawText(textRenderer, Text.literal("A"), row.dragX + 4, row.y + 5, 0x8AA8BA, false);
            }
        }
    }

    private void layoutTrackEventRow(int x, int y, TrackEventRow row) {
        row.y = y;
        row.dragX = x;
        int gap = 4;
        int numericWidth = relativeTrackEventCellWidth();
        int easeWidth = relativeTrackEventEaseWidth(numericWidth);
        int cursorX = x + EVENT_DRAG_WIDTH + gap;
        row.startBeat.setPosition(cursorX, y);
        row.startBeat.setWidth(numericWidth);
        cursorX += numericWidth + gap;
        row.endBeat.setPosition(cursorX, y);
        row.endBeat.setWidth(numericWidth);
        cursorX += numericWidth + gap;
        row.startValue.setPosition(cursorX, y);
        row.startValue.setWidth(numericWidth);
        cursorX += numericWidth + gap;
        row.endValue.setPosition(cursorX, y);
        row.endValue.setWidth(numericWidth);
        cursorX += numericWidth + gap;
        row.easingButton.setPosition(cursorX, y);
        row.easingButton.setWidth(easeWidth);
        row.rowRight = row.easingButton.getX() + easeWidth;
    }

    private int relativeTrackEventCellWidth() {
        int gap = 4;
        int contentWidth = rightPanelInnerWidth() - EVENT_DRAG_WIDTH - gap;
        int easeWidth = clamp((int) Math.round(contentWidth * 0.30), 64, 100);
        return Math.max(24, (contentWidth - easeWidth - gap * 4) / 4);
    }

    private int relativeTrackEventEaseWidth(int numericWidth) {
        int gap = 4;
        int contentWidth = rightPanelInnerWidth() - EVENT_DRAG_WIDTH - gap;
        return Math.max(60, contentWidth - numericWidth * 4 - gap * 4);
    }

    private void setTrackEventEditorVisible(TrackEventEditor editor, boolean visible) {
        editor.addButton.visible = visible;
        editor.addButton.active = visible;
        editor.duplicateButton.visible = visible;
        editor.duplicateButton.active = visible && hasSelectedTrackEventRows(editor);
        editor.deleteButton.visible = visible;
        editor.deleteButton.active = visible && hasSelectedTrackEventRows(editor);
        for (TrackEventRow row : editor.rows) {
            row.visible = visible;
            row.startBeat.visible = visible;
            row.startBeat.active = visible;
            row.endBeat.visible = visible;
            row.endBeat.active = visible;
            row.startValue.visible = visible;
            row.startValue.active = visible;
            row.endValue.visible = visible;
            row.endValue.active = visible;
            row.easingButton.visible = visible;
            row.easingButton.active = visible;
            row.copyButton.visible = false;
            row.copyButton.active = false;
            row.removeButton.visible = false;
            row.removeButton.active = false;
        }
    }

    private void hideTrackEventEditors() {
        for (TrackEventEditor editor : trackEventEditors) {
            editor.sectionY = -1;
            editor.titleY = -1;
            editor.columnsY = -1;
            setTrackEventEditorVisible(editor, false);
        }
    }

    private void syncTrackEventEditors(TrackData track) {
        for (TrackEventEditor editor : trackEventEditors) {
            List<NumEventData> events = eventsForLane(track, editor.eventType());
            ensureTrackEventRows(editor, 1);
            for (int index = 0; index < editor.rows.size(); index++) {
                TrackEventRow row = editor.rows.get(index);
                boolean active = index == 0;
                row.visible = active;
                row.startBeat.visible = active;
                row.endBeat.visible = active;
                row.startValue.visible = active;
                row.endValue.visible = active;
                row.easingButton.visible = active;
                row.copyButton.visible = false;
                row.removeButton.visible = false;
                row.startBeat.active = active;
                row.endBeat.active = active;
                row.startValue.active = active;
                row.endValue.active = active;
                row.easingButton.active = active;
                row.copyButton.active = false;
                row.removeButton.active = false;
                if (!active) {
                    continue;
                }
                if (events.isEmpty()) {
                    row.startBeat.setText("");
                    row.endBeat.setText("");
                    row.startValue.setText("");
                    row.endValue.setText("");
                    row.easingType = cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.LINEAR;
                } else {
                    NumEventData event = events.getFirst();
                    row.startBeat.setText(format(event.startBeat()));
                    row.endBeat.setText(format(event.endBeat()));
                    row.startValue.setText(format(event.startValue()));
                    row.endValue.setText(format(event.endValue()));
                    row.easingType = event.easingType();
                }
                row.easingButton.setMessage(Text.literal(trackEaseDisplayLabel(row.easingType)));
            }
        }
    }

    private void applyTrackEventEditors(TrackData track) {
        for (TrackEventEditor editor : trackEventEditors) {
            List<NumEventData> updated = new ArrayList<>();
            TrackEventRow row = editor.rows.isEmpty() ? null : editor.rows.getFirst();
            if (row != null && !row.startBeat.getText().isBlank()) {
                updated.add(new NumEventData(
                        parseDouble(row.startBeat.getText()),
                        parseDouble(row.endBeat.getText()),
                        parseDouble(row.startValue.getText()),
                        parseDouble(row.endValue.getText()),
                        row.easingType
                ));
            }
            replaceNumEvents(eventsForLane(track, editor.eventType()), updated);
        }
    }

    private void ensureTrackEventRows(TrackEventEditor editor, int count) {
        while (editor.rows.size() < count) {
            editor.rows.add(createTrackEventRow(editor));
        }
    }

    private TrackEventRow createTrackEventRow(TrackEventEditor editor) {
        TextFieldWidget startBeat = addDrawableChild(new TextFieldWidget(textRenderer, 0, 0, EVENT_CELL_WIDTH, 18, Text.literal("Start")));
        TextFieldWidget endBeat = addDrawableChild(new TextFieldWidget(textRenderer, 0, 0, EVENT_CELL_WIDTH, 18, Text.literal("End")));
        TextFieldWidget startValue = addDrawableChild(new TextFieldWidget(textRenderer, 0, 0, EVENT_CELL_WIDTH, 18, Text.literal("From")));
        TextFieldWidget endValue = addDrawableChild(new TextFieldWidget(textRenderer, 0, 0, EVENT_CELL_WIDTH, 18, Text.literal("To")));
        ButtonWidget easingButton = addDrawableChild(ButtonWidget.builder(Text.literal(trackEaseDisplayLabel(cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.LINEAR)), b -> openTrackEventEasingPopup(findTrackEventRow(editor, b))).dimensions(0, 0, EVENT_EASE_WIDTH, 18).build());
        startBeat.setMaxLength(64);
        endBeat.setMaxLength(64);
        startValue.setMaxLength(64);
        endValue.setMaxLength(64);
        ButtonWidget copyButton = addDrawableChild(ButtonWidget.builder(Text.literal("Set"), b -> copyTrackEventRow(editor, findTrackEventRow(editor, b))).dimensions(0, 0, EVENT_COPY_WIDTH, 18).build());
        ButtonWidget removeButton = addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> removeTrackEventRow(editor, findTrackEventRow(editor, b))).dimensions(0, 0, EVENT_REMOVE_WIDTH, 18).build());
        TrackEventRow row = new TrackEventRow(startBeat, endBeat, startValue, endValue, easingButton, copyButton, removeButton);
        setTrackEventRowVisible(row, false);
        return row;
    }

    private TrackEventRow findTrackEventRow(TrackEventEditor editor, ButtonWidget button) {
        for (TrackEventRow row : editor.rows) {
            if (row.removeButton == button || row.copyButton == button || row.easingButton == button) {
                return row;
            }
        }
        return null;
    }

    private void setTrackEventRowVisible(TrackEventRow row, boolean visible) {
        row.visible = visible;
        row.startBeat.visible = visible;
        row.startBeat.active = visible;
        row.endBeat.visible = visible;
        row.endBeat.active = visible;
        row.startValue.visible = visible;
        row.startValue.active = visible;
        row.endValue.visible = visible;
        row.endValue.active = visible;
        row.easingButton.visible = visible;
        row.easingButton.active = visible;
        row.copyButton.visible = false;
        row.copyButton.active = false;
        row.removeButton.visible = false;
        row.removeButton.active = false;
    }

    private void cycleTrackEventRowEasing(TrackEventRow row) {
        if (row == null) {
            return;
        }
        cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType[] values = cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.values();
        int nextIndex = (row.easingType.ordinal() + 1) % values.length;
        row.easingType = values[nextIndex];
        row.easingButton.setMessage(Text.literal(trackEaseDisplayLabel(row.easingType)));
    }

    private void copyTrackEventRow(TrackEventEditor editor, TrackEventRow row) {
        state.setStatus(editor.eventType().label + " is a single-event lane; edit this automation card directly");
    }

    private void addTrackEventRow(EventLaneType eventType) {
        TrackData track = state.selection().track();
        if (track == null) {
            return;
        }
        List<NumEventData> events = eventsForLane(track, eventType);
        double beat = state.playheadBeat();
        double startValue = defaultStartValueForEventLane(eventType);
        double endValue = startValue;
        if (!events.isEmpty()) {
            NumEventData event = events.getFirst();
            startValue = event.startValue();
            endValue = event.endValue();
        }
        events.clear();
        events.add(new NumEventData(beat, beat + 1.0, startValue, endValue, cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.LINEAR));
        state.markDirty();
        state.setStatus("Set " + eventType.label + " automation lane");
        populateFieldsFromSelection();
        layoutPropertyFields();
    }

    private void removeTrackEventRow(TrackEventEditor editor, TrackEventRow row) {
        TrackData track = state.selection().track();
        if (track == null || row == null) {
            return;
        }
        List<NumEventData> events = eventsForLane(track, editor.eventType());
        if (!events.isEmpty()) {
            events.clear();
            state.markDirty();
            state.setStatus("Cleared " + editor.eventType().label + " automation lane");
            populateFieldsFromSelection();
            layoutPropertyFields();
        }
    }

    private double defaultStartValueForEventLane(EventLaneType eventType) {
        return switch (eventType) {
            case SCALE_X, SCALE_Y, SCALE_Z -> 1.0;
            case SPEED -> 8.0;
            default -> 0.0;
        };
    }

    private void updateDraggedTrackEventTarget(double mouseY) {
        if (draggingTrackEventEditor == null || draggingTrackEventSourceIndex < 0) {
            return;
        }
        int targetIndex = visibleTrackEventCount(draggingTrackEventEditor);
        for (int index = 0; index < draggingTrackEventEditor.rows.size(); index++) {
            TrackEventRow row = draggingTrackEventEditor.rows.get(index);
            if (!row.visible) {
                continue;
            }
            if (mouseY < row.y + 9) {
                targetIndex = index;
                break;
            }
        }
        draggingTrackEventTargetIndex = Math.max(0, targetIndex);
    }

    private void applyDraggedTrackEventReorder() {
        TrackData track = state.selection().track();
        if (track == null || draggingTrackEventEditor == null || draggingTrackEventSourceIndex < 0 || draggingTrackEventTargetIndex < 0) {
            return;
        }
        List<NumEventData> events = eventsForLane(track, draggingTrackEventEditor.eventType());
        if (draggingTrackEventSourceIndex >= events.size()) {
            return;
        }
        int targetIndex = draggingTrackEventTargetIndex;
        if (targetIndex > draggingTrackEventSourceIndex) {
            targetIndex--;
        }
        targetIndex = clamp(targetIndex, 0, Math.max(0, events.size() - 1));
        if (targetIndex == draggingTrackEventSourceIndex) {
            return;
        }
        NumEventData moved = events.remove(draggingTrackEventSourceIndex);
        events.add(targetIndex, moved);
        state.markDirty();
        state.setStatus("Reordered " + draggingTrackEventEditor.eventType().label + " events");
        populateFieldsFromSelection();
        layoutPropertyFields();
    }

    private int visibleTrackEventCount(TrackEventEditor editor) {
        int count = 0;
        for (TrackEventRow row : editor.rows) {
            if (row.visible) {
                count++;
            }
        }
        return count;
    }

    private int trackEventInsertLineY(TrackEventEditor editor, int targetIndex) {
        if (editor.rows.isEmpty() || visibleTrackEventCount(editor) == 0) {
            return editor.columnsY + 14;
        }
        int visibleCount = visibleTrackEventCount(editor);
        if (targetIndex >= visibleCount) {
            for (int index = editor.rows.size() - 1; index >= 0; index--) {
                TrackEventRow row = editor.rows.get(index);
                if (row.visible) {
                    return row.y + 20;
                }
            }
        }
        return editor.rows.get(targetIndex).y - 2;
    }

    private String shortTrackEaseLabel(cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType easingType) {
        String name = easingType.name().replace('_', ' ');
        return name.length() <= 9 ? name : name.substring(0, 9);
    }

    private String trackEaseDisplayLabel(cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType easingType) {
        String name = easingType.name().replace("IN_OUT_", "InOut ")
                .replace("IN_", "In ")
                .replace("OUT_", "Out ")
                .replace('_', ' ')
                .toLowerCase(Locale.ROOT);
        String[] parts = name.split(" ");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return String.join(" ", words);
    }

    private String shortTrackEasePopupLabel(cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType easingType) {
        return easingType.name().replace('_', ' ');
    }

    private cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType parseEasing(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.LINEAR;
        }
        try {
            return cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.fromId(parseInt(trimmed));
        }
    }

    private String trackEventSection(EventLaneType eventType) {
        return switch (eventType) {
            case SPEED -> "Speed";
            case MOVE_X, MOVE_Y, MOVE_Z -> "Position";
            case ROT_X, ROT_Y, ROT_Z -> "Rotation";
            case SCALE_X, SCALE_Y, SCALE_Z -> "Scale";
        };
    }

    private boolean handleLeftPanelClick(double mouseX, double mouseY, int x, int y) {
        int mapSize = leftPanelWidth() - 16;
        if (isInside(mouseX, mouseY, x + 8, y + 32, mapSize, mapSize)) {
            if (handleCurrentFrameMapClick(mouseX, mouseY, x + 8, y + 32, mapSize, mapSize)) {
                return true;
            }
        }
        int rowY = y + 32 + mapSize + 30;
        for (TrackData track : state.tracks()) {
            if (isInside(mouseX, mouseY, x + 10, rowY + 4, 14, 14)) {
                state.toggleTrackExpanded(track);
                return true;
            }
            if (isInside(mouseX, mouseY, x + 6, rowY, leftPanelWidth() - 12, 22)) {
                state.setSelection(EditorSelection.track(track));
                return true;
            }
            rowY += 30;
        }
        return false;
    }

    private boolean handleTrackBarClick(double mouseX, double mouseY, int x, int y, int width, int height) {
        int cursorX = x + 6;
        for (TrackData track : state.visibleTracks()) {
            int tabWidth = 88;
            if (isInside(mouseX, mouseY, cursorX + 4, y + 6, 12, 12)) {
                state.toggleTrackExpanded(track);
                return true;
            }
            if (isInside(mouseX, mouseY, cursorX, y + 4, tabWidth, height - 8)) {
                state.setSelection(EditorSelection.track(track));
                return true;
            }
            cursorX += tabWidth + 4;
        }

        int addWidth = 70;
        if (isInside(mouseX, mouseY, x + width - addWidth - 6, y + 4, addWidth, height - 8)) {
            state.addTrack();
            return true;
        }
        return false;
    }

    private boolean handleTimelineClick(Click click, int x, int y, int width, int height) {
        double mouseX = click.x();
        double mouseY = click.y();
        int contentX = x + TIMELINE_LABEL_WIDTH;
        int contentWidth = Math.max(48, width - TIMELINE_LABEL_WIDTH);
        int audioY = y + height - TIMELINE_AUDIO_STRIP_HEIGHT;
        if (mouseY < y + TIMELINE_RULER_HEIGHT) {
            if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT && isInsideTimelineZoomBar(mouseX, mouseY, contentX, contentWidth, y)) {
                setTimelineZoomFromMouseX(mouseX, contentX, contentWidth);
            } else {
                state.seekToBeat(snapBeat(screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX))));
            }
            return true;
        }
        if (mouseY >= audioY) {
            state.seekToBeat(snapBeat(screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX))));
            return true;
        }
        double rawBeat = screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX));
        double beat = snapBeat(rawBeat);
        List<TimelineLaneLayout> layouts = buildTimelineLaneLayouts(y + TIMELINE_RULER_HEIGHT);
        TimelineLaneLayout layout = timelineLaneAt(layouts, mouseY);
        if (layout == null) {
            return false;
        }
        TimelineLane lane = layout.lane();
        if (lane.track != null) {
            beat = snapBeat(rawBeat, lane.track);
        }
        if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_RIGHT && lane.track != null && lane.type == LaneType.EVENTS && lane.eventType != null) {
            if (splitEventAtBeat(lane.track, lane.eventType, beat)) {
                populateFieldsFromSelection();
            } else {
                state.setStatus("Right-click inside an event clip to split it");
            }
            return true;
        }
        if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_RIGHT && lane.track != null && lane.type == LaneType.NOTE_AXIS && lane.noteAxis != null) {
            NoteData note = state.addNote(lane.track, beat);
            activeNoteAxis = lane.noteAxis;
            setNoteAxisValue(note, lane.noteAxis, noteLaneScreenToValue(layout.top(), layout.height(), noteLaneRange(lane.track, lane.noteAxis), mouseY));
            if (note.noteType() == NoteType.HOLD) {
                note.pos().set(note.pos().x(), note.pos().y(), -1.0);
                note.setHoldLengthBeats(trackGridStep(lane.track));
            }
            state.markDirty();
            replaceSelectedNotes(List.of(new SelectedNote(lane.track, note)));
            populateFieldsFromSelection();
            state.setStatus("Added " + lane.noteAxis.label + " note on TrackID " + lane.track.id() + " at beat " + format(beat));
            return true;
        }
        if (lane.type == LaneType.BPM) {
            selectedNotes.clear();
            selectedEventClips.clear();
            BpmPoint bpm = findNearestBpm(contentX, contentWidth, mouseX);
            if (bpm != null) {
                state.setSelection(EditorSelection.bpm(bpm));
                dragMode = DragMode.BPM;
            } else {
                state.seekToBeat(beat);
            }
            return true;
        }
        if (lane.type == LaneType.EFFECTS) {
            selectedNotes.clear();
            selectedEventClips.clear();
            EffectData effect = findNearestEffect(contentX, contentWidth, mouseX);
            if (effect != null) {
                state.setSelection(EditorSelection.effect(effect));
                dragMode = DragMode.EFFECT;
            } else {
                state.seekToBeat(beat);
            }
            return true;
        }
        if (lane.track != null && lane.type == LaneType.TRACK_HEADER) {
            selectedNotes.clear();
            selectedEventClips.clear();
            if (mouseX < x + 24) {
                state.toggleTrackExpanded(lane.track);
            } else {
                state.setSelection(EditorSelection.track(lane.track));
            }
            return true;
        }
        if (lane.track != null && lane.type == LaneType.NOTE_AXIS && lane.noteAxis != null) {
            TrackData track = lane.track;
            SelectedNote holdLengthHandle = findHoldLengthHandleAt(track, lane.noteAxis, contentX, contentWidth, mouseX, layout.top(), layout.height(), mouseY);
            if (holdLengthHandle != null) {
                activeNoteAxis = lane.noteAxis;
                draggingHoldLengthNote = holdLengthHandle;
                dragAnchorBeat = beat;
                dragAnchorHoldLength = holdLengthHandle.note().holdLengthBeats();
                replaceSelectedNotes(List.of(holdLengthHandle));
                state.setSelection(EditorSelection.note(holdLengthHandle.track(), holdLengthHandle.note()));
                dragMode = DragMode.HOLD_LENGTH;
                state.setStatus("Drag hold tail, snapped to Track " + track.id() + " 1/" + track.beatDivision());
                return true;
            }
            NoteData note = findNearestNote(track, lane.noteAxis, contentX, contentWidth, mouseX, layout.top(), layout.height(), mouseY);
            if (note != null) {
                activeNoteAxis = lane.noteAxis;
                SelectedNote selectedNote = new SelectedNote(track, note);
                updateNoteSelectionFromClick(selectedNote, isControlDown());
                state.setSelection(EditorSelection.note(track, note));
                initializeNoteDrag(layout, beat, mouseY);
                dragMode = DragMode.NOTE;
            } else {
                activeNoteAxis = lane.noteAxis;
                state.setSelection(EditorSelection.track(track));
                if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT) {
                    selectionBox = new SelectionBox((int) mouseX, (int) mouseY, (int) mouseX, (int) mouseY);
                    selectionBoxAdditive = isControlDown();
                    dragMode = DragMode.BOX_SELECT;
                }
                state.seekToBeat(beat);
            }
            return true;
        }
        if (lane.track != null && lane.type == LaneType.EVENTS) {
            SelectedEventHandle handle = findNearestEventHandle(lane.track, lane.eventType, contentX, layout.top(), layout.height(), contentWidth, mouseX, mouseY);
            if (handle != null) {
                selectedEventHandle = handle;
                selectedEventClip = new SelectedEventClip(handle.track, handle.eventType, handle.eventIndex);
                updateEventClipSelectionFromClick(selectedEventClip, isControlDown());
                initializeEventClipDrag(beat);
                dragMode = DragMode.EVENT_HANDLE;
                state.setSelection(EditorSelection.track(lane.track));
                state.setStatus("Editing TrackID " + lane.track.id() + " " + lane.eventType.label + " " + handle.anchor.label);
                return true;
            }
            SelectedEventClip clip = findEventClipAt(lane.track, lane.eventType, contentX, layout.top(), layout.height(), contentWidth, mouseX, mouseY);
            if (clip != null) {
                selectedEventClip = clip;
                selectedEventHandle = null;
                updateEventClipSelectionFromClick(clip, isControlDown());
                initializeEventClipDrag(beat);
                state.setSelection(EditorSelection.track(lane.track));
                dragMode = DragMode.EVENT_CLIP;
                state.setStatus("Selected TrackID " + lane.track.id() + " " + lane.eventType.label + " clip");
                return true;
            }
            state.setSelection(EditorSelection.track(lane.track));
            selectedEventClip = null;
            selectedEventHandle = null;
            if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT) {
                selectionBox = new SelectionBox((int) mouseX, (int) mouseY, (int) mouseX, (int) mouseY);
                selectionBoxAdditive = isControlDown();
                dragMode = DragMode.BOX_SELECT;
            }
            state.setStatus("Selected TrackID " + lane.track.id() + " event lane: " + lane.eventType.label);
            return true;
        }
        return false;
    }

    private void handleTimelineDrag(double mouseX, double mouseY) {
        EditorLayout layout = editorLayout();
        int contentX = layout.centerX() + TIMELINE_LABEL_WIDTH;
        int contentWidth = Math.max(48, layout.centerWidth() - TIMELINE_LABEL_WIDTH);
        double beat = snapBeat(screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX)));
        List<TimelineLaneLayout> layouts = buildTimelineLaneLayouts(layout.timelineY() + TIMELINE_RULER_HEIGHT);
        int laneBottom = layout.timelineY() + layout.timelineHeight() - TIMELINE_AUDIO_STRIP_HEIGHT;
        TimelineLaneLayout hoveredLayout = mouseY >= laneBottom ? null : timelineLaneAt(layouts, mouseY);
        switch (dragMode) {
            case BPM -> state.moveSelectedBpm(beat);
            case EFFECT -> state.moveSelectedEffect(beat);
            case NOTE -> {
                TimelineLane lane = hoveredLayout == null ? null : hoveredLayout.lane();
                TrackData targetTrack = lane != null && lane.track != null ? lane.track : state.selection().track();
                if (hoveredLayout != null && lane != null && lane.type == LaneType.NOTE_AXIS && lane.noteAxis != null) {
                    activeNoteAxis = lane.noteAxis;
                    double snappedBeat = applyTimelineBeatSnap(screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX)), contentX, contentWidth, selectedNoteBeats(), selectedEventClipEdges(), lane.track);
                    double beatDelta = snappedBeat - dragAnchorBeat;
                    double range = noteLaneRange(targetTrack, lane.noteAxis);
                    double axisValue = noteLaneScreenToValue(hoveredLayout.top(), hoveredLayout.height(), range, mouseY);
                    double axisDelta = axisValue - dragAnchorAxisValue;
                    applyDraggedNotes(beatDelta, axisDelta, targetTrack, selectedNotes.size() == 1 ? targetTrack : null);
                }
            }
            case EVENT_HANDLE -> dragSelectedEventHandle(mouseX, mouseY, contentX, contentWidth, hoveredLayout);
            case EVENT_CLIP -> dragSelectedEventClip(mouseX, contentX, contentWidth);
            case HOLD_LENGTH -> dragHoldLength(mouseX, contentX, contentWidth);
            case BOX_SELECT -> selectionBox = selectionBox == null
                    ? new SelectionBox((int) mouseX, (int) mouseY, (int) mouseX, (int) mouseY)
                    : new SelectionBox(selectionBox.startX(), selectionBox.startY(), (int) mouseX, (int) mouseY);
            default -> {
            }
        }
        populateFieldsFromSelection();
    }

    private boolean handlePreviewClick(Click click, int x, int y, int width, int height) {
        if (client == null || client.world == null) {
            return false;
        }
        if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_MIDDLE) {
            pickWorldDisplay();
            return true;
        }
        if (RmcChartClient.getWorldLauncher().pickLookTarget()) {
            populateFieldsFromSelection();
            dragMode = DragMode.WORLD_SELECTION;
            return true;
        }
        if (state.selection().kind() == EditorSelection.Kind.NOTE || state.selection().kind() == EditorSelection.Kind.EFFECT) {
            dragMode = DragMode.WORLD_SELECTION;
            return true;
        }
        return false;
    }

    private BpmPoint findNearestBpm(int x, int width, double mouseX) {
        return state.level().meta().bpms().stream()
                .min(Comparator.comparingDouble(bpm -> Math.abs(beatToScreen(x, width, bpm.beat()) - mouseX)))
                .filter(bpm -> Math.abs(beatToScreen(x, width, bpm.beat()) - mouseX) <= 6.0)
                .orElse(null);
    }

    private EffectData findNearestEffect(int x, int width, double mouseX) {
        return state.level().effects().stream()
                .min(Comparator.comparingDouble(effect -> Math.abs(beatToScreen(x, width, effect.beat()) - mouseX)))
                .filter(effect -> Math.abs(beatToScreen(x, width, effect.beat()) - mouseX) <= 6.0)
                .orElse(null);
    }

    private NoteData findNearestNote(TrackData track, NoteAxis noteAxis, int x, int width, double mouseX, int rowTop, int laneHeight, double mouseY) {
        double range = noteLaneRange(track, noteAxis);
        NoteData best = null;
        double bestDistance = Double.MAX_VALUE;
        for (NoteData note : track.notes()) {
            int noteX = beatToScreen(x, width, note.beat());
            int noteY = noteLaneValueToScreen(rowTop, laneHeight, range, noteAxisValue(note, noteAxis));
            double distance = squaredDistance(mouseX, mouseY, noteX, noteY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = note;
            }
        }
        return bestDistance <= 256.0 ? best : null;
    }

    private SelectedNote findHoldLengthHandleAt(TrackData track, NoteAxis noteAxis, int x, int width, double mouseX, int rowTop, int laneHeight, double mouseY) {
        if (track == null || noteAxis != NoteAxis.Z) {
            return null;
        }
        double range = noteLaneRange(track, noteAxis);
        NoteData best = null;
        double bestDistance = Double.MAX_VALUE;
        for (NoteData note : track.notes()) {
            if (note.noteType() != NoteType.HOLD) {
                continue;
            }
            int handleX = beatToScreen(x, width, note.beat() + note.holdLengthBeats());
            int handleY = noteLaneValueToScreen(rowTop, laneHeight, range, -1.0);
            double distance = squaredDistance(mouseX, mouseY, handleX, handleY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = note;
            }
        }
        return best != null && bestDistance <= 144.0 ? new SelectedNote(track, best) : null;
    }

    private void newProject() {
        Path path = parsePath();
        if (path != null) {
            state.newProject(path);
            pathField.setText(path.toString());
            clearTimelineSelections();
            resetHistorySnapshots();
        }
    }

    private void loadProject() {
        Path path = parsePath();
        if (path != null) {
            state.loadProject(path);
            pathField.setText(path.toString());
            clearTimelineSelections();
            resetHistorySnapshots();
        }
    }

    private void saveProject() {
        Path path = parsePath();
        if (path != null) {
            state.saveProject(path);
            pathField.setText(path.toString());
        }
    }

    private void reloadAudio() {
        state.reloadAudio();
    }

    private void togglePlaybackFromToolbar() {
        if (client != null && RmcChartClient.getWorldLauncher().isEditorWorldActive(client)) {
            RmcChartClient.getWorldLauncher().toggleWorldPlayback();
        } else {
            state.togglePlayback();
        }
    }

    private void stopPlaybackFromToolbar() {
        if (client != null && RmcChartClient.getWorldLauncher().isEditorWorldActive(client)) {
            RmcChartClient.getWorldLauncher().stopWorldPlayback();
        } else {
            state.stopPlayback();
        }
    }

    private void toggleWorldAutoPlay() {
        if (client != null && RmcChartClient.getWorldLauncher().isEditorWorldActive(client)) {
            boolean next = !RmcChartClient.getWorldLauncher().playbackEngine().isAutoPlay();
            RmcChartClient.getWorldLauncher().setWorldPlaybackAutoPlay(next);
        }
    }

    private void syncWorldDisplays() {
        if (client != null && client.world != null) {
            RmcChartClient.getWorldLauncher().syncDisplays();
            state.setStatus("Synced in-world display entities");
        }
    }

    private void pickWorldDisplay() {
        if (client != null && client.world != null && RmcChartClient.getWorldLauncher().pickLookTarget()) {
            populateFieldsFromSelection();
        }
    }

    private void selectSong() {
        state.setSelection(EditorSelection.song());
    }

    private void selectMeta() {
        state.setSelection(EditorSelection.meta());
    }

    private Path parsePath() {
        try {
            return Path.of(pathField.getText().trim());
        } catch (InvalidPathException exception) {
            state.setStatus("Invalid path: " + exception.getMessage());
            return null;
        }
    }

    private Text propertyTitle() {
        return switch (state.selection().kind()) {
            case SONG -> Text.literal("Song Manifest Properties");
            case META -> Text.literal(state.activeDifficulty().displayName() + " Meta Properties");
            case TRACK -> Text.literal("Track Properties");
            case NOTE -> Text.literal("Note Properties");
            case EFFECT -> Text.literal("Effect Properties");
            case BPM -> Text.literal("BPM Point Properties");
        };
    }

    private String selectedObjectSummary() {
        return switch (state.selection().kind()) {
            case SONG -> "Current object: song manifest";
            case META -> "Current object: level meta for " + state.activeDifficulty().displayName();
            case TRACK -> state.selection().track() == null ? "Current object: track" : "Current object: TrackID " + state.selection().track().id();
            case NOTE -> state.selection().note() == null ? "Current object: note" : "Current object: " + state.selection().note().noteType() + " @ beat " + format(state.selection().note().beat());
            case EFFECT -> state.selection().effect() == null ? "Current object: effect" : "Current object: " + state.selection().effect().effectType().name() + " @ beat " + format(state.selection().effect().beat());
            case BPM -> state.selection().bpm() == null ? "Current object: BPM point" : "Current object: BPM " + format(state.selection().bpm().bpm()) + " @ beat " + format(state.selection().bpm().beat());
        };
    }

    private String displayLabel(LabeledField field) {
        if (!effectFields.contains(field) || state.selection().kind() != EditorSelection.Kind.EFFECT || state.selection().effect() == null) {
            return field.label;
        }
        int index = effectFields.indexOf(field);
        EffectType type = state.selection().effect().effectType();
        return switch (index) {
            case 0 -> "Beat";
            case 1 -> "Effect Type";
            case 2 -> switch (type) {
                case TEXT_DISPLAY, TEXT_DISPLAY_EFFECT, TEXT_DISPLAY_REMOVE, TEXT_DISPLAY_SYNC_TRACK, TEXT_DISPLAY_DESYNC_TRACK -> "Text Display ID";
                default -> "Effect ID";
            };
            case 3 -> switch (type) {
                case TITLE -> "Title Text";
                case MESSAGE -> "Message Text";
                case TIME -> "Time Value";
                case WEATHER -> "Weather Value";
                case FIREWORK -> "Firework Value";
                default -> "Text / Value";
            };
            case 4 -> type == EffectType.GLOW_COLOR ? "Glow Color" : "Color";
            case 5 -> switch (type) {
                case TEXT_DISPLAY_SYNC_TRACK, TEXT_DISPLAY_DESYNC_TRACK, HIDE_NOTES -> "Track ID";
                default -> "Track ID / Target Track";
            };
            case 6 -> "Position (x,y,z)";
            case 7 -> "Scale (x,y,z)";
            case 8 -> "Rotation (x,y,z)";
            case 9 -> switch (type) {
                case ARENA -> "Arena";
                case WEATHER -> "Weather Target";
                case TIME -> "Time Target";
                default -> "Arena / Target";
            };
            case 10 -> switch (type) {
                case TEXT_DISPLAY_SYNC_TRACK -> "Sync Mode";
                case TEXT_DISPLAY_DESYNC_TRACK -> "Desync Mode";
                case HIDE_NOTES -> "Hide State";
                case ARENA -> "Transition / Mode";
                case MESSAGE -> "Message Mode";
                default -> "Mode / State";
            };
            case 11 -> "Advanced Extra Fields";
            default -> field.label;
        };
    }

    private List<LabeledField> activeFieldGroup() {
        return switch (state.selection().kind()) {
            case SONG -> songFields;
            case META -> metaFields;
            case TRACK -> trackFields;
            case NOTE -> noteFields;
            case EFFECT -> activeEffectFields();
            case BPM -> bpmFields;
        };
    }

    private List<LabeledField> activeEffectFields() {
        EffectData effect = state.selection().effect();
        if (effect == null) {
            return effectFields;
        }
        return switch (effect.effectType()) {
            case TEXT_DISPLAY, TEXT_DISPLAY_EFFECT -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(2), effectFields.get(3), effectFields.get(4), effectFields.get(5), effectFields.get(6), effectFields.get(7), effectFields.get(8), effectFields.get(10));
            case TEXT_DISPLAY_REMOVE -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(2));
            case TEXT_DISPLAY_SYNC_TRACK, TEXT_DISPLAY_DESYNC_TRACK -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(2), effectFields.get(5), effectFields.get(10));
            case TITLE -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(4), effectFields.get(10));
            case MESSAGE -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(4), effectFields.get(10));
            case GLOW_COLOR -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(4), effectFields.get(10));
            case HIDE_NOTES -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(5), effectFields.get(10));
            case ARENA -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(9), effectFields.get(10));
            case WEATHER, TIME -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(9), effectFields.get(10));
            case FIREWORK, HOLOGRAM, REMOVE_HOLOGRAM, EFFECT, CLEAR_EFFECT -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(4), effectFields.get(6), effectFields.get(7), effectFields.get(8), effectFields.get(9), effectFields.get(10), effectFields.get(11));
        };
    }

    private List<LabeledField> allFields() {
        List<LabeledField> fields = new ArrayList<>();
        fields.addAll(songFields);
        fields.addAll(metaFields);
        fields.addAll(trackFields);
        fields.addAll(noteFields);
        fields.addAll(effectFields);
        fields.addAll(bpmFields);
        return fields;
    }

    private String shortEffectLabel(EffectType effectType) {
        return switch (effectType) {
            case TEXT_DISPLAY -> "TXT";
            case TEXT_DISPLAY_EFFECT -> "TXT+";
            case TEXT_DISPLAY_REMOVE -> "TXT-";
            case TEXT_DISPLAY_SYNC_TRACK -> "SYNC";
            default -> effectType.name().substring(0, Math.min(3, effectType.name().length()));
        };
    }

    private List<String> activeTextDisplayLines() {
        Map<String, String> displays = new LinkedHashMap<>();
        for (EffectData effect : state.triggeredEffects()) {
            JsonObject properties = effect.properties();
            String id = propertyString(properties, "id", propertyString(properties, "textId", "display"));
            if (effect.effectType() == EffectType.TEXT_DISPLAY) {
                displays.put(id, propertyString(properties, "text", propertyString(properties, "content", id)));
            } else if (effect.effectType() == EffectType.TEXT_DISPLAY_REMOVE) {
                displays.remove(id);
            }
        }
        List<String> lines = new ArrayList<>();
        if (displays.isEmpty()) {
            lines.add("No active text displays");
        } else {
            lines.add("Active text displays:");
            displays.forEach((id, text) -> lines.add(id + ": " + text));
        }
        return lines;
    }

    private String propertyString(JsonObject properties, String key, String fallback) {
        if (properties != null && properties.has(key)) {
            return properties.get(key).getAsString();
        }
        return fallback;
    }

    private String firstProperty(JsonObject properties, String... keys) {
        for (String key : keys) {
            if (properties != null && properties.has(key) && properties.get(key).isJsonPrimitive()) {
                return properties.get(key).getAsString();
            }
        }
        return "";
    }

    private double getDouble(JsonObject properties, String key, double fallback) {
        if (properties != null && properties.has(key) && properties.get(key).isJsonPrimitive()) {
            try {
                return properties.get(key).getAsDouble();
            } catch (RuntimeException ignored) {
            }
        }
        return fallback;
    }

    private JsonObject filterExtraEffectProperties(JsonObject properties) {
        JsonObject filtered = new JsonObject();
        if (properties == null) {
            return filtered;
        }
        for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
            if (!isKnownEffectProperty(entry.getKey())) {
                filtered.add(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private boolean isKnownEffectProperty(String key) {
        return switch (key) {
            case "id", "textId", "text", "content", "title", "message", "value", "color", "glowColor",
                    "trackId", "track", "x", "y", "z", "scaleX", "scaleY", "scaleZ",
                    "rotationX", "rotationY", "rotationZ", "arena", "target", "weather", "time",
                    "mode", "state", "type", "action" -> true;
            default -> false;
        };
    }

    private int countTrackEvents(TrackData track) {
        return track.speedEvents().size()
                + track.xTransformEvents().size() + track.yTransformEvents().size() + track.zTransformEvents().size()
                + track.xRotateEvents().size() + track.yRotateEvents().size() + track.zRotateEvents().size()
                + track.xScaleEvents().size() + track.yScaleEvents().size() + track.zScaleEvents().size();
    }

    private List<TimelineLane> buildTimelineLanes() {
        List<TimelineLane> lanes = new ArrayList<>();
        lanes.add(new TimelineLane(LaneType.BPM, null, null, null));
        lanes.add(new TimelineLane(LaneType.EFFECTS, null, null, null));
        for (TrackData track : state.visibleTracks()) {
            lanes.add(new TimelineLane(LaneType.TRACK_HEADER, track, null, null));
            if (!state.isTrackExpanded(track)) {
                continue;
            }
            for (NoteAxis axis : NoteAxis.values()) {
                lanes.add(new TimelineLane(LaneType.NOTE_AXIS, track, null, axis));
            }
            for (String group : List.of("Speed", "Position", "Rotation", "Scale")) {
                lanes.add(new TimelineLane(LaneType.EVENT_GROUP_HEADER, track, null, null, group));
                if (state.isTrackEventGroupExpanded(track, group)) {
                    for (EventLaneType eventType : eventTypesInGroup(group)) {
                        if (!eventsForLane(track, eventType).isEmpty()) {
                            lanes.add(new TimelineLane(LaneType.EVENTS, track, eventType, null));
                        }
                    }
                }
            }
        }
        return lanes;
    }

    private List<EventLaneType> eventTypesInGroup(String group) {
        return switch (group) {
            case "Speed" -> List.of(EventLaneType.SPEED);
            case "Position" -> List.of(EventLaneType.MOVE_X, EventLaneType.MOVE_Y, EventLaneType.MOVE_Z);
            case "Rotation" -> List.of(EventLaneType.ROT_X, EventLaneType.ROT_Y, EventLaneType.ROT_Z);
            case "Scale" -> List.of(EventLaneType.SCALE_X, EventLaneType.SCALE_Y, EventLaneType.SCALE_Z);
            default -> List.of();
        };
    }

    private List<TimelineLaneLayout> buildTimelineLaneLayouts(int timelineY) {
        List<TimelineLaneLayout> layouts = new ArrayList<>();
        int laneTop = timelineY - trackScrollY;
        for (TimelineLane lane : buildTimelineLanes()) {
            int laneHeight = laneHeight(lane);
            layouts.add(new TimelineLaneLayout(lane, laneTop, laneHeight));
            laneTop += laneHeight;
        }
        return layouts;
    }

    private int totalTimelineLaneHeight() {
        int total = 0;
        for (TimelineLane lane : buildTimelineLanes()) {
            total += laneHeight(lane);
        }
        return total;
    }

    private TimelineLaneLayout timelineLaneAt(List<TimelineLaneLayout> layouts, double mouseY) {
        for (TimelineLaneLayout layout : layouts) {
            if (mouseY >= layout.top() && mouseY < layout.top() + layout.height()) {
                return layout;
            }
        }
        return null;
    }

    private int laneHeight(TimelineLane lane) {
        return switch (lane.type) {
            case BPM, EFFECTS, EVENT_GROUP_HEADER -> BASE_ROW_HEIGHT;
            case TRACK_HEADER -> TRACK_HEADER_HEIGHT;
            case NOTE_AXIS -> NOTE_LANE_HEIGHT;
            case EVENTS -> EVENT_LANE_HEIGHT;
        };
    }

    private void drawTimelineLane(DrawContext context, TimelineLaneLayout layout, int x, int contentX, int contentWidth) {
        TimelineLane lane = layout.lane();
        switch (lane.type) {
            case BPM -> drawBpmLane(context, x, contentX, layout.top(), layout.height(), contentWidth);
            case EFFECTS -> drawEffectLane(context, x, contentX, layout.top(), layout.height(), contentWidth);
            case TRACK_HEADER -> drawTrackHeaderLane(context, lane.track, x, contentX, layout.top(), layout.height(), contentWidth);
            case NOTE_AXIS -> drawTrackNoteAxisLane(context, lane.track, lane.noteAxis, x, contentX, layout.top(), layout.height(), contentWidth);
            case EVENT_GROUP_HEADER -> drawEventGroupHeaderLane(context, lane.track, lane.eventGroup, x, contentX, layout.top(), layout.height(), contentWidth);
            case EVENTS -> drawTrackEventLane(context, lane.track, lane.eventType, x, contentX, layout.top(), layout.height(), contentWidth);
        }
    }

    private void drawEventGroupHeaderLane(DrawContext context, TrackData track, String group, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null || group == null) return;
        boolean expanded = state.isTrackEventGroupExpanded(track, group);
        int color = switch (group) {
            case "Speed" -> 0xFFE8B15B;
            case "Position" -> 0xFF7BC8FF;
            case "Rotation" -> 0xFFFF8F8F;
            case "Scale" -> 0xFFD2A8FF;
            default -> UI_MUTED;
        };
        context.drawText(textRenderer, Text.literal((expanded ? "- " : "+ ") + group), labelX + 8, rowTop + laneHeight / 2 - 4, color, false);
        context.fill(labelX, rowTop + laneHeight - 1, contentX + width, rowTop + laneHeight, 0x15354048);
    }

    private void drawBpmLane(DrawContext context, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        context.drawText(textRenderer, Text.literal("BPM"), labelX + 8, rowTop + laneHeight / 2 - 4, 0x90D0FF, false);
        if (state.estimatedBpmReference() > 0.0) {
            context.drawText(textRenderer, Text.literal("Ref " + format(state.estimatedBpmReference())), labelX + 42, rowTop + laneHeight / 2 - 4, 0x66CCFF, false);
        }
        context.enableScissor(contentX + 1, rowTop, contentX + width - 1, rowTop + laneHeight);
        for (BpmPoint bpm : state.level().meta().bpms()) {
            int markerX = beatToScreen(contentX, width, bpm.beat());
            int markerY = rowTop + laneHeight / 2;
            context.fill(markerX - 3, markerY - 3, markerX + 3, markerY + 3, bpm == state.selection().bpm() ? 0xFF88CCFF : 0xFF4477AA);
            context.drawText(textRenderer, Text.literal(format(bpm.bpm())), markerX + 4, markerY - 4, 0xFF88CCFF, false);
        }
        context.disableScissor();
    }

    private void drawEffectLane(DrawContext context, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        context.drawText(textRenderer, Text.literal("Effects"), labelX + 8, rowTop + laneHeight / 2 - 4, 0xFFD090, false);
        context.enableScissor(contentX + 1, rowTop, contentX + width - 1, rowTop + laneHeight);
        for (EffectData effect : state.level().effects()) {
            int markerX = beatToScreen(contentX, width, effect.beat());
            context.fill(markerX - 2, rowTop + 4, markerX + 2, rowTop + laneHeight - 4, effect == state.selection().effect() ? 0xFFFFD77A : 0xFFAA7722);
            context.drawText(textRenderer, Text.literal(shortEffectLabel(effect.effectType())), markerX + 4, rowTop + laneHeight / 2 - 4, 0xFFEBC48C, false);
        }
        context.disableScissor();
    }

    private void drawTrackHeaderLane(DrawContext context, TrackData track, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null) {
            return;
        }
        int accent = state.selectedTrack() == track ? 0xAA3A6EA5 : 0x66405060;
        context.fill(labelX, rowTop, contentX + width, rowTop + laneHeight - 1, accent);
        context.fill(labelX + 4, rowTop + 5, labelX + 18, rowTop + 19, 0x55000000);
        context.drawText(textRenderer, Text.literal(state.isTrackExpanded(track) ? "-" : "+"), labelX + 9, rowTop + 8, 0xFFFFFFFF, false);
        context.drawText(textRenderer, Text.literal("TrackID " + track.id()), labelX + 24, rowTop + 5, 0xFFFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Notes " + track.notes().size() + "  |  Grid 1/" + track.beatDivision() + "  |  Event Lanes " + countVisibleEventLanes(track)), contentX + 8, rowTop + 5, 0xFFD7E8F4, false);
    }

    private void drawTrackNoteAxisLane(DrawContext context, TrackData track, NoteAxis noteAxis, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null || noteAxis == null) {
            return;
        }
        double range = noteLaneRange(track, noteAxis);
        int centerY = rowTop + laneHeight / 2;
        boolean activeAxis = activeNoteAxis == noteAxis;
        context.drawText(textRenderer, Text.literal(noteAxis.label), labelX + 8, rowTop + 8, noteAxis.color, false);
        context.drawText(textRenderer, Text.literal("1/" + track.beatDivision() + " grid  |  range " + format(range) + (activeAxis ? "  active" : "")), labelX + 8, rowTop + 20, 0x81B8CF, false);
        NoteData selectedNote = state.selection().kind() == EditorSelection.Kind.NOTE && state.selection().track() == track ? state.selection().note() : null;
        if (selectedNote != null) {
            context.drawText(textRenderer, Text.literal("sel " + shortNoteLabel(selectedNote.noteType()) + " = " + format(noteAxisValue(selectedNote, noteAxis))), labelX + 8, rowTop + 32, 0xDCEBFF, false);
        }
        context.enableScissor(contentX + 1, rowTop, contentX + width - 1, rowTop + laneHeight);
        context.fill(contentX, centerY, contentX + width, centerY + 1, 0x60A5D6F6);
        drawTrackDivisionGrid(context, track, contentX, rowTop, width, laneHeight);
        for (int guide = 1; guide <= 2; guide++) {
            int guideTop = noteLaneValueToScreen(rowTop, laneHeight, range, range * guide / 2.0);
            int guideBottom = noteLaneValueToScreen(rowTop, laneHeight, range, -range * guide / 2.0);
            context.fill(contentX, guideTop, contentX + width, guideTop + 1, 0x183D5466);
            context.fill(contentX, guideBottom, contentX + width, guideBottom + 1, 0x183D5466);
        }
        for (NoteData note : track.notes()) {
            int noteX = beatToScreen(contentX, width, note.beat());
            double axisValue = noteAxisValue(note, noteAxis);
            int noteY = noteLaneValueToScreen(rowTop, laneHeight, range, axisValue);
            boolean selected = isNoteSelected(track, note);
            int color = selected ? 0xFFFFFFFF : noteColor(note.noteType());
            if (note.noteType() == NoteType.HOLD && noteAxis == NoteAxis.Z) {
                int holdEndX = beatToScreen(contentX, width, note.beat() + note.holdLengthBeats());
                context.fill(Math.min(noteX, holdEndX), noteY - 3, Math.max(noteX + 2, holdEndX), noteY + 3, selected ? 0xAAFFFFFF : 0xAA26A69A);
                context.fill(holdEndX - 2, noteY - 7, holdEndX + 2, noteY + 7, selected ? 0xFFFFFFFF : 0xFF26A69A);
            }
            int halfWidth = selected ? 8 : 6;
            int halfHeight = selected ? 5 : 4;
            context.fill(noteX - halfWidth, noteY - halfHeight, noteX + halfWidth, noteY + halfHeight, color);
            if (selected) {
                context.fill(noteX - 1, rowTop + 4, noteX + 1, rowTop + laneHeight - 4, 0x88FFFFFF);
            }
        }
        context.disableScissor();
    }

    private void drawTrackEventLane(DrawContext context, TrackData track, EventLaneType eventType, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null || eventType == null) {
            return;
        }
        context.drawText(textRenderer, Text.literal(eventType.label), labelX + 8, rowTop + laneHeight / 2 - 4, eventType.color, false);
        List<NumEventData> events = eventsForLane(track, eventType);
        if (!events.isEmpty()) {
            NumEventData event = events.getFirst();
            context.drawText(textRenderer, Text.literal(shortEasingLabel(event.easingType().name()) + " " + format(event.startValue()) + ">" + format(event.endValue())), labelX + 62, rowTop + laneHeight / 2 - 4, 0xBFCFDF, false);
        }
        context.enableScissor(contentX + 1, rowTop, contentX + width - 1, rowTop + laneHeight);
        drawTrackDivisionGrid(context, track, contentX, rowTop, width, laneHeight);
        double[] range = eventValueRange(events);
        for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
            NumEventData event = events.get(eventIndex);
            int startX = beatToScreen(contentX, width, event.startBeat());
            int endX = beatToScreen(contentX, width, event.endBeat());
            if (endX <= startX) {
                endX = startX + 2;
            }
            boolean clipSelected = isEventClipSelected(track, eventType, eventIndex);
            context.fill(startX, rowTop + 4, endX, rowTop + laneHeight - 4, eventType.fillColor);
            if (clipSelected) {
                context.fill(startX, rowTop + 4, endX, rowTop + 6, 0xB0FFFFFF);
                context.fill(startX, rowTop + laneHeight - 6, endX, rowTop + laneHeight - 4, 0xB0FFFFFF);
                context.fill(startX, rowTop + 4, startX + 2, rowTop + laneHeight - 4, 0xB0FFFFFF);
                context.fill(endX - 2, rowTop + 4, endX, rowTop + laneHeight - 4, 0xB0FFFFFF);
            }
            drawEventCurve(context, contentX, width, rowTop, laneHeight, event, eventType, range[0], range[1]);
            SelectedEventHandle currentHandle = selectedEventHandle;
            boolean selected = currentHandle != null && currentHandle.track == track && currentHandle.eventType == eventType && currentHandle.eventIndex == eventIndex;
            int startY = valueToLaneY(rowTop, laneHeight, event.startValue(), range[0], range[1]);
            int endY = valueToLaneY(rowTop, laneHeight, event.endValue(), range[0], range[1]);
            int handleColor = selected ? 0xFFFFFFFF : eventType.color;
            int edgeHandleColor = clipSelected ? 0xFFFFFFFF : 0xCC000000;
            context.fill(startX - 2, rowTop + 4, startX + 2, rowTop + laneHeight - 4, edgeHandleColor);
            context.fill(endX - 2, rowTop + 4, endX + 2, rowTop + laneHeight - 4, edgeHandleColor);
            context.fill(startX - 2, startY - 2, startX + 2, startY + 2, handleColor);
            context.fill(endX - 2, endY - 2, endX + 2, endY + 2, handleColor);
        }
        context.disableScissor();
    }

    private void drawTrackDivisionGrid(DrawContext context, TrackData track, int contentX, int rowTop, int width, int laneHeight) {
        double step = trackGridStep(track);
        double startBeat = Math.floor(state.visibleStartBeat() / step) * step;
        double endBeat = state.visibleEndBeat() + step;
        int previousX = Integer.MIN_VALUE;
        for (double beat = startBeat; beat <= endBeat; beat += step) {
            int lineX = beatToScreen(contentX, width, beat);
            if (lineX - previousX < 5) {
                continue;
            }
            previousX = lineX;
            boolean whole = isWholeBeat(beat);
            int color = whole ? 0x335FBCD3 : 0x1E5FBCD3;
            context.fill(lineX, rowTop + 4, lineX + 1, rowTop + laneHeight - 4, color);
        }
    }

    private void drawEventCurve(DrawContext context, int x, int width, int rowTop, int laneHeight, NumEventData event, EventLaneType eventType, double minValue, double maxValue) {
        int previousX = beatToScreen(x, width, event.startBeat());
        int previousY = valueToLaneY(rowTop, laneHeight, event.startValue(), minValue, maxValue);
        int samples = Math.max(8, Math.min(48, beatToScreen(x, width, event.endBeat()) - previousX));
        for (int step = 1; step <= samples; step++) {
            double progress = step / (double) samples;
            double beat = event.startBeat() + (event.endBeat() - event.startBeat()) * progress;
            double value = cn.frkovo.rhythmcv2.rmcChart.chart.core.EasingFunctions.getEase(event.startValue(), event.endValue(), progress, event.easingType());
            int nextX = beatToScreen(x, width, beat);
            int nextY = valueToLaneY(rowTop, laneHeight, value, minValue, maxValue);
            int fromX = Math.min(previousX, nextX);
            int toX = Math.max(previousX, nextX) + 1;
            int fromY = Math.min(previousY, nextY);
            int toY = Math.max(previousY, nextY) + 1;
            context.fill(fromX, fromY, toX, toY, eventType.color);
            previousX = nextX;
            previousY = nextY;
        }
    }

    private double[] eventValueRange(List<NumEventData> events) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (NumEventData event : events) {
            min = Math.min(min, Math.min(event.startValue(), event.endValue()));
            max = Math.max(max, Math.max(event.startValue(), event.endValue()));
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            return new double[]{-1.0, 1.0};
        }
        if (Math.abs(max - min) < 1.0E-6) {
            min -= 1.0;
            max += 1.0;
        }
        double padding = (max - min) * 0.15;
        return new double[]{min - padding, max + padding};
    }

    private int valueToLaneY(int rowTop, int laneHeight, double value, double minValue, double maxValue) {
        double progress = (value - minValue) / (maxValue - minValue);
        progress = Math.max(0.0, Math.min(1.0, progress));
        return rowTop + laneHeight - 5 - (int) Math.round(progress * (laneHeight - 10));
    }

    private SelectedEventHandle findNearestEventHandle(TrackData track, EventLaneType eventType, int x, int rowTop, int laneHeight, int width, double mouseX, double mouseY) {
        List<NumEventData> events = eventsForLane(track, eventType);
        double[] range = eventValueRange(events);
        SelectedEventHandle best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int index = 0; index < events.size(); index++) {
            NumEventData event = events.get(index);
            int startX = beatToScreen(x, width, event.startBeat());
            int endX = beatToScreen(x, width, event.endBeat());
            int startY = valueToLaneY(rowTop, laneHeight, event.startValue(), range[0], range[1]);
            int endY = valueToLaneY(rowTop, laneHeight, event.endValue(), range[0], range[1]);
            if (mouseY >= rowTop + 4 && mouseY <= rowTop + laneHeight - 4) {
                double startEdgeDistance = Math.abs(mouseX - startX);
                if (startEdgeDistance < bestDistance) {
                    bestDistance = startEdgeDistance;
                    best = new SelectedEventHandle(track, eventType, index, EventAnchor.START);
                }
                double endEdgeDistance = Math.abs(mouseX - endX);
                if (endEdgeDistance < bestDistance) {
                    bestDistance = endEdgeDistance;
                    best = new SelectedEventHandle(track, eventType, index, EventAnchor.END);
                }
            }
            double startDistance = squaredDistance(mouseX, mouseY, startX, startY);
            if (startDistance < bestDistance) {
                bestDistance = startDistance;
                best = new SelectedEventHandle(track, eventType, index, EventAnchor.START);
            }
            double endDistance = squaredDistance(mouseX, mouseY, endX, endY);
            if (endDistance < bestDistance) {
                bestDistance = endDistance;
                best = new SelectedEventHandle(track, eventType, index, EventAnchor.END);
            }
        }
        return bestDistance <= 8.0 || bestDistance <= 144.0 ? best : null;
    }

    private SelectedEventClip findEventClipAt(TrackData track, EventLaneType eventType, int x, int rowTop, int laneHeight, int width, double mouseX, double mouseY) {
        List<NumEventData> events = eventsForLane(track, eventType);
        for (int index = 0; index < events.size(); index++) {
            NumEventData event = events.get(index);
            int startX = beatToScreen(x, width, event.startBeat());
            int endX = Math.max(startX + 2, beatToScreen(x, width, event.endBeat()));
            if (mouseX >= startX && mouseX <= endX && mouseY >= rowTop + 4 && mouseY <= rowTop + laneHeight - 4) {
                return new SelectedEventClip(track, eventType, index);
            }
        }
        return null;
    }

    private boolean isEventClipSelected(TrackData track, EventLaneType eventType, int eventIndex) {
        if (selectedEventHandle != null
                && selectedEventHandle.track == track
                && selectedEventHandle.eventType == eventType
                && selectedEventHandle.eventIndex == eventIndex) {
            return true;
        }
        return selectedEventClip != null
                && selectedEventClip.track == track
                && selectedEventClip.eventType == eventType
                && selectedEventClip.eventIndex == eventIndex;
    }

    private void dragSelectedEventHandle(double mouseX, double mouseY, int timelineX, int timelineWidth, TimelineLaneLayout hoveredLayout) {
        if (selectedEventHandle == null || hoveredLayout == null) {
            return;
        }
        TimelineLane lane = hoveredLayout.lane();
        if (lane.track != selectedEventHandle.track || lane.eventType != selectedEventHandle.eventType) {
            return;
        }
        List<NumEventData> events = eventsForLane(selectedEventHandle.track, selectedEventHandle.eventType);
        if (selectedEventHandle.eventIndex < 0 || selectedEventHandle.eventIndex >= events.size()) {
            return;
        }
        NumEventData event = events.get(selectedEventHandle.eventIndex);
        double[] range = eventValueRange(events);
        double beat = applyTimelineBeatSnap(screenToBeat(timelineX, timelineWidth, mouseX), timelineX, timelineWidth, selectedNoteBeats(), selectedEventClipEdges(), selectedEventHandle.track);
        double progress = 1.0 - ((mouseY - (hoveredLayout.top() + 5.0)) / (hoveredLayout.height() - 10.0));
        progress = Math.max(0.0, Math.min(1.0, progress));
        double value = range[0] + (range[1] - range[0]) * progress;
        if (selectedEventHandle.anchor == EventAnchor.START) {
            event.setStartBeat(Math.min(beat, event.endBeat() - 0.01));
            event.setStartValue(value);
        } else {
            event.setEndBeat(Math.max(beat, event.startBeat() + 0.01));
            event.setEndValue(value);
        }
        state.sortCurrentLevel();
        int updatedIndex = eventsForLane(selectedEventHandle.track, selectedEventHandle.eventType).indexOf(event);
        if (updatedIndex >= 0) {
            selectedEventHandle = new SelectedEventHandle(selectedEventHandle.track, selectedEventHandle.eventType, updatedIndex, selectedEventHandle.anchor);
            selectedEventClip = new SelectedEventClip(selectedEventHandle.track, selectedEventHandle.eventType, updatedIndex);
        }
        state.markDirty();
        state.setStatus("Edited " + selectedEventHandle.eventType.label + " " + selectedEventHandle.anchor.label + " handle");
    }

    private boolean splitEventAtBeat(TrackData track, EventLaneType eventType, double beat) {
        List<NumEventData> events = eventsForLane(track, eventType);
        for (int index = 0; index < events.size(); index++) {
            NumEventData event = events.get(index);
            if (beat <= event.startBeat() + 0.01 || beat >= event.endBeat() - 0.01) {
                continue;
            }
            double progress = (beat - event.startBeat()) / Math.max(0.01, event.endBeat() - event.startBeat());
            double splitValue = cn.frkovo.rhythmcv2.rmcChart.chart.core.EasingFunctions.getEase(event.startValue(), event.endValue(), progress, event.easingType());
            NumEventData tail = new NumEventData(beat, event.endBeat(), splitValue, event.endValue(), event.easingType());
            event.setEndBeat(beat);
            event.setEndValue(splitValue);
            events.add(index + 1, tail);
            selectedEventHandle = new SelectedEventHandle(track, eventType, index + 1, EventAnchor.START);
            selectedEventClip = new SelectedEventClip(track, eventType, index + 1);
            state.setSelection(EditorSelection.track(track));
            state.sortCurrentLevel();
            state.markDirty();
            state.setStatus("Split TrackID " + track.id() + " " + eventType.label + " at beat " + format(beat));
            return true;
        }
        return false;
    }

    private double noteLaneRange(TrackData track, NoteAxis noteAxis) {
        double maxAbs = NOTE_LANE_DEFAULT_RANGE;
        if (track != null && noteAxis != null) {
            for (NoteData note : track.notes()) {
                maxAbs = Math.max(maxAbs, Math.abs(noteAxisValue(note, noteAxis)) + 0.5);
            }
        }
        return maxAbs;
    }

    private int noteLaneValueToScreen(int rowTop, int laneHeight, double range, double axisValue) {
        double progress = (axisValue + range) / Math.max(0.0001, range * 2.0);
        progress = Math.max(0.0, Math.min(1.0, progress));
        return rowTop + laneHeight - 6 - (int) Math.round(progress * (laneHeight - 12));
    }

    private double noteLaneScreenToValue(int rowTop, int laneHeight, double range, double mouseY) {
        double progress = 1.0 - ((mouseY - (rowTop + 6.0)) / Math.max(1.0, laneHeight - 12.0));
        progress = Math.max(0.0, Math.min(1.0, progress));
        return (progress * 2.0 - 1.0) * range;
    }

    private double noteAxisValue(NoteData note, NoteAxis noteAxis) {
        return switch (noteAxis) {
            case X -> note.pos().x();
            case Y -> note.pos().y();
            case Z -> note.pos().z();
        };
    }

    private void setNoteAxisValue(NoteData note, NoteAxis noteAxis, double value) {
        switch (noteAxis) {
            case X -> note.pos().set(value, note.pos().y(), note.pos().z());
            case Y -> note.pos().set(note.pos().x(), value, note.pos().z());
            case Z -> note.pos().set(note.pos().x(), note.pos().y(), note.noteType() == NoteType.HOLD ? -1.0 : value);
        }
    }

    private void updateNoteSelectionFromClick(SelectedNote selectedNote, boolean additive) {
        if (!additive) {
            selectedEventClips.clear();
            replaceSelectedNotes(List.of(selectedNote));
            return;
        }
        if (!selectedNotes.add(selectedNote)) {
            selectedNotes.remove(selectedNote);
        }
        if (selectedNotes.isEmpty()) {
            selectedNotes.add(selectedNote);
        }
    }

    private void updateEventClipSelectionFromClick(SelectedEventClip clip, boolean additive) {
        if (!additive) {
            selectedNotes.clear();
            replaceSelectedEventClips(List.of(clip));
            return;
        }
        if (!selectedEventClips.add(clip)) {
            selectedEventClips.remove(clip);
        }
        if (selectedEventClips.isEmpty()) {
            selectedEventClips.add(clip);
        }
    }

    private void replaceSelectedNotes(List<SelectedNote> notes) {
        selectedNotes.clear();
        selectedNotes.addAll(notes);
    }

    private void replaceSelectedEventClips(List<SelectedEventClip> clips) {
        selectedEventClips.clear();
        selectedEventClips.addAll(clips);
    }

    private boolean isNoteSelected(TrackData track, NoteData note) {
        return note == state.selection().note() || selectedNotes.contains(new SelectedNote(track, note));
    }

    private void initializeNoteDrag(TimelineLaneLayout layout, double pointerBeat, double pointerY) {
        noteDragSnapshots.clear();
        if (selectedNotes.isEmpty() && state.selection().track() != null && state.selection().note() != null) {
            selectedNotes.add(new SelectedNote(state.selection().track(), state.selection().note()));
        }
        dragAnchorBeat = pointerBeat;
        dragAnchorAxisValue = layout.lane().noteAxis == null
                ? 0.0
                : noteLaneScreenToValue(layout.top(), layout.height(), noteLaneRange(layout.lane().track(), layout.lane().noteAxis()), pointerY);
        for (SelectedNote selectedNote : selectedNotes) {
            noteDragSnapshots.put(selectedNote, new NoteDragSnapshot(selectedNote.track(), selectedNote.note().beat(), noteAxisValue(selectedNote.note(), activeNoteAxis)));
        }
    }

    private void initializeEventClipDrag(double pointerBeat) {
        eventClipDragSnapshots.clear();
        dragAnchorBeat = pointerBeat;
        for (SelectedEventClip clip : selectedEventClips) {
            NumEventData event = eventForClip(clip);
            if (event != null) {
                eventClipDragSnapshots.put(clip, new EventClipDragSnapshot(event, event.startBeat(), event.endBeat()));
            }
        }
    }

    private void applyDraggedNotes(double beatDelta, double axisDelta, TrackData hoveredTrack, TrackData singleMoveTarget) {
        if (noteDragSnapshots.isEmpty()) {
            return;
        }
        LinkedHashSet<SelectedNote> updatedSelection = new LinkedHashSet<>();
        SelectedNote primary = null;
        for (Map.Entry<SelectedNote, NoteDragSnapshot> entry : noteDragSnapshots.entrySet()) {
            SelectedNote selectedNote = entry.getKey();
            NoteDragSnapshot snapshot = entry.getValue();
            NoteData note = selectedNote.note();
            TrackData targetTrack = noteDragSnapshots.size() == 1 && singleMoveTarget != null ? singleMoveTarget : snapshot.track();
            note.setBeat(snapshot.beat() + beatDelta);
            setNoteAxisValue(note, activeNoteAxis, snapshot.axisValue() + axisDelta);
            if (snapshot.track() != targetTrack) {
                snapshot.track().notes().remove(note);
                targetTrack.notes().add(note);
            }
            SelectedNote updated = new SelectedNote(targetTrack, note);
            updatedSelection.add(updated);
            if (primary == null || note == state.selection().note()) {
                primary = updated;
            }
        }
        replaceSelectedNotes(new ArrayList<>(updatedSelection));
        if (primary != null) {
            state.setSelection(EditorSelection.note(primary.track(), primary.note()));
        }
        state.sortCurrentLevel();
        state.markDirty();
        snapGuideBeat = Double.isFinite(dragAnchorBeat + beatDelta) ? dragAnchorBeat + beatDelta : Double.NaN;
    }

    private void dragHoldLength(double mouseX, int timelineX, int timelineWidth) {
        if (draggingHoldLengthNote == null || draggingHoldLengthNote.note() == null) {
            return;
        }
        NoteData note = draggingHoldLengthNote.note();
        TrackData track = draggingHoldLengthNote.track();
        double rawTailBeat = Math.max(note.beat() + trackGridStep(track), screenToBeat(timelineX, timelineWidth, mouseX));
        double snappedTailBeat = snapBeat(rawTailBeat, track);
        double length = snapHoldLength(snappedTailBeat - note.beat(), track);
        note.setHoldLengthBeats(length);
        note.pos().set(note.pos().x(), note.pos().y(), -1.0);
        state.markDirty();
        snapGuideBeat = note.beat() + length;
        state.setStatus("Hold length " + format(length) + " beats (" + subdivisionLabel(note.beat() + length, track) + ")");
    }

    private void dragSelectedEventClip(double mouseX, int timelineX, int timelineWidth) {
        if (eventClipDragSnapshots.isEmpty()) {
            return;
        }
        double snappedBeat = applyTimelineBeatSnap(screenToBeat(timelineX, timelineWidth, mouseX), timelineX, timelineWidth, selectedNoteBeats(), selectedEventClipEdges(), state.selectedTrack());
        double beatDelta = snappedBeat - dragAnchorBeat;
        double minStart = Double.POSITIVE_INFINITY;
        for (EventClipDragSnapshot snapshot : eventClipDragSnapshots.values()) {
            minStart = Math.min(minStart, snapshot.startBeat() + beatDelta);
        }
        if (minStart < -64.0) {
            beatDelta += -64.0 - minStart;
        }
        LinkedHashSet<SelectedEventClip> updatedClips = new LinkedHashSet<>();
        SelectedEventClip primary = null;
        for (Map.Entry<SelectedEventClip, EventClipDragSnapshot> entry : eventClipDragSnapshots.entrySet()) {
            EventClipDragSnapshot snapshot = entry.getValue();
            snapshot.event().setStartBeat(snapshot.startBeat() + beatDelta);
            snapshot.event().setEndBeat(snapshot.endBeat() + beatDelta);
        }
        for (Map.Entry<SelectedEventClip, EventClipDragSnapshot> entry : eventClipDragSnapshots.entrySet()) {
            SelectedEventClip clip = entry.getKey();
            EventClipDragSnapshot snapshot = entry.getValue();
            List<NumEventData> events = eventsForLane(clip.track(), clip.eventType());
            int updatedIndex = events.indexOf(snapshot.event());
            if (updatedIndex >= 0) {
                SelectedEventClip updated = new SelectedEventClip(clip.track(), clip.eventType(), updatedIndex);
                updatedClips.add(updated);
                if (primary == null || clip.equals(selectedEventClip)) {
                    primary = updated;
                }
            }
        }
        replaceSelectedEventClips(new ArrayList<>(updatedClips));
        if (primary != null) {
            selectedEventClip = primary;
        }
        state.sortCurrentLevel();
        state.markDirty();
        state.setStatus("Moved " + updatedClips.size() + " event clip(s)");
    }

    private double applyTimelineBeatSnap(double rawBeat, int timelineX, int timelineWidth, Set<Double> excludedNoteBeats, Set<Double> excludedClipBeats, TrackData snapTrack) {
        if (isAltDown()) {
            snapGuideBeat = Double.NaN;
            return rawBeat;
        }
        double bestBeat = snapBeat(rawBeat, snapTrack);
        double bestDistance = Math.abs(beatToScreen(timelineX, timelineWidth, bestBeat) - beatToScreen(timelineX, timelineWidth, rawBeat));
        for (double candidate : collectSnapCandidateBeats()) {
            if (isExcludedSnapBeat(candidate, excludedNoteBeats, excludedClipBeats)) {
                continue;
            }
            double distance = Math.abs(beatToScreen(timelineX, timelineWidth, candidate) - beatToScreen(timelineX, timelineWidth, rawBeat));
            if (distance <= 10.0 && distance < bestDistance) {
                bestDistance = distance;
                bestBeat = candidate;
            }
        }
        snapGuideBeat = bestBeat;
        return bestBeat;
    }

    private boolean isExcludedSnapBeat(double candidate, Set<Double> excludedNoteBeats, Set<Double> excludedClipBeats) {
        for (double beat : excludedNoteBeats) {
            if (Math.abs(beat - candidate) < 1.0E-6) {
                return true;
            }
        }
        for (double beat : excludedClipBeats) {
            if (Math.abs(beat - candidate) < 1.0E-6) {
                return true;
            }
        }
        return false;
    }

    private List<Double> collectSnapCandidateBeats() {
        List<Double> candidates = new ArrayList<>();
        for (TrackData track : state.tracks()) {
            for (NoteData note : track.notes()) {
                candidates.add(note.beat());
            }
            for (EventLaneType eventType : EventLaneType.values()) {
                for (NumEventData event : eventsForLane(track, eventType)) {
                    candidates.add(event.startBeat());
                    candidates.add(event.endBeat());
                }
            }
        }
        for (BpmPoint bpm : state.level().meta().bpms()) {
            candidates.add(bpm.beat());
        }
        for (EffectData effect : state.level().effects()) {
            candidates.add(effect.beat());
        }
        return candidates;
    }

    private Set<Double> selectedNoteBeats() {
        LinkedHashSet<Double> beats = new LinkedHashSet<>();
        for (SelectedNote selectedNote : selectedNotes) {
            beats.add(selectedNote.note().beat());
        }
        return beats;
    }

    private Set<Double> selectedEventClipEdges() {
        LinkedHashSet<Double> beats = new LinkedHashSet<>();
        for (SelectedEventClip clip : selectedEventClips) {
            NumEventData event = eventForClip(clip);
            if (event != null) {
                beats.add(event.startBeat());
                beats.add(event.endBeat());
            }
        }
        return beats;
    }

    private NumEventData eventForClip(SelectedEventClip clip) {
        if (clip == null) {
            return null;
        }
        List<NumEventData> events = eventsForLane(clip.track(), clip.eventType());
        if (clip.eventIndex() < 0 || clip.eventIndex() >= events.size()) {
            return null;
        }
        return events.get(clip.eventIndex());
    }

    private void applySelectionBox() {
        if (selectionBox == null) {
            return;
        }
        EditorLayout editorLayout = editorLayout();
        int timelineY = editorLayout.timelineY();
        int contentX = editorLayout.centerX() + TIMELINE_LABEL_WIDTH;
        int contentWidth = Math.max(48, editorLayout.centerWidth() - TIMELINE_LABEL_WIDTH);
        int contentBottom = timelineY + editorLayout.timelineHeight() - TIMELINE_AUDIO_STRIP_HEIGHT;
        List<TimelineLaneLayout> layouts = buildTimelineLaneLayouts(timelineY + TIMELINE_RULER_HEIGHT);
        int left = Math.min(selectionBox.startX(), selectionBox.endX());
        int right = Math.max(selectionBox.startX(), selectionBox.endX());
        int top = Math.max(timelineY + TIMELINE_RULER_HEIGHT, Math.min(selectionBox.startY(), selectionBox.endY()));
        int bottom = Math.min(contentBottom, Math.max(selectionBox.startY(), selectionBox.endY()));
        LinkedHashSet<SelectedNote> hitNotes = selectionBoxAdditive ? new LinkedHashSet<>(selectedNotes) : new LinkedHashSet<>();
        LinkedHashSet<SelectedEventClip> hitClips = selectionBoxAdditive ? new LinkedHashSet<>(selectedEventClips) : new LinkedHashSet<>();
        NoteAxis firstAxis = activeNoteAxis;
        SelectedNote firstNote = null;
        SelectedEventClip firstClip = null;
        for (TimelineLaneLayout layout : layouts) {
            TimelineLane lane = layout.lane();
            if (lane.type == LaneType.NOTE_AXIS && lane.track() != null && lane.noteAxis() != null) {
                double range = noteLaneRange(lane.track(), lane.noteAxis());
                for (NoteData note : lane.track().notes()) {
                    int noteX = beatToScreen(contentX, contentWidth, note.beat());
                    int noteY = noteLaneValueToScreen(layout.top(), layout.height(), range, noteAxisValue(note, lane.noteAxis()));
                    if (noteX >= left && noteX <= right && noteY >= top && noteY <= bottom) {
                        SelectedNote selectedNote = new SelectedNote(lane.track(), note);
                        hitNotes.add(selectedNote);
                        if (firstNote == null) {
                            firstNote = selectedNote;
                            firstAxis = lane.noteAxis();
                        }
                    }
                }
            } else if (lane.type == LaneType.EVENTS && lane.track() != null && lane.eventType() != null) {
                List<NumEventData> events = eventsForLane(lane.track(), lane.eventType());
                for (int index = 0; index < events.size(); index++) {
                    NumEventData event = events.get(index);
                    int startX = beatToScreen(contentX, contentWidth, event.startBeat());
                    int endX = Math.max(startX + 2, beatToScreen(contentX, contentWidth, event.endBeat()));
                    if (rectanglesIntersect(left, top, right, bottom, startX, layout.top() + 4, endX, layout.top() + layout.height() - 4)) {
                        SelectedEventClip clip = new SelectedEventClip(lane.track(), lane.eventType(), index);
                        hitClips.add(clip);
                        if (firstClip == null) {
                            firstClip = clip;
                        }
                    }
                }
            }
        }
        replaceSelectedNotes(new ArrayList<>(hitNotes));
        replaceSelectedEventClips(new ArrayList<>(hitClips));
        if (firstNote != null) {
            activeNoteAxis = firstAxis;
            state.setSelection(EditorSelection.note(firstNote.track(), firstNote.note()));
            state.setStatus("Selected " + hitNotes.size() + " note(s)");
        } else if (firstClip != null) {
            selectedEventClip = firstClip;
            state.setSelection(EditorSelection.track(firstClip.track()));
            state.setStatus("Selected " + hitClips.size() + " event clip(s)");
        } else if (!selectionBoxAdditive) {
            clearTimelineSelections();
        }
    }

    private boolean rectanglesIntersect(int leftA, int topA, int rightA, int bottomA, int leftB, int topB, int rightB, int bottomB) {
        return rightA >= leftB && rightB >= leftA && bottomA >= topB && bottomB >= topA;
    }

    private double squaredDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private int countVisibleEventLanes(TrackData track) {
        int count = 0;
        for (EventLaneType eventType : EventLaneType.values()) {
            if (!eventsForLane(track, eventType).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private List<NumEventData> eventsForLane(TrackData track, EventLaneType eventType) {
        return switch (eventType) {
            case SPEED -> track.speedEvents();
            case MOVE_X -> track.xTransformEvents();
            case MOVE_Y -> track.yTransformEvents();
            case MOVE_Z -> track.zTransformEvents();
            case ROT_X -> track.xRotateEvents();
            case ROT_Y -> track.yRotateEvents();
            case ROT_Z -> track.zRotateEvents();
            case SCALE_X -> track.xScaleEvents();
            case SCALE_Y -> track.yScaleEvents();
            case SCALE_Z -> track.zScaleEvents();
        };
    }

    private String shortNoteLabel(NoteType noteType) {
        return switch (noteType) {
            case TAP -> "TAP";
            case LOOK -> "LOOK";
            case HOLD -> "HOLD";
            case DODGE -> "DODGE";
        };
    }

    private NoteType nextNoteType(NoteType noteType) {
        NoteType[] values = NoteType.values();
        int index = noteType == null ? 0 : noteType.ordinal() + 1;
        return values[index % values.length];
    }

    private String shortEasingLabel(String easingName) {
        return easingName.length() <= 8 ? easingName : easingName.substring(0, 8);
    }

    private String audioSummary() {
        if (state.audioPath() == null) {
            return state.audioStatus();
        }
        return state.audioPath().getFileName() + " (" + formatMillis(state.audioLengthMillis()) + ")";
    }

    private String stateAudioMillis() {
        return formatMillis(Math.max(0L, state.timing().beatToMillis(state.playheadBeat())));
    }

    private int beatToScreen(int x, int width, double beat) {
        return x + (int) Math.round((beat - state.visibleStartBeat()) / state.beatsPerScreen() * width);
    }

    private double screenToBeat(int x, int width, double screenX) {
        double progress = (screenX - x) / width;
        return state.visibleStartBeat() + progress * state.beatsPerScreen();
    }

    private double snapBeat(double beat) {
        return snapBeat(beat, null);
    }

    private double snapBeat(double beat, TrackData track) {
        if (isAltDown()) {
            return beat;
        }
        int division = track == null ? 16 : track.beatDivision();
        double grid = Math.max(1.0 / 64.0, 4.0 / Math.max(1, division));
        return Math.round(beat / grid) * grid;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private int noteColor(NoteType noteType) {
        return switch (noteType) {
            case TAP -> 0xFF4FC3F7;
            case LOOK -> 0xFFFFF176;
            case HOLD -> 0xFF26A69A;
            case DODGE -> 0xFFEF5350;
        };
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private void set(List<LabeledField> fields, int index, String value) {
        fields.get(index).widget.setText(value);
    }

    private String get(List<LabeledField> fields, int index) {
        return fields.get(index).widget.getText();
    }

    private String joinCsv(List<String> values) {
        return String.join(",", values);
    }

    private List<String> splitCsv(String text) {
        List<String> values = new ArrayList<>();
        if (text.isBlank()) {
            return values;
        }
        for (String token : text.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private String serializeVec(Vec3Data vec) {
        return format(vec.x()) + "," + format(vec.y()) + "," + format(vec.z());
    }

    private Vec3Data parseVec(String value) {
        String[] parts = value.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expected x,y,z");
        }
        return new Vec3Data(parseDouble(parts[0]), parseDouble(parts[1]), parseDouble(parts[2]));
    }

    private Vec3Data parseOptionalVec(String value, Vec3Data fallback) {
        if (value == null || value.isBlank()) {
            return fallback.copy();
        }
        return parseVec(value);
    }

    private String serializeTriple(double x, double y, double z) {
        return format(x) + "," + format(y) + "," + format(z);
    }

    private String serializeNumEvents(List<NumEventData> events) {
        JsonArray array = new JsonArray();
        for (NumEventData event : events) {
            JsonObject object = new JsonObject();
            object.addProperty("startBeat", event.startBeat());
            object.addProperty("endBeat", event.endBeat());
            object.addProperty("startValue", event.startValue());
            object.addProperty("endValue", event.endValue());
            object.addProperty("easingType", event.easingType().id());
            array.add(object);
        }
        return GSON.toJson(array);
    }

    private String serializeEventList(List<NumEventData> events) {
        List<String> rows = new ArrayList<>();
        for (NumEventData event : events) {
            rows.add(format(event.startBeat()) + "," + format(event.endBeat()) + "," + format(event.startValue()) + "," + format(event.endValue()) + "," + event.easingType().id());
        }
        return String.join(" | ", rows);
    }

    private List<NumEventData> parseEventList(String text) {
        List<NumEventData> events = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return events;
        }
        String[] rows = text.split("\\|");
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(",");
            if (parts.length < 5) {
                throw new IllegalArgumentException("Event entry requires: start,end,startValue,endValue,easingId");
            }
            events.add(new NumEventData(
                    parseDouble(parts[0]),
                    parseDouble(parts[1]),
                    parseDouble(parts[2]),
                    parseDouble(parts[3]),
                    cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.fromId(parseInt(parts[4]))
            ));
        }
        return events;
    }

    private String serializeMapEntries(List<Map<String, Object>> entries) {
        List<String> rows = new ArrayList<>();
        for (Map<String, Object> map : entries) {
            List<String> pairs = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                pairs.add(entry.getKey() + "=" + entry.getValue());
            }
            rows.add(String.join(",", pairs));
        }
        return String.join(" | ", rows);
    }

    private List<Map<String, Object>> parseMapEntries(String text) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return list;
        }
        String[] rows = text.split("\\|");
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            for (String pair : trimmed.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
            if (!map.isEmpty()) {
                list.add(map);
            }
        }
        return list;
    }

    private String serializeExtraProperties(JsonObject object) {
        if (object == null || object.entrySet().isEmpty()) {
            return "";
        }
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String value = entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : entry.getValue().toString();
            pairs.add(entry.getKey() + "=" + value);
        }
        return String.join(",", pairs);
    }

    private Map<String, String> parseKeyValuePairs(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return map;
        }
        for (String pair : text.split(",")) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] kv = trimmed.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    private List<NumEventData> parseNumEvents(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonArray()) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        List<NumEventData> events = new ArrayList<>();
        for (JsonElement element : parsed.getAsJsonArray()) {
            JsonObject object = element.getAsJsonObject();
            events.add(new NumEventData(
                    object.get("startBeat").getAsDouble(),
                    object.get("endBeat").getAsDouble(),
                    object.get("startValue").getAsDouble(),
                    object.get("endValue").getAsDouble(),
                    cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.fromId(object.get("easingType").getAsInt())
            ));
        }
        return events;
    }

    private List<Map<String, Object>> parseMapList(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonArray()) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = GSON.fromJson(parsed, List.class);
        return list == null ? List.of() : list;
    }

    private void replaceStrings(List<String> target, List<String> source) {
        target.clear();
        target.addAll(source);
    }

    private void replaceMaps(List<Map<String, Object>> target, List<Map<String, Object>> source) {
        target.clear();
        target.addAll(source);
    }

    private void replaceNumEvents(List<NumEventData> target, List<NumEventData> source) {
        target.clear();
        target.addAll(source);
    }

    private void putIfNotBlank(JsonObject object, String key, String value) {
        if (value != null && !value.isBlank()) {
            object.addProperty(key, value.trim());
        }
    }

    private void putIntIfNotBlank(JsonObject object, String key, String value) {
        if (value != null && !value.isBlank()) {
            object.addProperty(key, parseInt(value));
        }
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    private int parseInt(String value) {
        return Integer.parseInt(value.trim());
    }

    private long parseLong(String value) {
        return Long.parseLong(value.trim());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatMillis(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        long ms = Math.max(0L, millis % 1000L);
        return String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, ms);
    }

    private void drawTrimmedText(DrawContext context, String text, int x, int y, int maxWidth, int color) {
        String display = trimToWidth(text == null ? "" : text, maxWidth);
        context.drawText(textRenderer, Text.literal(display), x, y, color, false);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = textRenderer.getWidth(suffix);
        int length = text.length();
        while (length > 0 && textRenderer.getWidth(text.substring(0, length)) + suffixWidth > maxWidth) {
            length--;
        }
        return text.substring(0, Math.max(0, length)) + suffix;
    }

    private void drawStatLine(DrawContext context, int x, int y, String label, String value, int color) {
        context.drawText(textRenderer, Text.literal(label), x, y, UI_MUTED, false);
        context.drawText(textRenderer, Text.literal(value), x + 58, y, color, false);
    }

    private void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color, int thickness) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            context.fill(x1, y1, x1 + thickness, y1 + thickness, color);
            return;
        }
        int half = Math.max(0, thickness / 2);
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            context.fill(x - half, y - half, x + half + 1, y + half + 1, color);
        }
    }

    private boolean isShiftDown() {
        return InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_SHIFT);
    }

    private boolean isControlDown() {
        return InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_CONTROL);
    }

    private boolean isAltDown() {
        return InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_ALT)
                || InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_ALT);
    }

    private static final class LabeledField {
        private final String label;
        private String hint = "";
        private String section = "";
        private int sectionY = -1;
        private final TextFieldWidget widget;

        private LabeledField(String label, TextFieldWidget widget) {
            this.label = label;
            this.widget = widget;
        }
    }

    private record EditorLayout(int leftX, int topY, int leftWidth, int rightX, int rightWidth, int centerX, int centerWidth,
                                int panelHeight, int previewHeight, int timelineY, int timelineHeight, int trackBarY) {
    }

    private record PropertySectionHeader(String key, String title, int x, int y, int width, int height) {
    }

    private static final class TrackEventEditor {
        private final EventLaneType eventType;
        private final String section;
        private final ButtonWidget addButton;
        private final ButtonWidget duplicateButton;
        private final ButtonWidget deleteButton;
        private final List<TrackEventRow> rows;
        private int baseX;
        private int sectionY = -1;
        private int titleY = -1;
        private int columnsY = -1;

        private TrackEventEditor(EventLaneType eventType, String section, ButtonWidget addButton, ButtonWidget duplicateButton, ButtonWidget deleteButton, List<TrackEventRow> rows) {
            this.eventType = eventType;
            this.section = section;
            this.addButton = addButton;
            this.duplicateButton = duplicateButton;
            this.deleteButton = deleteButton;
            this.rows = rows;
        }

        private EventLaneType eventType() {
            return eventType;
        }

        private String section() {
            return section;
        }
    }

    private record ToolbarActionButton(ToolbarMenu menu, ButtonWidget button, boolean worldOnly) {
    }

    private static final class TrackEventRow {
        private final TextFieldWidget startBeat;
        private final TextFieldWidget endBeat;
        private final TextFieldWidget startValue;
        private final TextFieldWidget endValue;
        private final ButtonWidget easingButton;
        private cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType easingType = cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType.LINEAR;
        private final ButtonWidget copyButton;
        private final ButtonWidget removeButton;
        private boolean visible;
        private int y;
        private int dragX;
        private int rowRight;

        private TrackEventRow(TextFieldWidget startBeat, TextFieldWidget endBeat, TextFieldWidget startValue, TextFieldWidget endValue, ButtonWidget easingButton, ButtonWidget copyButton, ButtonWidget removeButton) {
            this.startBeat = startBeat;
            this.endBeat = endBeat;
            this.startValue = startValue;
            this.endValue = endValue;
            this.easingButton = easingButton;
            this.copyButton = copyButton;
            this.removeButton = removeButton;
        }
    }

    private record EasingPopupGroup(String title, int y) {
    }

    private record EasingPopupEntry(cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType easingType, String label, int x, int y, int width, int height) {
    }

    private record EasingPopupLayout(int width, int height, List<EasingPopupGroup> groups, List<EasingPopupEntry> entries) {
    }

    private record MapSelection(EditorSelection.Kind kind, TrackData track, NoteData note, EffectData effect, int x, int y, double distance) {
    }

    private record SelectionBox(int startX, int startY, int endX, int endY) {
    }

    private record SelectedNote(TrackData track, NoteData note) {
    }

    private record SmoothingCircle(double centerX, double centerY, double radius, double startAngle, double sweep) {
    }

    private record NoteClipboardEntry(int trackOffset, double beatOffset, NoteType noteType, Vec3Data pos, Vec3Data scale, Vec3Data rotation, int holdGroup, double holdLengthBeats) {
    }

    private record NoteClipboard(List<NoteClipboardEntry> entries, int baseTrackIndex, double baseBeat) {
    }

    private record TimelineLane(LaneType type, TrackData track, EventLaneType eventType, NoteAxis noteAxis, String eventGroup) {
        TimelineLane(LaneType type, TrackData track, EventLaneType eventType, NoteAxis noteAxis) {
            this(type, track, eventType, noteAxis, null);
        }
    }

    private record TimelineLaneLayout(TimelineLane lane, int top, int height) {
    }

    private record SelectedEventClip(TrackData track, EventLaneType eventType, int eventIndex) {
    }

    private record NoteDragSnapshot(TrackData track, double beat, double axisValue) {
    }

    private record EventClipDragSnapshot(NumEventData event, double startBeat, double endBeat) {
    }

    private record SelectedEventHandle(TrackData track, EventLaneType eventType, int eventIndex, EventAnchor anchor) {
    }

    private enum LaneType {
        BPM,
        EFFECTS,
        TRACK_HEADER,
        NOTE_AXIS,
        EVENT_GROUP_HEADER,
        EVENTS
    }

    private enum NoteAxis {
        X("Note X", 0xFF87D8FF),
        Y("Note Y", 0xFF93F2C1),
        Z("Note Z", 0xFFFFD37A);

        private final String label;
        private final int color;

        NoteAxis(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private enum EventAnchor {
        START("start"),
        END("end");

        private final String label;

        EventAnchor(String label) {
            this.label = label;
        }
    }

    private enum EventLaneType {
        SPEED("Speed", 0xFFE8B15B, 0x55452A11),
        MOVE_X("Move X", 0xFF7BC8FF, 0x55204155),
        MOVE_Y("Move Y", 0xFF90F0AE, 0x55203F26),
        MOVE_Z("Move Z", 0xFF9AD9FF, 0x5520384D),
        ROT_X("Rot X", 0xFFFF8F8F, 0x55522A2A),
        ROT_Y("Rot Y", 0xFFFFB38A, 0x55523924),
        ROT_Z("Rot Z", 0xFFFFD37A, 0x5552421F),
        SCALE_X("Scale X", 0xFFD2A8FF, 0x55432A56),
        SCALE_Y("Scale Y", 0xFFC7B5FF, 0x55372E57),
        SCALE_Z("Scale Z", 0xFFB89DFF, 0x55352C52);

        private final String label;
        private final int color;
        private final int fillColor;

        EventLaneType(String label, int color, int fillColor) {
            this.label = label;
            this.color = color;
            this.fillColor = fillColor;
        }
    }

    private enum ToolbarMenu {
        FILE("File"),
        EDIT("Edit"),
        OPTIONS("Options");

        private final String label;

        ToolbarMenu(String label) {
            this.label = label;
        }
    }

    private enum DragMode {
        NONE,
        NOTE,
        EFFECT,
        BPM,
        EVENT_HANDLE,
        EVENT_CLIP,
        HOLD_LENGTH,
        BOX_SELECT,
        TRACK_EVENT_BOX_SELECT,
        TRACK_EVENT_ROW,
        WORLD_SELECTION
    }

    public static void open() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ChartEditorScreen(new ChartEditorState()));
    }

    @Override
    public void close() {
        if (client != null && client.world != null) {
            RmcChartClient.getWorldLauncher().onEditorClosed();
            client.setScreen(null);
            return;
        }
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

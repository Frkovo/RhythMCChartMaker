package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChartEditorScreen extends Screen {
    static final int OUTER_PADDING = 10;
    static final int PANEL_GAP = 8;
    static final int TOP_BAR_HEIGHT = 86;
    static final int STATUS_BAR_HEIGHT = 20;
    static final int MIN_LEFT_PANEL_WIDTH = 220;
    static final int MAX_LEFT_PANEL_WIDTH = 280;
    static final int MIN_RIGHT_PANEL_WIDTH = 300;
    static final int MAX_RIGHT_PANEL_WIDTH = 380;
    static final int MIN_PREVIEW_HEIGHT = 72;
    static final int MAX_PREVIEW_HEIGHT = 110;
    static final int TIMELINE_SCROLLBAR_HEIGHT = 10;
    static final int BASE_ROW_HEIGHT = 24;
    static final int TRACK_HEADER_HEIGHT = 24;
    static final int NOTE_LANE_HEIGHT = 58;
    static final int EVENT_LANE_HEIGHT = 30;
    static final int TIMELINE_LABEL_WIDTH = 132;
    static final int TIMELINE_RULER_HEIGHT = 22;
    static final int TIMELINE_AUDIO_STRIP_HEIGHT = 42;
    static final int TIMELINE_ZOOM_BAR_WIDTH = 126;
    static final int TIMELINE_ZOOM_BAR_HEIGHT = 8;
    static final int TRACK_BAR_HEIGHT = 26;
    static final int FX_TRACK_HEADER_HEIGHT = 24;
    static final int FX_EFFECT_CLIP_HEIGHT = 30;
    static final int MENU_TAB_WIDTH = 70;
    static final int EVENT_DRAG_WIDTH = 14;
    static final int EVENT_CELL_WIDTH = 30;
    static final int EVENT_EASE_WIDTH = 72;
    static final int EVENT_COPY_WIDTH = 22;
    static final int EVENT_REMOVE_WIDTH = 18;
    static final int MAP_OVERLAY_HEIGHT = 42;
    static final int MAP_OVERLAY_BUTTON_WIDTH = 44;
    static final int SMOOTHING_POPUP_WIDTH = 360;
    static final int SMOOTHING_POPUP_HEIGHT = 210;
    static final double NOTE_LANE_DEFAULT_RANGE = 2.0;
    static final double NOTE_BEAT_EPSILON = 1.0E-5;
    static final int UI_BG = 0xF0101318;
    static final int UI_TOP_BG = 0xF2182028;
    static final int UI_PANEL = 0xD71B222B;
    static final int UI_PANEL_ALT = 0xC4161C23;
    static final int UI_PANEL_STRONG = 0xEA202A34;
    static final int UI_BORDER = 0xFF48596A;
    static final int UI_BORDER_SOFT = 0x7748596A;
    static final int UI_TEXT = 0xFFE9F2FA;
    static final int UI_MUTED = 0xFF9CB1C2;
    static final int UI_DIM = 0xFF6F8394;
    static final int UI_ACCENT = 0xFF7BC8FF;
    static final int UI_GREEN = 0xFF8FE1B2;
    static final int UI_WARN = 0xFFFFD37A;
    static final int GROUP_NAME_POPUP_WIDTH = 220;
    static final int GROUP_NAME_POPUP_HEIGHT = 72;

    final ChartEditorState state;
    final EditorChrome chrome;
    final EditorTimeline timeline;
    final EditorDragHandler dragHandler;
    final EditorPropertyPanel propertyPanel;
    final EditorMiscActions actions;
    final EditorPreviewBridge previewBridge;
    final EditorToolbar toolbar;
    final EditorHistory history;
    final EditorSelectionManager selectionManager;
    final EditorSmoothingEngine smoothing;
    final EditorSceneMap sceneMap;

    TextFieldWidget pathField;

    EditorSelection lastSelection = null;
    DragMode dragMode = DragMode.NONE;
    int trackScrollY = 0;
    double sceneMapZoom = 1.0;
    SelectedEventHandle selectedEventHandle;
    SelectedEventClip selectedEventClip;
    SelectionBox selectionBox;
    boolean selectionBoxAdditive;
    ToolbarMenu activeToolbarMenu = ToolbarMenu.FILE;
    double snapGuideBeat = Double.NaN;
    final Map<SelectedNote, NoteDragSnapshot> noteDragSnapshots = new java.util.HashMap<>();
    final Map<SelectedEffect, EffectDragSnapshot> effectDragSnapshots = new java.util.HashMap<>();
    final Map<SelectedEventClip, EventClipDragSnapshot> eventClipDragSnapshots = new java.util.HashMap<>();
    final Map<SelectedNote, cn.frkovo.rhythmcv2.rmcChart.chart.model.Vec3Data> mapNoteDragOrigins = new java.util.HashMap<>();
    final Map<EffectData, cn.frkovo.rhythmcv2.rmcChart.chart.model.Vec3Data> mapEffectDragOrigins = new java.util.HashMap<>();
    double dragAnchorBeat;
    double dragAnchorNoteProjectionY;
    double mapDragAnchorMouseX;
    double mapDragAnchorMouseY;
    SelectedNote draggingHoldLengthNote;
    double dragAnchorHoldLength;
    TrackEventEditor draggingTrackEventEditor;
    int draggingTrackEventSourceIndex = -1;
    int draggingTrackEventTargetIndex = -1;
    double draggingTrackEventMouseY;
    final Set<Integer> consumedKeyCodes = new HashSet<>();

    boolean groupNamePopupOpen;
    int groupNamePopupX;
    int groupNamePopupY;
    TextFieldWidget groupNameField;
    ButtonWidget groupNameApplyButton;
    ButtonWidget groupNameCancelButton;

    PendingPreviewAction pendingPreviewAction = PendingPreviewAction.NONE;
    double pendingServerPreviewBeat = Double.NaN;
    boolean openWorldPreviewOnReady;
    boolean autoPreviewStarted;

    public ChartEditorScreen(ChartEditorState state) {
        super(Text.literal("RhythMC Chart Maker"));
        this.state = state;
        this.chrome = new EditorChrome(this);
        this.timeline = new EditorTimeline(this);
        this.dragHandler = new EditorDragHandler(this);
        this.propertyPanel = new EditorPropertyPanel(this);
        this.actions = new EditorMiscActions(this);
        this.previewBridge = new EditorPreviewBridge(this);
        this.toolbar = new EditorToolbar(this);
        this.history = new EditorHistory(this);
        this.selectionManager = new EditorSelectionManager(this);
        this.smoothing = new EditorSmoothingEngine(this);
        this.sceneMap = new EditorSceneMap(this);
    }

    int publicWidth() { return width; }
    int publicHeight() { return height; }
    <T extends net.minecraft.client.gui.Element & net.minecraft.client.gui.Drawable & net.minecraft.client.gui.Selectable> T publicAddDrawableChild(T child) {
        return addDrawableChild(child);
    }
    MinecraftClient getClient() { return client; }

    int leftPanelWidth() {
        return EditorUtils.clamp((int) Math.round(width * 0.19), MIN_LEFT_PANEL_WIDTH, MAX_LEFT_PANEL_WIDTH);
    }

    int rightPanelWidth() {
        return EditorUtils.clamp((int) Math.round(width * 0.27), MIN_RIGHT_PANEL_WIDTH, MAX_RIGHT_PANEL_WIDTH);
    }

    int rightPanelInnerWidth() {
        return rightPanelWidth() - 16;
    }

    int rightPanelX() {
        return width - rightPanelWidth() - OUTER_PADDING;
    }

    private int contentTopY() {
        return TOP_BAR_HEIGHT;
    }

    private int previewHeight() {
        return EditorUtils.clamp((int) Math.round(height * 0.16), MIN_PREVIEW_HEIGHT, MAX_PREVIEW_HEIGHT);
    }

    EditorLayout editorLayout() {
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
        int timelineHeight = Math.max(TIMELINE_RULER_HEIGHT + TIMELINE_AUDIO_STRIP_HEIGHT + 48, height - timelineY - TRACK_BAR_HEIGHT - TIMELINE_SCROLLBAR_HEIGHT - STATUS_BAR_HEIGHT - OUTER_PADDING - 4);
        int trackBarY = timelineY + timelineHeight + 4;
        int scrollbarY = trackBarY + TRACK_BAR_HEIGHT;
        return new EditorLayout(leftX, topY, leftWidth, rightX, rightWidth, centerX, centerWidth, panelHeight, previewHeight, timelineY, timelineHeight, trackBarY, scrollbarY);
    }

    @Override
    protected void init() {
        clearChildren();
        propertyPanel.clearFields();
        toolbar.clearToolbar();
        dragHandler.clearDragState();
        selectionManager.clearTimelineSelections();
        smoothing.smoothingPopupOpen = false;
        groupNamePopupOpen = false;

        int topY = 8;
        pathField = addDrawableChild(new TextFieldWidget(textRenderer, 112, topY, Math.max(120, width - 392), 18, Text.literal("Project Path")));
        pathField.setMaxLength(512);
        pathField.setText(state.project().projectPath().toString());
        smoothing.createSmoothingPopupWidgets();
        RmcChartClient.getPreviewClient().setEventHandler(previewBridge::handleServerPreviewEvent);

        int buttonY = 34;
        toolbar.addTopButtons(buttonY);
        toolbar.addDifficultyButtons(58);
        propertyPanel.createPropertyFields();
        propertyPanel.populateFieldsFromSelection();
        propertyPanel.layoutPropertyFields();
        if (history.undoSnapshots.isEmpty()) {
            history.resetHistorySnapshots();
        } else {
            history.lastSnapshotRevision = state.revision();
        }
        if (!autoPreviewStarted
                && RmcChartClient.getPreviewClient().isReady()
                && !state.serverPreviewRunning()
                && !state.previewUploading()
                && !state.previewChartUploaded()
                && !state.previewAutoStarted()) {
            autoPreviewStarted = true;
            state.markPreviewAutoStarted();
            previewBridge.startWorldPreviewFromEditor(PendingPreviewAction.START);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (client != null && client.getWindow() != null) {
            consumedKeyCodes.removeIf(code -> !InputUtil.isKeyPressed(client.getWindow(), code));
        }
        if (!RmcChartClient.canOpenEditor(MinecraftClient.getInstance())) {
            state.stopServerPreviewAudio("Editor locked: preview server handshake lost");
            close();
            return;
        }
        state.tick();
        if (state.previewChartDirty()
                && !state.previewUploading()
                && RmcChartClient.getPreviewClient().isReady()) {
            previewBridge.startWorldPreviewFromEditor(PendingPreviewAction.LOAD_ONLY);
        }
        history.captureHistorySnapshotIfNeeded();
        if (!state.selection().equals(lastSelection)) {
            propertyPanel.onSelectionChanged();
            lastSelection = state.selection();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        EditorLayout layout = editorLayout();

        chrome.drawEditorChrome(context);
        chrome.drawLeftPanel(context, layout.leftX(), layout.topY(), layout.leftWidth(), layout.panelHeight(), mouseX, mouseY);
        chrome.drawPreviewPanel(context, layout.centerX(), layout.topY(), layout.centerWidth(), layout.previewHeight());
        timeline.drawTimeline(context, layout.centerX(), layout.timelineY(), layout.centerWidth(), layout.timelineHeight(), mouseX, mouseY);
        timeline.drawTrackBar(context, layout.centerX(), layout.trackBarY(), layout.centerWidth(), TRACK_BAR_HEIGHT, mouseX, mouseY);
        timeline.drawTimelineScrollbar(context, layout.centerX(), layout.scrollbarY(), layout.centerWidth(), TIMELINE_SCROLLBAR_HEIGHT);
        propertyPanel.drawPropertyPanel(context, layout.rightX(), layout.topY(), layout.rightWidth(), layout.panelHeight());
        chrome.drawStatusBar(context);
        chrome.drawSmoothingPopupBackground(context);
        chrome.drawGroupNamePopupBackground(context);

        super.render(context, mouseX, mouseY, delta);
        chrome.drawTrackEventEasingPopup(context, mouseX, mouseY);
        chrome.drawSmoothingPopupText(context);
        chrome.drawGroupNamePopupText(context);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (groupNamePopupOpen) {
            if (EditorUtils.isInside(mouseX, mouseY, groupNamePopupX(), groupNamePopupY(), GROUP_NAME_POPUP_WIDTH, GROUP_NAME_POPUP_HEIGHT)) {
                return super.mouseClicked(click, doubleClick) || true;
            }
            return true;
        }
        if (smoothing.smoothingPopupOpen) {
            if (EditorUtils.isInside(mouseX, mouseY, smoothing.smoothingPopupX(), smoothing.smoothingPopupY(), SMOOTHING_POPUP_WIDTH, SMOOTHING_POPUP_HEIGHT)) {
                return super.mouseClicked(click, doubleClick) || true;
            }
            return true;
        }
        if (propertyPanel.trackEvents.handleEasingPopupClick(mouseX, mouseY)) {
            return true;
        }
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        EditorLayout layout = editorLayout();

        if (EditorUtils.isInside(mouseX, mouseY, layout.leftX(), layout.topY(), layout.leftWidth(), layout.panelHeight())) {
            if (dragHandler.handleLeftPanelClick(mouseX, mouseY, layout.leftX(), layout.topY())) {
                return true;
            }
        }
        if (EditorUtils.isInside(mouseX, mouseY, layout.centerX(), layout.topY(), layout.centerWidth(), layout.previewHeight())) {
            if (dragHandler.handlePreviewClick(click, layout.centerX(), layout.topY(), layout.centerWidth(), layout.previewHeight())) {
                return true;
            }
        }
        if (EditorUtils.isInside(mouseX, mouseY, layout.centerX(), layout.timelineY(), layout.centerWidth(), layout.timelineHeight())) {
            if (dragHandler.handleTimelineClick(click, layout.centerX(), layout.timelineY(), layout.centerWidth(), layout.timelineHeight())) {
                return true;
            }
        }
        if (EditorUtils.isInside(mouseX, mouseY, layout.centerX(), layout.trackBarY(), layout.centerWidth(), TRACK_BAR_HEIGHT)) {
            if (dragHandler.handleTrackBarClick(mouseX, mouseY, layout.centerX(), layout.trackBarY(), layout.centerWidth(), TRACK_BAR_HEIGHT)) {
                return true;
            }
        }
        if (EditorUtils.isInside(mouseX, mouseY, layout.centerX(), layout.scrollbarY(), layout.centerWidth(), TIMELINE_SCROLLBAR_HEIGHT)) {
            dragHandler.startTimelineScrollbarDrag(mouseX);
            return true;
        }
        if (EditorUtils.isInside(mouseX, mouseY, layout.rightX(), layout.topY(), layout.rightWidth(), layout.panelHeight())) {
            if (propertyPanel.handlePropertyPanelClick(click, mouseX, mouseY)) {
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
                    propertyPanel.populateFieldsFromSelection();
                }
            } else if (dragMode == DragMode.TRACK_EVENT_BOX_SELECT) {
                propertyPanel.trackEvents.trackEventSelectionBox = propertyPanel.trackEvents.trackEventSelectionBox == null
                        ? new SelectionBox((int) click.x(), (int) click.y(), (int) click.x(), (int) click.y())
                        : new SelectionBox(propertyPanel.trackEvents.trackEventSelectionBox.startX(), propertyPanel.trackEvents.trackEventSelectionBox.startY(), (int) click.x(), (int) click.y());
            } else if (dragMode == DragMode.TRACK_EVENT_ROW) {
                propertyPanel.trackEvents.draggingTrackEventMouseY = click.y();
                propertyPanel.trackEvents.updateDraggedTrackEventTarget(click.y());
            } else if (dragMode == DragMode.MAP_NOTE || dragMode == DragMode.MAP_EFFECT) {
                EditorLayout layout = editorLayout();
                int mapX = layout.leftX() + 10;
                int mapY = layout.topY() + 28;
                int mapSize = layout.leftWidth() - 20;
                sceneMap.handleCurrentFrameMapDrag(click.x(), click.y(), mapX, mapY, mapSize, mapSize);
            } else if (dragMode == DragMode.TIMELINE_ZOOM) {
                EditorLayout layout = editorLayout();
                int contentX = layout.centerX() + TIMELINE_LABEL_WIDTH;
                int contentWidth = Math.max(48, layout.centerWidth() - TIMELINE_LABEL_WIDTH);
                dragHandler.setTimelineZoomFromMouseX(click.x(), contentX, contentWidth);
            } else if (dragMode == DragMode.TIMELINE_SCROLL) {
                dragHandler.handleTimelineScrollbarDrag(click.x());
            } else {
                dragHandler.handleTimelineDrag(click.x(), click.y());
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragMode == DragMode.BOX_SELECT && selectionBox != null) {
            dragHandler.applySelectionBox();
        }
        if (dragMode == DragMode.TRACK_EVENT_BOX_SELECT && propertyPanel.trackEvents.trackEventSelectionBox != null) {
            propertyPanel.trackEvents.applyTrackEventSelectionBox();
        }
        if (dragMode == DragMode.TRACK_EVENT_ROW) {
            propertyPanel.trackEvents.applyDraggedTrackEventReorder();
        }
        dragHandler.endDrag();
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        EditorLayout layout = editorLayout();
        int mapSize = layout.leftWidth() - 20;
        int mapX = layout.leftX() + 10;
        int mapY = layout.topY() + 28;
        if (EditorUtils.isInside(mouseX, mouseY, mapX, mapY, mapSize, mapSize)) {
            double factor = verticalAmount > 0 ? 1.15 : 0.87;
            sceneMapZoom = EditorUtils.clamp(sceneMapZoom * factor, 0.25, 8.0);
            state.setStatus("Scene Map zoom " + EditorUtils.format(sceneMapZoom) + "x");
            return true;
        }
        if (EditorUtils.isInside(mouseX, mouseY, layout.rightX(), layout.topY(), layout.rightWidth(), layout.panelHeight())) {
            propertyPanel.propertyScroll += (int) Math.round(verticalAmount * 16.0);
            propertyPanel.propertyScroll = EditorUtils.clamp(propertyPanel.propertyScroll, -1600, 0);
            propertyPanel.layoutPropertyFields();
            return true;
        }
        if (EditorUtils.isInside(mouseX, mouseY, layout.centerX(), layout.timelineY(), layout.centerWidth(), layout.timelineHeight())) {
            if (isControlDown()) {
                state.zoom(verticalAmount > 0 ? 0.9 : 1.1);
                state.setStatus("Timeline zoom: " + EditorUtils.format(state.beatsPerScreen()) + " beats");
            } else if (isShiftDown()) {
                state.scrollWindow(-verticalAmount * (state.beatsPerScreen() / 8.0));
            } else {
                int contentHeight = layout.timelineHeight() - TIMELINE_RULER_HEIGHT - TIMELINE_AUDIO_STRIP_HEIGHT;
                int maxScroll = Math.max(0, timeline.totalTimelineLaneHeight() - contentHeight);
                trackScrollY += (int) Math.round(verticalAmount * 32.0);
                trackScrollY = EditorUtils.clamp(trackScrollY, 0, maxScroll);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        if (groupNamePopupOpen) {
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.cancelKeyBinding())) {
                actions.closeGroupNamePopup();
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_ENTER || keyCode == InputUtil.GLFW_KEY_KP_ENTER) {
                actions.applyGroupNameFromPopup();
                return true;
            }
            if (super.keyPressed(keyInput)) {
                return true;
            }
            return true;
        }
        if (smoothing.smoothingPopupOpen) {
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.cancelKeyBinding())) {
                smoothing.closeSmoothingPopup();
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_ENTER || keyCode == InputUtil.GLFW_KEY_KP_ENTER) {
                smoothing.applySmoothingFromPopup();
                return true;
            }
            if (super.keyPressed(keyInput)) {
                return true;
            }
            return true;
        }
        if (isKeyPressedThisFrame(keyInput, RmcChartClient.smoothingPopupKeyBinding()) && isControlDown()) {
            smoothing.openSmoothingPopup();
            return true;
        }
        if (super.keyPressed(keyInput)) {
            return true;
        }
        if (isControlDown()) {
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.undoKeyBinding())) {
                if (isShiftDown()) {
                    history.redoEditorChange();
                } else {
                    history.undoEditorChange();
                }
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.redoKeyBinding())) {
                history.redoEditorChange();
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.newProjectKeyBinding())) {
                actions.openNewProjectWizard();
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.openProjectKeyBinding())) {
                actions.openProjectBrowser();
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.selectAllKeyBinding())) {
                selectionManager.selectAllNotes();
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.copyKeyBinding())) {
                if (!selectionManager.currentSelectedEffects().isEmpty()) {
                    selectionManager.copySelectedEffectsToClipboard();
                } else {
                    selectionManager.copySelectedNotesToClipboard();
                }
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.cutKeyBinding())) {
                if (!selectionManager.currentSelectedEffects().isEmpty()) {
                    selectionManager.cutSelectedEffectsToClipboard();
                } else {
                    selectionManager.cutSelectedNotesToClipboard();
                }
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.pasteKeyBinding())) {
                if (selectionManager.effectClipboard != null && !selectionManager.effectClipboard.entries().isEmpty()
                        && (selectionManager.noteClipboard == null || selectionManager.noteClipboard.entries().isEmpty()
                        || !selectionManager.currentSelectedEffects().isEmpty())) {
                    selectionManager.pasteClipboardEffects();
                } else {
                    selectionManager.pasteClipboardNotes();
                }
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_G) {
                actions.groupSelectedObjects();
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_D) {
                actions.duplicateSelectedObjects();
                return true;
            }
        }
        if (isAltDown() && selectionManager.hasSelectedNotes()) {
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.seekBackwardKeyBinding())) {
                selectionManager.moveSelectedNotesByShortcut(-selectionManager.noteShortcutBeatStep(), 0);
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.seekForwardKeyBinding())) {
                selectionManager.moveSelectedNotesByShortcut(selectionManager.noteShortcutBeatStep(), 0);
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_UP) {
                selectionManager.moveSelectedNotesByShortcut(0.0, -1);
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_DOWN) {
                selectionManager.moveSelectedNotesByShortcut(0.0, 1);
                return true;
            }
        }
        if (!isControlDown() && !isAltDown()) {
            if (getFocused() instanceof TextFieldWidget) {
                return false;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.noteTapKeyBinding())) {
                actions.createNoteFromShortcut(NoteType.TAP);
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.noteLookKeyBinding())) {
                actions.createNoteFromShortcut(NoteType.LOOK);
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.noteHoldKeyBinding())) {
                actions.createNoteFromShortcut(NoteType.HOLD);
                return true;
            }
            if (isKeyPressedThisFrame(keyInput, RmcChartClient.noteDodgeKeyBinding())) {
                actions.createNoteFromShortcut(NoteType.DODGE);
                return true;
            }
        }
        if (isKeyPressedThisFrame(keyInput, RmcChartClient.cancelKeyBinding())) {
            selectionManager.clearTimelineSelections();
            if (state.selection().kind() == EditorSelection.Kind.NOTE) {
                state.setSelection(EditorSelection.track(state.selection().track()));
            }
            return true;
        }
        if (isKeyPressedThisFrame(keyInput, RmcChartClient.playPreviewKeyBinding()) && !isControlDown() && !isAltDown()) {
            if (getFocused() instanceof TextFieldWidget) {
                return false;
            }
            if (state.playing()) {
                state.stopPlayback();
                RmcChartClient.getPreviewClient().sendPreviewStop();
            } else {
                state.startPlaybackAt(state.playheadBeat(), "Playback started");
                RmcChartClient.getPreviewClient().sendPreviewStart(state.playheadBeat());
            }
            return true;
        }
        if (isKeyPressedThisFrame(keyInput, RmcChartClient.toggleLocalPlaybackKeyBinding())) {
            if (!(getFocused() instanceof TextFieldWidget)) {
                state.togglePlayback();
                return true;
            }
        }
        if (isKeyPressedThisFrame(keyInput, RmcChartClient.openEditorKeyBinding()) && !isControlDown() && !isAltDown()) {
            close();
            return true;
        }
        if (isKeyPressedThisFrame(keyInput, RmcChartClient.deleteKeyBinding())) {
            actions.deleteCurrentSelection();
            return true;
        }
        if (keyCode == InputUtil.GLFW_KEY_BACKSPACE && !(getFocused() instanceof TextFieldWidget)) {
            actions.deleteCurrentSelection();
            return true;
        }
        if (isKeyPressedThisFrame(keyInput, RmcChartClient.seekForwardKeyBinding())) {
            state.seekToBeat(state.playheadBeat() + 0.25);
            return true;
        }
        if (isKeyPressedThisFrame(keyInput, RmcChartClient.seekBackwardKeyBinding())) {
            state.seekToBeat(state.playheadBeat() - 0.25);
            return true;
        }
        if (isControlDown() && isKeyPressedThisFrame(keyInput, RmcChartClient.saveKeyBinding())) {
            actions.saveProject();
            return true;
        }
        if (!(getFocused() instanceof TextFieldWidget)) {
            if (keyCode == InputUtil.GLFW_KEY_HOME) {
                state.seekToBeat(0.0);
                state.setStatus("Jumped to start");
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_END) {
                state.seekToBeat(timeline.totalTimelineBeats());
                state.setStatus("Jumped to end");
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_PAGE_UP) {
                timeline.scrollTimelineLanes(-1);
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_PAGE_DOWN) {
                timeline.scrollTimelineLanes(1);
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_EQUAL) {
                state.zoom(0.9);
                state.setStatus("Zoom in: " + EditorUtils.format(state.beatsPerScreen()) + " beats");
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_MINUS) {
                state.zoom(1.1);
                state.setStatus("Zoom out: " + EditorUtils.format(state.beatsPerScreen()) + " beats");
                return true;
            }
        }
        return false;
    }

    int groupNamePopupX() {
        return (width - GROUP_NAME_POPUP_WIDTH) / 2;
    }

    int groupNamePopupY() {
        return (height - GROUP_NAME_POPUP_HEIGHT) / 2;
    }

    int beatToScreen(int x, int width, double beat) {
        return x + (int) Math.round((beat - state.visibleStartBeat()) / state.beatsPerScreen() * width);
    }

    double screenToBeat(int x, int width, double screenX) {
        double progress = (screenX - x) / width;
        return state.visibleStartBeat() + progress * state.beatsPerScreen();
    }

    double snapBeat(double beat) {
        return snapBeat(beat, null);
    }

    double snapBeat(double beat, TrackData track) {
        if (isAltDown()) {
            return beat;
        }
        int division = track == null ? 16 : track.beatDivision();
        double grid = Math.max(1.0 / 64.0, 4.0 / Math.max(1, division));
        return Math.round(beat / grid) * grid;
    }

    List<NumEventData> eventsForLane(TrackData track, EventLaneType eventType) {
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

    NumEventData eventForClip(SelectedEventClip clip) {
        if (clip == null) {
            return null;
        }
        List<NumEventData> events = eventsForLane(clip.track(), clip.eventType());
        if (clip.eventIndex() < 0 || clip.eventIndex() >= events.size()) {
            return null;
        }
        return events.get(clip.eventIndex());
    }

    boolean isEventClipSelected(TrackData track, EventLaneType eventType, int eventIndex) {
        return selectionManager.selectedEventClips.contains(new SelectedEventClip(track, eventType, eventIndex));
    }

    int countVisibleEventLanes(TrackData track) {
        int count = 0;
        for (EventLaneType eventType : EventLaneType.values()) {
            if (!eventsForLane(track, eventType).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    boolean isShiftDown() {
        return InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_SHIFT);
    }

    boolean isControlDown() {
        return InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_CONTROL);
    }

    boolean isAltDown() {
        return InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_ALT)
                || InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_ALT);
    }

    int countTrackEvents(TrackData track) {
        return track.speedEvents().size()
                + track.xTransformEvents().size() + track.yTransformEvents().size() + track.zTransformEvents().size()
                + track.xRotateEvents().size() + track.yRotateEvents().size() + track.zRotateEvents().size()
                + track.xScaleEvents().size() + track.yScaleEvents().size() + track.zScaleEvents().size();
    }

    double trackGridStep(TrackData track) {
        int division = track == null ? 16 : track.beatDivision();
        return Math.max(1.0 / 64.0, 4.0 / Math.max(1, division));
    }

    double snapHoldLength(double length, TrackData track) {
        double step = trackGridStep(track);
        return Math.max(step, Math.round(Math.max(0.0, length) / step) * step);
    }

    double effectEndBeat(EffectData effect) {
        if (effect == null || effect.properties() == null) {
            return effect == null ? 0.0 : effect.beat();
        }
        long durationMs = effect.properties().has("duration") ? effect.properties().get("duration").getAsLong() : 0L;
        if (durationMs > 0L) {
            double startMs = state.timing().beatToMillis(effect.beat());
            return state.timing().calcBeat((long) (startMs + durationMs));
        }
        return effect.beat() + 1.0;
    }

    String effectInfoLabel(EffectData effect) {
        return EditorUtils.shortEffectLabel(effect.effectType()) + " @ " + EditorUtils.format(effect.beat());
    }

    String effectClipText(EffectData effect) {
        JsonObject properties = effect.properties() == null ? new JsonObject() : effect.properties();
        String text = EditorUtils.firstProperty(properties, "text", "content", "title", "message", "value", "id");
        if (text == null || text.isBlank()) {
            text = EditorUtils.propertyString(properties, "id", "-");
        }
        return EditorUtils.trimToWidth(getTextRenderer(), text, Math.max(40, TIMELINE_LABEL_WIDTH - 24));
    }

    String shortEasingLabel(String easingName) {
        return easingName.length() <= 8 ? easingName : easingName.substring(0, 8);
    }

    String subdivisionLabel(double beat, TrackData track) {
        int beatIndex = (int) Math.floor(beat);
        double fraction = beat - beatIndex;
        int division = track == null ? 16 : track.beatDivision();
        int partsPerBeat = Math.max(1, division / 4);
        int part = EditorUtils.clamp((int) Math.round(fraction * partsPerBeat), 0, partsPerBeat - 1) + 1;
        return "B" + beatIndex + " " + part + "/" + partsPerBeat;
    }

    void refreshAfterMetaEdit() {
        propertyPanel.populateFieldsFromSelection();
        propertyPanel.layoutPropertyFields();
    }

    public static void open() {
        RmcChartClient.openActiveEditor();
    }

    @Override
    public void close() {
        RmcChartClient.getPreviewClient().setEventHandler(null);
        if (client != null && client.world != null) {
            RmcChartClient.getWorldLauncher().onEditorClosed();
            client.setScreen(null);
            return;
        }
        super.close();
    }

    private boolean isKeyPressedThisFrame(KeyInput keyInput, KeyBinding binding) {
        if (binding == null || !binding.matchesKey(keyInput)) {
            return false;
        }
        int keyCode = keyInput.key();
        if (consumedKeyCodes.contains(keyCode)) {
            return false;
        }
        consumedKeyCodes.add(keyCode);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

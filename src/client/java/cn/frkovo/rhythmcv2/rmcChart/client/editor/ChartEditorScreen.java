package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartMath;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectType;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ChartEditorScreen extends Screen {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int LEFT_PANEL_WIDTH = 220;
    private static final int RIGHT_PANEL_WIDTH = 290;
    private static final int PREVIEW_HEIGHT = 150;
    private static final int BASE_ROW_HEIGHT = 24;
    private static final int TRACK_HEADER_HEIGHT = 24;
    private static final int NOTE_LANE_HEIGHT = 58;
    private static final int EVENT_LANE_HEIGHT = 30;
    private static final int TIMELINE_RULER_HEIGHT = 24;
    private static final int TIMELINE_LABEL_WIDTH = 132;
    private static final int TRACK_BAR_HEIGHT = 26;
    private static final int TIMELINE_ZOOM_BAR_WIDTH = 132;
    private static final int TIMELINE_ZOOM_BAR_HEIGHT = 6;
    private static final double NOTE_LANE_DEFAULT_RANGE = 2.0;

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

    private EditorSelection lastSelection = null;
    private DragMode dragMode = DragMode.NONE;
    private int propertyScroll = 0;
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
    private double dragAnchorBeat;
    private double dragAnchorAxisValue;

    public ChartEditorScreen(ChartEditorState state) {
        super(Text.literal("RhythMC Chart Maker"));
        this.state = state;
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

        int topY = 8;
        pathField = addDrawableChild(new TextFieldWidget(textRenderer, 12, topY, width - 24, 18, Text.literal("Project Path")));
        pathField.setMaxLength(512);
        pathField.setText(state.project().projectPath().toString());

        int buttonY = 32;
        addTopButtons(buttonY);
        addDifficultyButtons(buttonY + 24);
        createPropertyFields();
        populateFieldsFromSelection();
        layoutPropertyFields();
    }

    @Override
    public void tick() {
        super.tick();
        state.tick();
        if (!state.selection().equals(lastSelection)) {
            propertyScroll = 0;
            populateFieldsFromSelection();
            layoutPropertyFields();
            lastSelection = state.selection();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int topY = 60;
        int leftX = 12;
        int previewX = leftX + LEFT_PANEL_WIDTH + 8;
        int rightX = width - RIGHT_PANEL_WIDTH - 12;
        int previewWidth = rightX - previewX - 8;
        int timelineY = topY + PREVIEW_HEIGHT + 8;
        int timelineHeight = height - timelineY - TRACK_BAR_HEIGHT - 34;
        int trackBarY = timelineY + timelineHeight + 4;

        drawLeftPanel(context, leftX, topY, LEFT_PANEL_WIDTH, height - topY - 28, mouseX, mouseY);
        drawPreviewPanel(context, previewX, topY, previewWidth, PREVIEW_HEIGHT);
        drawTimeline(context, previewX, timelineY, previewWidth, timelineHeight, mouseX, mouseY);
        drawTrackBar(context, previewX, trackBarY, previewWidth, TRACK_BAR_HEIGHT, mouseX, mouseY);
        drawPropertyPanel(context, rightX, topY, RIGHT_PANEL_WIDTH, height - topY - 28);
        context.drawText(textRenderer, Text.literal(state.statusMessage()), 12, height - 18, 0xFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        double mouseX = click.x();
        double mouseY = click.y();

        int topY = 60;
        int leftX = 12;
        int previewX = leftX + LEFT_PANEL_WIDTH + 8;
        int rightX = width - RIGHT_PANEL_WIDTH - 12;
        int previewWidth = rightX - previewX - 8;
        int timelineY = topY + PREVIEW_HEIGHT + 8;
        int timelineHeight = height - timelineY - TRACK_BAR_HEIGHT - 34;
        int trackBarY = timelineY + timelineHeight + 4;

        if (isInside(mouseX, mouseY, leftX, topY, LEFT_PANEL_WIDTH, height - topY - 28)) {
            if (handleLeftPanelClick(mouseX, mouseY, leftX, topY)) {
                return true;
            }
        }
        if (isInside(mouseX, mouseY, previewX, topY, previewWidth, PREVIEW_HEIGHT)) {
            if (handlePreviewClick(click, previewX, topY, previewWidth, PREVIEW_HEIGHT)) {
                return true;
            }
        }
        if (isInside(mouseX, mouseY, previewX, timelineY, previewWidth, timelineHeight)) {
            if (handleTimelineClick(click, previewX, timelineY, previewWidth, timelineHeight)) {
                return true;
            }
        }
        if (isInside(mouseX, mouseY, previewX, trackBarY, previewWidth, TRACK_BAR_HEIGHT)) {
            if (handleTrackBarClick(mouseX, mouseY, previewX, trackBarY, previewWidth, TRACK_BAR_HEIGHT)) {
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
        dragMode = DragMode.NONE;
        selectionBox = null;
        snapGuideBeat = Double.NaN;
        noteDragSnapshots.clear();
        eventClipDragSnapshots.clear();
        return super.mouseReleased(click);
    }

    private void deleteActiveSelection() {
        if (selectedNotes.isEmpty() && selectedEventClips.isEmpty()) {
            state.deleteSelection();
            populateFieldsFromSelection();
            return;
        }
        int deletedNotes = 0;
        for (SelectedNote selectedNote : new ArrayList<>(selectedNotes)) {
            if (selectedNote.track() != null && selectedNote.note() != null && selectedNote.track().notes().remove(selectedNote.note())) {
                deletedNotes++;
            }
        }
        int deletedClips = 0;
        for (SelectedEventClip clip : new ArrayList<>(selectedEventClips)) {
            NumEventData event = eventForClip(clip);
            if (event != null && eventsForLane(clip.track(), clip.eventType()).remove(event)) {
                deletedClips++;
            }
        }
        selectedNotes.clear();
        selectedEventClips.clear();
        selectedEventHandle = null;
        selectedEventClip = null;
        selectionBox = null;
        noteDragSnapshots.clear();
        eventClipDragSnapshots.clear();
        state.setSelection(EditorSelection.meta());
        state.sortCurrentLevel();
        state.markDirty();
        if (deletedNotes > 0 || deletedClips > 0) {
            state.setStatus("Deleted " + deletedNotes + " note(s), " + deletedClips + " event clip(s)");
        }
        populateFieldsFromSelection();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int topY = 60;
        int rightX = width - RIGHT_PANEL_WIDTH - 12;
        if (isInside(mouseX, mouseY, rightX, topY, RIGHT_PANEL_WIDTH, height - topY - 28)) {
            propertyScroll += (int) Math.round(verticalAmount * 16.0);
            propertyScroll = clamp(propertyScroll, -300, 0);
            layoutPropertyFields();
            return true;
        }
        int leftX = 12 + LEFT_PANEL_WIDTH + 8;
        int timelineY = topY + PREVIEW_HEIGHT + 8;
        int timelineWidth = rightX - leftX - 8;
        int timelineHeight = height - timelineY - TRACK_BAR_HEIGHT - 34;
        if (isInside(mouseX, mouseY, leftX, timelineY, timelineWidth, timelineHeight)) {
            if (isShiftDown()) {
                state.zoom(verticalAmount > 0 ? 0.9 : 1.1);
            } else {
                state.scrollWindow(-verticalAmount * (state.beatsPerScreen() / 8.0));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (super.keyPressed(keyInput)) {
            return true;
        }
        int keyCode = keyInput.key();
        if (keyCode == InputUtil.GLFW_KEY_SPACE) {
            state.togglePlayback();
            return true;
        }
        if (keyCode == InputUtil.GLFW_KEY_DELETE || keyCode == InputUtil.GLFW_KEY_BACKSPACE) {
            deleteActiveSelection();
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

    private void addTopButtons(int y) {
        int x = 12;
        x = addButton(x, y, 48, "New", b -> newProject()) + 4;
        x = addButton(x, y, 48, "Load", b -> loadProject()) + 4;
        x = addButton(x, y, 48, "Save", b -> saveProject()) + 8;
        x = addButton(x, y, 54, "Audio", b -> reloadAudio()) + 8;
        if (client != null && client.world != null) {
            x = addButton(x, y, 46, "Sync", b -> syncWorldDisplays()) + 4;
            x = addButton(x, y, 46, "Pick", b -> pickWorldDisplay()) + 8;
        }
        x = addButton(x, y, 50, "Song", b -> selectSong()) + 4;
        x = addButton(x, y, 50, "Meta", b -> selectMeta()) + 4;
        x = addButton(x, y, 56, "+Track", b -> state.addTrack()) + 4;
        x = addButton(x, y, 52, "+Note", b -> state.addNote()) + 4;
        x = addButton(x, y, 52, "+Fx", b -> state.addEffect()) + 4;
        x = addButton(x, y, 58, "+BPM", b -> state.addBpm()) + 4;
        x = addButton(x, y, 56, "Delete", b -> deleteActiveSelection()) + 8;
        x = addButton(x, y, 48, "Play", b -> state.togglePlayback()) + 4;
        x = addButton(x, y, 48, "Stop", b -> state.stopPlayback()) + 4;
        if (client != null && client.world != null) {
            addButton(x, y, 48, "Title", b -> MinecraftClient.getInstance().disconnect(new TitleScreen(), false));
        }
    }

    private void addDifficultyButtons(int y) {
        int x = 12;
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

    private int addButton(int x, int y, int width, String label, ButtonWidget.PressAction action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), action).dimensions(x, y, width, 18).build());
        return x + width;
    }

    private void createPropertyFields() {
        int rightX = width - RIGHT_PANEL_WIDTH - 12 + 8;
        int fieldWidth = RIGHT_PANEL_WIDTH - 16;

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
        trackFields.add(field(rightX, fieldWidth, "Speed Events"));
        trackFields.add(field(rightX, fieldWidth, "X Transform"));
        trackFields.add(field(rightX, fieldWidth, "Y Transform"));
        trackFields.add(field(rightX, fieldWidth, "Z Transform"));
        trackFields.add(field(rightX, fieldWidth, "X Rotate"));
        trackFields.add(field(rightX, fieldWidth, "Y Rotate"));
        trackFields.add(field(rightX, fieldWidth, "Z Rotate"));
        trackFields.add(field(rightX, fieldWidth, "X Scale"));
        trackFields.add(field(rightX, fieldWidth, "Y Scale"));
        trackFields.add(field(rightX, fieldWidth, "Z Scale"));

        noteFields.add(field(rightX, fieldWidth, "Beat"));
        noteFields.add(field(rightX, fieldWidth, "Type"));
        noteFields.add(field(rightX, fieldWidth, "Track ID"));
        noteFields.add(field(rightX, fieldWidth, "Center Pos(x,y,z)"));
        noteFields.add(field(rightX, fieldWidth, "Scale(x,y,z)"));
        noteFields.add(field(rightX, fieldWidth, "Rot(x,y,z)"));
        noteFields.add(field(rightX, fieldWidth, "Hold Group"));

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
    }

    private LabeledField field(int x, int width, String label) {
        TextFieldWidget widget = addDrawableChild(new TextFieldWidget(textRenderer, x, 0, width, 18, Text.literal(label)));
        widget.setMaxLength(4096);
        return new LabeledField(label, widget);
    }

    private void applyInputHints() {
        setFieldHint(songFields, 0, "Song title shown in song list");
        setFieldHint(songFields, 1, "Main composer / artist name");
        setFieldHint(songFields, 2, "Minecraft material icon key");
        setFieldHint(songFields, 3, "Short alias / song code");
        setFieldHint(songFields, 4, "Audio length in milliseconds");
        setFieldHint(songFields, 5, "Resource pack sha1, optional");
        setFieldHint(songFields, 6, "Song description text");
        setFieldHint(songFields, 7, "Unique integer song id");
        setFieldHint(songFields, 8, "Manifest version string");
        setFieldHint(songFields, 9, "Comma-separated comments");
        setFieldHint(songFields, 10, "Comma-separated player alias");
        setFieldHint(songFields, 11, "Comma-separated tags");
        setFieldHint(songFields, 12, "One line each: key=value,key=value");
        setFieldHint(songFields, 13, "One line each: key=value,key=value");
        setFieldHint(songFields, 14, "One line each: key=value,key=value");
        setFieldHint(songFields, 15, "One line each: key=value,key=value");

        setFieldHint(metaFields, 0, "Difficulty UID integer");
        setFieldHint(metaFields, 1, "Initial arena id");
        setFieldHint(metaFields, 2, "Audio offset in ms");
        setFieldHint(metaFields, 3, "Difficulty level number");
        setFieldHint(metaFields, 4, "Comma-separated charters");
        setFieldHint(metaFields, 5, "Comma-separated comments");

        setFieldHint(trackFields, 0, "TrackID integer");
        for (int i = 1; i < trackFields.size(); i++) {
            setFieldHint(trackFields, i, "Event format: start,end,startValue,endValue,easingId | ...");
        }

        setFieldHint(noteFields, 0, "Beat position");
        setFieldHint(noteFields, 1, "TAP / LOOK / HOLD / DODGE");
        setFieldHint(noteFields, 2, "Target TrackID");
        setFieldHint(noteFields, 3, "Center-relative x,y,z; decimals allowed");
        setFieldHint(noteFields, 4, "x,y,z local scale");
        setFieldHint(noteFields, 5, "x,y,z local rotation");
        setFieldHint(noteFields, 6, "Hold group id, -1 means none");

        setFieldHint(effectFields, 0, "Beat position");
        setFieldHint(effectFields, 1, "EffectType enum name");
        setFieldHint(effectFields, 2, "Text display / effect id");
        setFieldHint(effectFields, 3, "Main text or value field");
        setFieldHint(effectFields, 4, "Color string (e.g. aqua)");
        setFieldHint(effectFields, 5, "TrackID when needed");
        setFieldHint(effectFields, 6, "x,y,z position");
        setFieldHint(effectFields, 7, "x,y,z scale");
        setFieldHint(effectFields, 8, "x,y,z rotation");
        setFieldHint(effectFields, 9, "Arena or target value");
        setFieldHint(effectFields, 10, "Mode / state / action");
        setFieldHint(effectFields, 11, "Extra keys: key=value,key=value");

        setFieldHint(bpmFields, 0, "Beat position");
        setFieldHint(bpmFields, 1, "BPM value");
    }

    private void setFieldHint(List<LabeledField> fields, int index, String hint) {
        if (index >= 0 && index < fields.size()) {
            fields.get(index).widget.setPlaceholder(Text.literal(hint));
        }
    }

    private void layoutPropertyFields() {
        int panelX = width - RIGHT_PANEL_WIDTH - 12 + 8;
        int currentY = 110 + propertyScroll;
        List<LabeledField> activeFields = activeFieldGroup();
        for (LabeledField field : allFields()) {
            boolean visible = activeFields.contains(field);
            field.widget.visible = visible;
            field.widget.active = visible;
            if (visible) {
                field.widget.setPosition(panelX, currentY);
                currentY += 26;
            }
        }
        applyButton.setPosition(panelX, height - 42);
        resetButton.setPosition(panelX + 92, height - 42);
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

        if (state.selection().track() != null) {
            TrackData track = state.selection().track();
            set(trackFields, 0, Integer.toString(track.id()));
            set(trackFields, 1, serializeEventList(track.speedEvents()));
            set(trackFields, 2, serializeEventList(track.xTransformEvents()));
            set(trackFields, 3, serializeEventList(track.yTransformEvents()));
            set(trackFields, 4, serializeEventList(track.zTransformEvents()));
            set(trackFields, 5, serializeEventList(track.xRotateEvents()));
            set(trackFields, 6, serializeEventList(track.yRotateEvents()));
            set(trackFields, 7, serializeEventList(track.zRotateEvents()));
            set(trackFields, 8, serializeEventList(track.xScaleEvents()));
            set(trackFields, 9, serializeEventList(track.yScaleEvents()));
            set(trackFields, 10, serializeEventList(track.zScaleEvents()));
        }

        if (state.selection().note() != null) {
            NoteData note = state.selection().note();
            set(noteFields, 0, format(note.beat()));
            set(noteFields, 1, note.noteType().name());
            set(noteFields, 2, Integer.toString(state.selection().track().id()));
            set(noteFields, 3, serializeVec(note.pos()));
            set(noteFields, 4, serializeVec(note.scale()));
            set(noteFields, 5, serializeVec(note.rotation()));
            set(noteFields, 6, Integer.toString(note.holdGroup()));
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
        replaceNumEvents(track.speedEvents(), parseEventList(get(trackFields, 1)));
        replaceNumEvents(track.xTransformEvents(), parseEventList(get(trackFields, 2)));
        replaceNumEvents(track.yTransformEvents(), parseEventList(get(trackFields, 3)));
        replaceNumEvents(track.zTransformEvents(), parseEventList(get(trackFields, 4)));
        replaceNumEvents(track.xRotateEvents(), parseEventList(get(trackFields, 5)));
        replaceNumEvents(track.yRotateEvents(), parseEventList(get(trackFields, 6)));
        replaceNumEvents(track.zRotateEvents(), parseEventList(get(trackFields, 7)));
        replaceNumEvents(track.xScaleEvents(), parseEventList(get(trackFields, 8)));
        replaceNumEvents(track.yScaleEvents(), parseEventList(get(trackFields, 9)));
        replaceNumEvents(track.zScaleEvents(), parseEventList(get(trackFields, 10)));
    }

    private void applyNoteFields() {
        NoteData note = state.selection().note();
        TrackData currentTrack = state.selection().track();
        note.setBeat(parseDouble(get(noteFields, 0)));
        note.setNoteType(NoteType.valueOf(get(noteFields, 1).trim().toUpperCase(Locale.ROOT)));
        Vec3Data pos = parseVec(get(noteFields, 3));
        note.pos().set(pos.x(), pos.y(), pos.z());
        Vec3Data scale = parseVec(get(noteFields, 4));
        note.scale().set(scale.x(), scale.y(), scale.z());
        Vec3Data rotation = parseVec(get(noteFields, 5));
        note.rotation().set(rotation.x(), rotation.y(), rotation.z());
        note.setHoldGroup(parseInt(get(noteFields, 6)));
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

    private void drawLeftPanel(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, 0x55202020);
        drawOutline(context, x, y, width, height, 0xFF606060);
        context.drawText(textRenderer, Text.literal("Current Frame Map"), x + 8, y + 8, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Live planar view at beat " + format(state.playheadBeat())), x + 8, y + 18, 0x90A8B8, false);

        int mapX = x + 8;
        int mapY = y + 32;
        int mapSize = width - 16;
        drawCurrentFrameMap(context, mapX, mapY, mapSize, mapSize);

        int rowY = mapY + mapSize + 8;
        context.drawText(textRenderer, Text.literal("Track List - " + state.activeDifficulty().displayName()), x + 8, rowY, 0xFFFFFF, false);
        rowY += 12;
        context.drawText(textRenderer, Text.literal("Click icon to expand / collapse"), x + 8, rowY, 0x90A8B8, false);
        rowY += 10;
        for (TrackData track : state.tracks()) {
            boolean selected = state.selection().track() == track || (state.selection().kind() == EditorSelection.Kind.NOTE && state.selection().track() == track);
            int color = selected ? 0xAA3A6EA5 : 0x66303030;
            context.fill(x + 6, rowY, x + width - 6, rowY + 22, color);
            context.fill(x + 10, rowY + 4, x + 24, rowY + 18, 0x55000000);
            context.drawText(textRenderer, Text.literal(state.isTrackExpanded(track) ? "-" : "+"), x + 15, rowY + 7, 0xFFFFFF, false);
            context.drawText(textRenderer, Text.literal("TrackID: " + track.id()), x + 30, rowY + 4, 0xFFFFFF, false);
            context.drawText(textRenderer, Text.literal("Notes " + track.notes().size() + " | Events " + countTrackEvents(track)), x + 30, rowY + 13, 0xBFD8E6, false);
            rowY += 24;
        }
        rowY += 8;
        context.drawText(textRenderer, Text.literal("Effects: " + state.level().effects().size()), x + 8, rowY, 0xFFD090, false);
        rowY += 14;
        context.drawText(textRenderer, Text.literal("BPM Points: " + state.level().meta().bpms().size()), x + 8, rowY, 0x90D0FF, false);
        rowY += 14;
        context.drawText(textRenderer, Text.literal("Audio: " + state.audioStatus()), x + 8, rowY, 0xC0FFC0, false);
        rowY += 14;
        if (client != null && client.world != null) {
            context.drawText(textRenderer, Text.literal("World: live Display/Text Display mode"), x + 8, rowY, 0xFFD580, false);
            rowY += 14;
        }
        context.drawText(textRenderer, Text.literal("Playhead: " + format(state.playheadBeat())), x + 8, rowY, 0xFFFFFF, false);
        rowY += 14;
        context.drawText(textRenderer, Text.literal("Shift+Wheel = Zoom"), x + 8, rowY, 0xA0A0A0, false);
    }

    private void drawCurrentFrameMap(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0x66101418);
        drawOutline(context, x, y, width, height, 0xFF506070);

        int centerX = x + width / 2;
        int centerY = y + height / 2;
        context.fill(centerX - 1, y + 6, centerX + 1, y + height - 6, 0x2255FFFF);
        context.fill(x + 6, centerY - 1, x + width - 6, centerY + 1, 0x2255FFFF);
        context.drawText(textRenderer, Text.literal("Y"), centerX + 4, y + 6, 0x80FFFFFF, false);
        context.drawText(textRenderer, Text.literal("X"), x + width - 12, centerY + 4, 0x80FFFFFF, false);

        double beat = state.playheadBeat();
        double scale = Math.max(6.0, width / 22.0);

        for (TrackData track : state.level().tracks()) {
            double xTransform = ChartMath.getTransformation(track.xTransformEvents(), beat, 0.0);
            double yTransform = ChartMath.getTransformation(track.yTransformEvents(), beat, 0.0);
            double zTransform = ChartMath.getTransformation(track.zTransformEvents(), beat, 0.0);
            double xRot = Math.toRadians(ChartMath.getTransformation(track.xRotateEvents(), beat, 0.0));
            double yRot = Math.toRadians(ChartMath.getTransformation(track.yRotateEvents(), beat, 0.0));
            double zRot = Math.toRadians(ChartMath.getTransformation(track.zRotateEvents(), beat, 0.0));
            double xScale = ChartMath.getTransformation(track.xScaleEvents(), beat, 1.0);
            double yScale = ChartMath.getTransformation(track.yScaleEvents(), beat, 1.0);
            double zScale = ChartMath.getTransformation(track.zScaleEvents(), beat, 1.0);
            double currentDistance = ChartMath.getDistance(track.speedEvents(), beat);

            int trackCenterX = centerX + (int) Math.round(xTransform * scale);
            int trackCenterY = centerY - (int) Math.round(yTransform * scale);
            context.fill(trackCenterX - 2, trackCenterY - 2, trackCenterX + 2, trackCenterY + 2, 0xFFFFFFFF);

            for (NoteData note : track.notes()) {
                double noteDistance = ChartMath.getDistance(track.speedEvents(), note.beat());
                double dis = noteDistance - currentDistance + note.pos().z() * zScale + zTransform;
                if (Math.abs(dis) > 24.0) {
                    continue;
                }

                double noteX = note.pos().x() * xScale;
                double noteY = note.pos().y() * yScale;
                double x1 = noteX * Math.cos(zRot) - noteY * Math.sin(zRot);
                double y1 = noteX * Math.sin(zRot) + noteY * Math.cos(zRot);
                double z1 = dis;
                double x2 = x1 * Math.cos(yRot) + z1 * Math.sin(yRot);
                double y2 = y1;
                double z2 = -x1 * Math.sin(yRot) + z1 * Math.cos(yRot);
                double x3 = x2;
                double y3 = y2 * Math.cos(xRot) - z2 * Math.sin(xRot);

                int drawX = centerX + (int) Math.round((x3 + xTransform) * scale);
                int drawY = centerY - (int) Math.round((y3 + yTransform) * scale);
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
        for (TrackData track : state.level().tracks()) {
            double xTransform = ChartMath.getTransformation(track.xTransformEvents(), beat, 0.0);
            double yTransform = ChartMath.getTransformation(track.yTransformEvents(), beat, 0.0);
            double zTransform = ChartMath.getTransformation(track.zTransformEvents(), beat, 0.0);
            double xRot = Math.toRadians(ChartMath.getTransformation(track.xRotateEvents(), beat, 0.0));
            double yRot = Math.toRadians(ChartMath.getTransformation(track.yRotateEvents(), beat, 0.0));
            double zRot = Math.toRadians(ChartMath.getTransformation(track.zRotateEvents(), beat, 0.0));
            double xScale = ChartMath.getTransformation(track.xScaleEvents(), beat, 1.0);
            double yScale = ChartMath.getTransformation(track.yScaleEvents(), beat, 1.0);
            double zScale = ChartMath.getTransformation(track.zScaleEvents(), beat, 1.0);
            double currentDistance = ChartMath.getDistance(track.speedEvents(), beat);
            for (NoteData note : track.notes()) {
                double noteDistance = ChartMath.getDistance(track.speedEvents(), note.beat());
                double dis = noteDistance - currentDistance + note.pos().z() * zScale + zTransform;
                if (Math.abs(dis) > 24.0) {
                    continue;
                }
                double noteX = note.pos().x() * xScale;
                double noteY = note.pos().y() * yScale;
                double x1 = noteX * Math.cos(zRot) - noteY * Math.sin(zRot);
                double y1 = noteX * Math.sin(zRot) + noteY * Math.cos(zRot);
                double z1 = dis;
                double x2 = x1 * Math.cos(yRot) + z1 * Math.sin(yRot);
                double y2 = y1;
                double z2 = -x1 * Math.sin(yRot) + z1 * Math.cos(yRot);
                double x3 = x2;
                double y3 = y2 * Math.cos(xRot) - z2 * Math.sin(xRot);
                selections.add(new MapSelection(
                        EditorSelection.Kind.NOTE,
                        track,
                        note,
                        null,
                        centerX + (int) Math.round((x3 + xTransform) * scale),
                        centerY - (int) Math.round((y3 + yTransform) * scale),
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
        context.fill(x, y, x + width, y + height, 0x55141414);
        drawOutline(context, x, y, width, height, 0xFF606060);
        context.drawText(textRenderer, Text.literal("Playback Preview"), x + 8, y + 8, 0xFFFFFF, false);

        int centerX = x + width / 2;
        int centerY = y + height / 2 + 12;
        context.fill(centerX - 1, y + 24, centerX + 1, y + height - 10, 0x30FFFFFF);
        context.fill(x + 12, centerY - 1, x + width - 12, centerY + 1, 0x20FFFFFF);

        double beat = state.playheadBeat();
        for (TrackData track : state.level().tracks()) {
            double currentDistance = ChartMath.getDistance(track.speedEvents(), beat);
            double xTransform = ChartMath.getTransformation(track.xTransformEvents(), beat, 0.0);
            double yTransform = ChartMath.getTransformation(track.yTransformEvents(), beat, 0.0);
            double zTransform = ChartMath.getTransformation(track.zTransformEvents(), beat, 0.0);
            double xRot = Math.toRadians(ChartMath.getTransformation(track.xRotateEvents(), beat, 0.0));
            double yRot = Math.toRadians(ChartMath.getTransformation(track.yRotateEvents(), beat, 0.0));
            double zRot = Math.toRadians(ChartMath.getTransformation(track.zRotateEvents(), beat, 0.0));
            double xScale = ChartMath.getTransformation(track.xScaleEvents(), beat, 1.0);
            double yScale = ChartMath.getTransformation(track.yScaleEvents(), beat, 1.0);
            double zScale = ChartMath.getTransformation(track.zScaleEvents(), beat, 1.0);

            int anchorX = centerX + (int) Math.round(xTransform * 12.0);
            int anchorY = centerY - (int) Math.round(yTransform * 12.0);
            context.fill(anchorX - 2, anchorY - 2, anchorX + 2, anchorY + 2, 0xFFFFFFFF);

            for (NoteData note : track.notes()) {
                double noteDistance = ChartMath.getDistance(track.speedEvents(), note.beat());
                double dis = noteDistance - currentDistance + note.pos().z() * zScale;
                if (dis < note.noteType().zNear() || dis > note.noteType().zFar()) {
                    continue;
                }
                double noteX = note.pos().x() * xScale;
                double noteY = note.pos().y() * yScale;

                double x1 = noteX * Math.cos(zRot) - noteY * Math.sin(zRot);
                double y1 = noteX * Math.sin(zRot) + noteY * Math.cos(zRot);
                double z1 = dis;
                double x2 = x1 * Math.cos(yRot) + z1 * Math.sin(yRot);
                double y2 = y1;
                double z2 = -x1 * Math.sin(yRot) + z1 * Math.cos(yRot);
                double x3 = x2;
                double y3 = y2 * Math.cos(xRot) - z2 * Math.sin(xRot);

                int drawX = centerX + (int) Math.round((x3 + xTransform) * 12.0);
                int drawY = centerY - (int) Math.round((y3 + yTransform) * 12.0);
                int color = noteColor(note.noteType());
                int size = note == state.selection().note() ? 8 : 6;
                context.fill(drawX - size / 2, drawY - size / 2, drawX + size / 2, drawY + size / 2, color);
            }
        }

        int textY = y + 26;
        context.drawText(textRenderer, Text.literal("Audio: " + audioSummary()), x + 8, textY, 0xC0FFC0, false);
        textY += 12;
        context.drawText(textRenderer, Text.literal("Chart ms: " + stateAudioMillis()), x + 8, textY, 0xA0A0A0, false);
        textY += 14;
        if (client != null && client.world != null) {
            context.drawText(textRenderer, Text.literal("Live world drag: LMB X/Y, RMB Z, Shift+RMB beat, middle click pick"), x + 8, textY, 0xFFD580, false);
            textY += 12;
        }
        for (String line : activeTextDisplayLines()) {
            context.drawText(textRenderer, Text.literal(line), x + 8, textY, 0xFFE7A8, false);
            textY += 10;
            if (textY > y + height - 14) {
                break;
            }
        }
    }

    private void drawTimeline(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, 0x55181818);
        drawOutline(context, x, y, width, height, 0xFF606060);

        int contentX = x + TIMELINE_LABEL_WIDTH;
        int contentWidth = Math.max(48, width - TIMELINE_LABEL_WIDTH);
        context.fill(x, y, contentX, y + height, 0xCC151C24);
        context.fill(contentX, y, contentX + 1, y + height, 0xFF53697E);

        drawWaveform(context, contentX, y, contentWidth, height);

        List<TimelineLaneLayout> layouts = buildTimelineLaneLayouts(y);
        for (int index = 0; index < layouts.size(); index++) {
            TimelineLaneLayout layout = layouts.get(index);
            int laneTop = layout.top();
            int laneBottom = laneTop + layout.height() - 1;
            int fill = index % 2 == 0 ? 0x331F1F1F : 0x33272727;
            int gutterFill = index % 2 == 0 ? 0xCC1B232D : 0xCC202A35;
            context.fill(x, laneTop, x + width, laneBottom, fill);
            context.fill(x, laneTop, contentX, laneBottom, gutterFill);
        }

        for (int beatLine = (int) Math.floor(state.visibleStartBeat()); beatLine <= Math.ceil(state.visibleEndBeat()); beatLine++) {
            int lineX = beatToScreen(contentX, contentWidth, beatLine);
            int color = beatLine % 4 == 0 ? 0x55FFFFFF : 0x22333333;
            context.fill(lineX, y, lineX + 1, y + height, color);
            context.drawText(textRenderer, Text.literal(Integer.toString(beatLine)), lineX + 2, y + 4, 0x80FFFFFF, false);
        }

        for (TimelineLaneLayout layout : layouts) {
            if (layout.top() >= y + height) {
                break;
            }
            drawTimelineLane(context, layout, x, contentX, contentWidth);
        }

        if (Double.isFinite(snapGuideBeat)) {
            int snapX = beatToScreen(contentX, contentWidth, snapGuideBeat);
            context.fill(snapX, y, snapX + 1, y + height, 0xFF7CE8FF);
        }

        if (selectionBox != null) {
            int left = Math.max(contentX, Math.min(selectionBox.startX(), selectionBox.endX()));
            int right = Math.min(contentX + contentWidth, Math.max(selectionBox.startX(), selectionBox.endX()));
            int top = Math.max(y, Math.min(selectionBox.startY(), selectionBox.endY()));
            int bottom = Math.min(y + height, Math.max(selectionBox.startY(), selectionBox.endY()));
            if (right > left && bottom > top) {
                context.fill(left, top, right, bottom, 0x224FC3F7);
                drawOutline(context, left, top, right - left, bottom - top, 0xFF7CE8FF);
            }
        }

        int playheadX = beatToScreen(contentX, contentWidth, state.playheadBeat());
        context.fill(playheadX, y, playheadX + 2, y + height, 0xFFFF4444);
        context.drawText(textRenderer, Text.literal("Timeline  |  RMB note lane = add note  |  RMB event clip = split  |  Ctrl multi-select  |  Drag axis lane to edit X/Y/Z"), x + 8, y + 6, 0xA7C8DD, false);
    }

    private void drawWaveform(DrawContext context, int x, int y, int width, int height) {
        if (!state.audioAnalysis().hasWaveform()) {
            return;
        }

        float[] waveform = state.audioAnalysis().waveform();
        long lengthMillis = state.audioAnalysis().lengthMillis();
        int centerY = y + height / 2;
        for (int index = 0; index < waveform.length; index++) {
            long timeMillis = waveform.length <= 1 ? 0L : Math.round((lengthMillis * index) / (double) (waveform.length - 1));
            double beat = state.timing().calcBeat(timeMillis);
            if (beat < state.visibleStartBeat() - 0.25 || beat > state.visibleEndBeat() + 0.25) {
                continue;
            }
            int lineX = beatToScreen(x, width, beat);
            int amplitude = Math.max(1, Math.round(waveform[index] * (height / 2.6f)));
            context.fill(lineX, centerY - amplitude, lineX + 1, centerY + amplitude, 0x2255CC88);
        }
    }

    private void drawTrackBar(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, 0x6613161D);
        drawOutline(context, x, y, width, height, 0xFF606060);

        int cursorX = x + 6;
        for (TrackData track : state.tracks()) {
            int tabWidth = 88;
            boolean selected = state.selectedTrack() == track;
            int fill = selected ? 0xCC2F5F9D : 0x66303030;
            context.fill(cursorX, y + 4, cursorX + tabWidth, y + height - 4, fill);
            context.fill(cursorX + 4, y + 6, cursorX + 16, y + 18, 0x55000000);
            context.drawText(textRenderer, Text.literal(state.isTrackExpanded(track) ? "-" : "+"), cursorX + 8, y + 9, 0xFFFFFF, false);
            context.drawText(textRenderer, Text.literal("TrackID " + track.id()), cursorX + 20, y + 9, 0xFFFFFF, false);
            cursorX += tabWidth + 4;
        }

        int addWidth = 70;
        context.fill(x + width - addWidth - 6, y + 4, x + width - 6, y + height - 4, 0x66557733);
        context.drawText(textRenderer, Text.literal("+ Track"), x + width - addWidth + 8, y + 9, 0xFFFFFF, false);
    }

    private void drawPropertyPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0x55202020);
        drawOutline(context, x, y, width, height, 0xFF606060);
        context.drawText(textRenderer, Text.literal("Right Panel"), x + 8, y + 8, 0x9FD7FF, false);
        context.drawText(textRenderer, propertyTitle(), x + 8, y + 20, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal(selectedObjectSummary()), x + 8, y + 32, 0xBFD8E6, false);
        for (LabeledField field : activeFieldGroup()) {
            if (field.widget.visible) {
                context.drawText(textRenderer, Text.literal(displayLabel(field)), field.widget.getX(), field.widget.getY() - 9, 0xA0A0A0, false);
            }
        }
        context.drawText(textRenderer, Text.literal("Wheel to scroll properties"), x + 8, height + y - 56, 0x808080, false);
    }

    private boolean handleLeftPanelClick(double mouseX, double mouseY, int x, int y) {
        int mapSize = LEFT_PANEL_WIDTH - 16;
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
            if (isInside(mouseX, mouseY, x + 6, rowY, LEFT_PANEL_WIDTH - 12, 22)) {
                state.setSelection(EditorSelection.track(track));
                return true;
            }
            rowY += 24;
        }
        return false;
    }

    private boolean handleTrackBarClick(double mouseX, double mouseY, int x, int y, int width, int height) {
        int cursorX = x + 6;
        for (TrackData track : state.tracks()) {
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
        double beat = snapBeat(screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX)));
        List<TimelineLaneLayout> layouts = buildTimelineLaneLayouts(y);
        TimelineLaneLayout layout = timelineLaneAt(layouts, mouseY);
        if (layout == null) {
            return false;
        }
        TimelineLane lane = layout.lane();
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
        int topY = 60;
        int previewX = 12 + LEFT_PANEL_WIDTH + 8;
        int rightX = width - RIGHT_PANEL_WIDTH - 12;
        int previewWidth = rightX - previewX - 8;
        int timelineY = topY + PREVIEW_HEIGHT + 8;
        int contentX = previewX + TIMELINE_LABEL_WIDTH;
        int contentWidth = Math.max(48, previewWidth - TIMELINE_LABEL_WIDTH);
        double beat = snapBeat(screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX)));
        List<TimelineLaneLayout> layouts = buildTimelineLaneLayouts(timelineY);
        TimelineLaneLayout hoveredLayout = timelineLaneAt(layouts, mouseY);
        switch (dragMode) {
            case BPM -> state.moveSelectedBpm(beat);
            case EFFECT -> state.moveSelectedEffect(beat);
            case NOTE -> {
                TimelineLane lane = hoveredLayout == null ? null : hoveredLayout.lane();
                TrackData targetTrack = lane != null && lane.track != null ? lane.track : state.selection().track();
                if (hoveredLayout != null && lane != null && lane.type == LaneType.NOTE_AXIS && lane.noteAxis != null) {
                    activeNoteAxis = lane.noteAxis;
                    double snappedBeat = applyTimelineBeatSnap(screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX)), contentX, contentWidth, selectedNoteBeats(), selectedEventClipEdges());
                    double beatDelta = snappedBeat - dragAnchorBeat;
                    double range = noteLaneRange(targetTrack, lane.noteAxis);
                    double axisValue = noteLaneScreenToValue(hoveredLayout.top(), hoveredLayout.height(), range, mouseY);
                    double axisDelta = axisValue - dragAnchorAxisValue;
                    applyDraggedNotes(beatDelta, axisDelta, targetTrack, selectedNotes.size() == 1 ? targetTrack : null);
                }
            }
            case EVENT_HANDLE -> dragSelectedEventHandle(mouseX, mouseY, contentX, contentWidth, hoveredLayout);
            case EVENT_CLIP -> dragSelectedEventClip(mouseX, contentX, contentWidth);
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

    private void newProject() {
        Path path = parsePath();
        if (path != null) {
            state.newProject(path);
            pathField.setText(path.toString());
        }
    }

    private void loadProject() {
        Path path = parsePath();
        if (path != null) {
            state.loadProject(path);
            pathField.setText(path.toString());
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
        for (TrackData track : state.tracks()) {
            lanes.add(new TimelineLane(LaneType.TRACK_HEADER, track, null, null));
            if (!state.isTrackExpanded(track)) {
                continue;
            }
            for (NoteAxis axis : NoteAxis.values()) {
                lanes.add(new TimelineLane(LaneType.NOTE_AXIS, track, null, axis));
            }
            for (EventLaneType eventType : EventLaneType.values()) {
                if (!eventsForLane(track, eventType).isEmpty()) {
                    lanes.add(new TimelineLane(LaneType.EVENTS, track, eventType, null));
                }
            }
        }
        return lanes;
    }

    private List<TimelineLaneLayout> buildTimelineLaneLayouts(int timelineY) {
        List<TimelineLaneLayout> layouts = new ArrayList<>();
        int laneTop = timelineY;
        for (TimelineLane lane : buildTimelineLanes()) {
            int laneHeight = laneHeight(lane);
            layouts.add(new TimelineLaneLayout(lane, laneTop, laneHeight));
            laneTop += laneHeight;
        }
        return layouts;
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
            case BPM, EFFECTS -> BASE_ROW_HEIGHT;
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
            case EVENTS -> drawTrackEventLane(context, lane.track, lane.eventType, x, contentX, layout.top(), layout.height(), contentWidth);
        }
    }

    private void drawBpmLane(DrawContext context, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        context.drawText(textRenderer, Text.literal("BPM"), labelX + 8, rowTop + laneHeight / 2 - 4, 0x90D0FF, false);
        if (state.estimatedBpmReference() > 0.0) {
            context.drawText(textRenderer, Text.literal("Ref " + format(state.estimatedBpmReference())), labelX + 42, rowTop + laneHeight / 2 - 4, 0x66CCFF, false);
        }
        for (BpmPoint bpm : state.level().meta().bpms()) {
            int markerX = beatToScreen(contentX, width, bpm.beat());
            int markerY = rowTop + laneHeight / 2;
            context.fill(markerX - 3, markerY - 3, markerX + 3, markerY + 3, bpm == state.selection().bpm() ? 0xFF88CCFF : 0xFF4477AA);
            context.drawText(textRenderer, Text.literal(format(bpm.bpm())), markerX + 4, markerY - 4, 0xFF88CCFF, false);
        }
    }

    private void drawEffectLane(DrawContext context, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        context.drawText(textRenderer, Text.literal("Effects"), labelX + 8, rowTop + laneHeight / 2 - 4, 0xFFD090, false);
        for (EffectData effect : state.level().effects()) {
            int markerX = beatToScreen(contentX, width, effect.beat());
            context.fill(markerX - 2, rowTop + 4, markerX + 2, rowTop + laneHeight - 4, effect == state.selection().effect() ? 0xFFFFD77A : 0xFFAA7722);
            context.drawText(textRenderer, Text.literal(shortEffectLabel(effect.effectType())), markerX + 4, rowTop + laneHeight / 2 - 4, 0xFFEBC48C, false);
        }
    }

    private void drawTrackHeaderLane(DrawContext context, TrackData track, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null) {
            return;
        }
        int accent = state.selectedTrack() == track ? 0xAA3A6EA5 : 0x66405060;
        context.fill(labelX, rowTop, contentX + width, rowTop + laneHeight - 1, accent);
        context.fill(labelX + 4, rowTop + 5, labelX + 18, rowTop + 19, 0x55000000);
        context.drawText(textRenderer, Text.literal(state.isTrackExpanded(track) ? "-" : "+"), labelX + 9, rowTop + 8, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("TrackID " + track.id()), labelX + 24, rowTop + 5, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Notes " + track.notes().size() + "  |  Event Lanes " + countVisibleEventLanes(track)), contentX + 8, rowTop + 5, 0xD7E8F4, false);
    }

    private void drawTrackNoteAxisLane(DrawContext context, TrackData track, NoteAxis noteAxis, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null || noteAxis == null) {
            return;
        }
        double range = noteLaneRange(track, noteAxis);
        int centerY = rowTop + laneHeight / 2;
        boolean activeAxis = activeNoteAxis == noteAxis;
        context.drawText(textRenderer, Text.literal(noteAxis.label), labelX + 8, rowTop + 8, noteAxis.color, false);
        context.drawText(textRenderer, Text.literal("center=0  +/-" + format(range) + (activeAxis ? "  active" : "")), labelX + 8, rowTop + 20, 0x81B8CF, false);
        context.fill(contentX, centerY, contentX + width, centerY + 1, 0x60A5D6F6);
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
            int halfWidth = selected ? 8 : 6;
            int halfHeight = selected ? 5 : 4;
            context.fill(noteX - halfWidth, noteY - halfHeight, noteX + halfWidth, noteY + halfHeight, color);
            context.drawText(textRenderer, Text.literal(shortNoteLabel(note.noteType()) + " " + noteAxis.name().toLowerCase(Locale.ROOT) + format(axisValue)), noteX + halfWidth + 4, noteY - 4, color, false);
        }
    }

    private void drawTrackEventLane(DrawContext context, TrackData track, EventLaneType eventType, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null || eventType == null) {
            return;
        }
        context.drawText(textRenderer, Text.literal(eventType.label), labelX + 8, rowTop + laneHeight / 2 - 4, eventType.color, false);
        List<NumEventData> events = eventsForLane(track, eventType);
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
            String label = shortEasingLabel(event.easingType().name()) + " " + format(event.startValue()) + "->" + format(event.endValue());
            context.drawText(textRenderer, Text.literal(label), startX + 6, rowTop + laneHeight / 2 - 4, clipSelected ? 0xFFFFFFFF : eventType.color, false);
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
        double beat = applyTimelineBeatSnap(screenToBeat(timelineX, timelineWidth, mouseX), timelineX, timelineWidth, selectedNoteBeats(), selectedEventClipEdges());
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
            case Z -> note.pos().set(note.pos().x(), note.pos().y(), value);
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

    private void dragSelectedEventClip(double mouseX, int timelineX, int timelineWidth) {
        if (eventClipDragSnapshots.isEmpty()) {
            return;
        }
        double snappedBeat = applyTimelineBeatSnap(screenToBeat(timelineX, timelineWidth, mouseX), timelineX, timelineWidth, selectedNoteBeats(), selectedEventClipEdges());
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

    private double applyTimelineBeatSnap(double rawBeat, int timelineX, int timelineWidth, Set<Double> excludedNoteBeats, Set<Double> excludedClipBeats) {
        if (isAltDown()) {
            snapGuideBeat = Double.NaN;
            return rawBeat;
        }
        double bestBeat = snapBeat(rawBeat);
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
        int topY = 60;
        int previewX = 12 + LEFT_PANEL_WIDTH + 8;
        int rightX = width - RIGHT_PANEL_WIDTH - 12;
        int previewWidth = rightX - previewX - 8;
        int timelineY = topY + PREVIEW_HEIGHT + 8;
        int contentX = previewX + TIMELINE_LABEL_WIDTH;
        int contentWidth = Math.max(48, previewWidth - TIMELINE_LABEL_WIDTH);
        List<TimelineLaneLayout> layouts = buildTimelineLaneLayouts(timelineY);
        int left = Math.min(selectionBox.startX(), selectionBox.endX());
        int right = Math.max(selectionBox.startX(), selectionBox.endX());
        int top = Math.min(selectionBox.startY(), selectionBox.endY());
        int bottom = Math.max(selectionBox.startY(), selectionBox.endY());
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
        if (isAltDown()) {
            return beat;
        }
        return Math.round(beat * 4.0) / 4.0;
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

    private void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
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

    private record LabeledField(String label, TextFieldWidget widget) {
    }

    private record MapSelection(EditorSelection.Kind kind, TrackData track, NoteData note, EffectData effect, int x, int y, double distance) {
    }

    private record SelectionBox(int startX, int startY, int endX, int endY) {
    }

    private record SelectedNote(TrackData track, NoteData note) {
    }

    private record TimelineLane(LaneType type, TrackData track, EventLaneType eventType, NoteAxis noteAxis) {
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

    private enum DragMode {
        NONE,
        NOTE,
        EFFECT,
        BPM,
        EVENT_HANDLE,
        EVENT_CLIP,
        BOX_SELECT,
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

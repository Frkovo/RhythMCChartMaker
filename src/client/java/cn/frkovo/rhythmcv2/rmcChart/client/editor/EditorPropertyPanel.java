package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.*;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import static cn.frkovo.rhythmcv2.rmcChart.client.editor.EditorUtils.*;
import com.google.gson.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.*;

final class EditorPropertyPanel {
    private static final double NOTE_BEAT_EPSILON = 1.0E-5;
    private static final double NOTE_LANE_DEFAULT_RANGE = 2.0;

    private final ChartEditorScreen screen;
    final EditorTrackEventPanel trackEvents;

    final List<LabeledField> songFields = new ArrayList<>();
    final List<LabeledField> metaFields = new ArrayList<>();
    final List<LabeledField> trackFields = new ArrayList<>();
    final List<LabeledField> noteFields = new ArrayList<>();
    final List<LabeledField> noteTransformFields = new ArrayList<>();
    final List<LabeledField> effectFields = new ArrayList<>();
    final List<LabeledField> bpmFields = new ArrayList<>();

    ButtonWidget applyButton;
    ButtonWidget resetButton;
    ButtonWidget quickNoteTypeButton;
    ButtonWidget quickNoteTrackPrevButton;
    ButtonWidget quickNoteTrackNextButton;
    ButtonWidget quickEffectTypeButton;

    int propertyScroll;
    final Set<String> collapsedPropertySections = new LinkedHashSet<>();
    final List<PropertySectionHeader> propertySectionHeaders = new ArrayList<>();
    final PropertyLayout propertyLayout = new PropertyLayout();

    EditorPropertyPanel(ChartEditorScreen screen) {
        this.screen = screen;
        this.trackEvents = new EditorTrackEventPanel(screen);
    }

    void clearFields() {
        songFields.clear();
        metaFields.clear();
        trackFields.clear();
        noteFields.clear();
        noteTransformFields.clear();
        effectFields.clear();
        bpmFields.clear();
        trackEvents.clear();
        propertySectionHeaders.clear();
        propertyScroll = 0;
    }

    void onSelectionChanged() {
        propertyScroll = 0;
        trackEvents.onSelectionChanged();
        populateFieldsFromSelection();
        layoutPropertyFields();
    }

    void drawPropertyPanel(DrawContext context, int x, int y, int width, int height) {
        screen.chrome.drawPropertyPanel(context, x, y, width, height);
    }

    void createPropertyFields() {
        EditorLayout layout = screen.editorLayout();
        int rightX = layout.rightX() + 10;
        int fieldWidth = screen.rightPanelInnerWidth();

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
        noteFields.add(field(rightX, fieldWidth, "Hold Group"));
        noteFields.add(field(rightX, fieldWidth, "Hold Length"));
        noteTransformFields.clear();
        noteTransformFields.add(field(rightX, fieldWidth, "Pos X"));
        noteTransformFields.add(field(rightX, fieldWidth, "Pos Y"));
        noteTransformFields.add(field(rightX, fieldWidth, "Pos Z"));
        noteTransformFields.add(field(rightX, fieldWidth, "Scale X"));
        noteTransformFields.add(field(rightX, fieldWidth, "Scale Y"));
        noteTransformFields.add(field(rightX, fieldWidth, "Scale Z"));
        noteTransformFields.add(field(rightX, fieldWidth, "Rot X"));
        noteTransformFields.add(field(rightX, fieldWidth, "Rot Y"));
        noteTransformFields.add(field(rightX, fieldWidth, "Rot Z"));

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
        effectFields.add(field(rightX, fieldWidth, "Duration(beats)"));

        applyInputHints();

        bpmFields.add(field(rightX, fieldWidth, "Beat"));
        bpmFields.add(field(rightX, fieldWidth, "BPM"));

        applyButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> applyFieldsToSelection())
                .dimensions(rightX, screen.publicHeight() - 42, 86, 18).build());
        resetButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> populateFieldsFromSelection())
                .dimensions(rightX + 92, screen.publicHeight() - 42, 86, 18).build());
        quickNoteTypeButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Type"), b -> screen.actions.cycleSelectedNoteType())
                .dimensions(rightX + 184, screen.publicHeight() - 42, 54, 18).build());
        quickNoteTrackPrevButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Track -"), b -> screen.actions.moveSelectedNoteTrack(-1))
                .dimensions(rightX + 242, screen.publicHeight() - 42, 62, 18).build());
        quickNoteTrackNextButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Track +"), b -> screen.actions.moveSelectedNoteTrack(1))
                .dimensions(rightX + 308, screen.publicHeight() - 42, 62, 18).build());
        quickEffectTypeButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Fx Type"), b -> screen.actions.cycleSelectedEffectType())
                .dimensions(rightX + 184, screen.publicHeight() - 42, 78, 18).build());
        trackEvents.createFields(rightX, fieldWidth);
    }

    LabeledField field(int x, int width, String label) {
        TextFieldWidget widget = (TextFieldWidget) screen.publicAddDrawableChild(new TextFieldWidget(screen.getTextRenderer(), x, 0, width, 18, Text.literal(label)));
        widget.setMaxLength(4096);
        return new LabeledField(label, widget);
    }

    void applyInputHints() {
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
        setFieldHint(noteFields, 3, "Hold link id");
        setFieldHint(noteFields, 4, "Hold length in beats, snapped to track grid");

        setFieldHint(noteTransformFields, 0, "Position X");
        setFieldHint(noteTransformFields, 1, "Position Y");
        setFieldHint(noteTransformFields, 2, "Position Z");
        setFieldHint(noteTransformFields, 3, "Scale X");
        setFieldHint(noteTransformFields, 4, "Scale Y");
        setFieldHint(noteTransformFields, 5, "Scale Z");
        setFieldHint(noteTransformFields, 6, "Rotation X");
        setFieldHint(noteTransformFields, 7, "Rotation Y");
        setFieldHint(noteTransformFields, 8, "Rotation Z");

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

    void assignFieldSections() {
        setFieldSection(songFields, 0, 8, "Identity");
        setFieldSection(songFields, 9, 11, "Lists");
        setFieldSection(songFields, 12, 15, "Unlocks");
        setFieldSection(metaFields, 0, 3, "Timing");
        setFieldSection(metaFields, 4, 5, "Credits");
        setFieldSection(trackFields, 0, 1, "Identity");
        setFieldSection(noteFields, 0, 2, "Timing");
        setFieldSection(noteFields, 3, 4, "Links");

        setFieldSection(effectFields, 0, 1, "Timing");
        setFieldSection(effectFields, 2, 5, "Identity");
        setFieldSection(effectFields, 6, 6, "Position");
        setFieldSection(effectFields, 7, 7, "Scale");
        setFieldSection(effectFields, 8, 8, "Rotation");
        setFieldSection(effectFields, 9, 10, "Target");
        setFieldSection(effectFields, 11, 11, "Advanced");
        setFieldSection(effectFields, 12, 12, "Timing");
        setFieldSection(bpmFields, 0, 1, "Timing");
    }

    void setFieldSection(List<LabeledField> fields, int start, int end, String section) {
        for (int index = start; index <= end && index < fields.size(); index++) {
            fields.get(index).section = section;
        }
    }

    void setFieldHint(List<LabeledField> fields, int index, String hint) {
        if (index >= 0 && index < fields.size()) {
            fields.get(index).hint = hint;
        }
    }

    void layoutPropertyFields() {
        EditorLayout layout = screen.editorLayout();
        int panelX = layout.rightX() + 10;
        int viewportTop = layout.topY() + 48;
        int viewportBottom = layout.topY() + layout.panelHeight() - 50;
        int currentY = viewportTop + propertyScroll;
        String lastSection = null;
        propertySectionHeaders.clear();
        for (LabeledField field : allFields()) {
            boolean visible = activeFieldGroup().contains(field);
            field.widget.visible = false;
            field.widget.active = false;
            field.sectionY = -1;
            if (visible) {
                if (field.section != null && !field.section.equals(lastSection)) {
                    currentY += 10;
                    field.sectionY = currentY;
                    propertySectionHeaders.add(new PropertySectionHeader(propertySectionKey(screen.state.selection().kind(), field.section), field.section, panelX, currentY, screen.rightPanelInnerWidth(), 12));
                    currentY += 12;
                    lastSection = field.section;
                }
                if (isSectionCollapsed(propertySectionKey(screen.state.selection().kind(), field.section))) {
                    continue;
                }
                field.widget.setPosition(panelX, currentY + 20);
                boolean insideViewport = field.widget.getY() >= viewportTop && field.widget.getY() + 18 <= viewportBottom;
                field.widget.visible = insideViewport;
                field.widget.active = insideViewport;
                currentY += 42;
            }
        }
        currentY = trackEvents.layout(panelX, currentY);
        if (screen.state.selection().kind() == EditorSelection.Kind.NOTE && screen.state.selection().note() != null) {
            propertyLayout.setTransformCard(panelX, currentY + 8, screen.rightPanelInnerWidth());
            currentY += 110;
        } else {
            propertyLayout.clearTransformCard();
        }
        int footerY = layout.topY() + layout.panelHeight() - 34;
        int footerButtonWidth = (screen.rightPanelInnerWidth()) / 2;
        applyButton.setPosition(panelX, footerY);
        applyButton.setWidth(footerButtonWidth - 3);
        resetButton.setPosition(panelX + footerButtonWidth + 3, footerY);
        resetButton.setWidth(footerButtonWidth - 3);
        layoutQuickInspectorButtons(panelX, footerY - 22);
    }

    void layoutQuickInspectorButtons(int panelX, int buttonY) {
        int gap = 6;
        int innerWidth = screen.rightPanelInnerWidth();
        int typeWidth = Math.min(62, Math.max(46, innerWidth / 4));
        int trackButtonWidth = Math.max(46, (innerWidth - typeWidth - gap * 2) / 2);
        quickNoteTypeButton.setPosition(panelX, buttonY);
        quickNoteTypeButton.setWidth(typeWidth);
        quickNoteTrackPrevButton.setPosition(panelX + typeWidth + gap, buttonY);
        quickNoteTrackPrevButton.setWidth(trackButtonWidth);
        quickNoteTrackNextButton.setPosition(panelX + typeWidth + gap + trackButtonWidth + gap, buttonY);
        quickNoteTrackNextButton.setWidth(Math.max(46, innerWidth - typeWidth - trackButtonWidth - gap * 2));
        quickEffectTypeButton.setPosition(panelX, buttonY);
        quickEffectTypeButton.setWidth(innerWidth);

        boolean noteSelected = screen.state.selection().kind() == EditorSelection.Kind.NOTE && screen.state.selection().note() != null;
        boolean effectSelected = screen.state.selection().kind() == EditorSelection.Kind.EFFECT && screen.state.selection().effect() != null;
        setQuickButtonVisible(quickNoteTypeButton, noteSelected);
        setQuickButtonVisible(quickNoteTrackPrevButton, noteSelected);
        setQuickButtonVisible(quickNoteTrackNextButton, noteSelected);
        setQuickButtonVisible(quickEffectTypeButton, effectSelected);

        if (noteSelected) {
            quickNoteTypeButton.setMessage(Text.literal(EditorUtils.shortNoteLabel(screen.state.selection().note().noteType())));
        }
        if (effectSelected) {
            quickEffectTypeButton.setMessage(Text.literal(shortEffectLabel(screen.state.selection().effect().effectType())));
        }
    }

    void setQuickButtonVisible(ButtonWidget button, boolean visible) {
        button.visible = visible;
        button.active = visible;
    }

    void populateFieldsFromSelection() {
        SongManifestData manifest = screen.state.project().manifest();
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

        MetaData meta = screen.state.level().meta();
        set(metaFields, 0, Integer.toString(meta.uid()));
        set(metaFields, 1, meta.initialArena());
        set(metaFields, 2, Long.toString(meta.offset()));
        set(metaFields, 3, format(meta.level()));
        set(metaFields, 4, joinCsv(meta.charters()));
        set(metaFields, 5, joinCsv(meta.comments()));

        if (screen.state.selection().kind() == EditorSelection.Kind.TRACK && screen.state.selection().track() != null) {
            TrackData track = screen.state.selection().track();
            set(trackFields, 0, Integer.toString(track.id()));
            set(trackFields, 1, Integer.toString(track.beatDivision()));
            trackEvents.syncTrackEventEditors(track);
        } else {
            trackEvents.hideTrackEventEditors();
        }

        if (screen.state.selection().note() != null) {
            NoteData note = screen.state.selection().note();
            set(noteFields, 0, format(note.beat()));
            set(noteFields, 1, note.noteType().name());
            set(noteFields, 2, Integer.toString(screen.state.selection().track().id()));
            set(noteFields, 3, Integer.toString(note.holdGroup()));
            set(noteFields, 4, format(note.holdLengthBeats()));
            set(noteTransformFields, 0, format(note.pos().x()));
            set(noteTransformFields, 1, format(note.pos().y()));
            set(noteTransformFields, 2, format(note.pos().z()));
            set(noteTransformFields, 3, format(note.scale().x()));
            set(noteTransformFields, 4, format(note.scale().y()));
            set(noteTransformFields, 5, format(note.scale().z()));
            set(noteTransformFields, 6, format(note.rotation().x()));
            set(noteTransformFields, 7, format(note.rotation().y()));
            set(noteTransformFields, 8, format(note.rotation().z()));
        }

        if (screen.state.selection().effect() != null) {
            EffectData effect = screen.state.selection().effect();
            JsonObject properties = effect.properties() == null ? new JsonObject() : effect.properties();
            set(effectFields, 0, format(effect.beat()));
            set(effectFields, 1, effect.effectType().name());
            set(effectFields, 2, propertyString(properties, "id", propertyString(properties, "textId", "")));
            set(effectFields, 3, firstProperty(properties, "text", "content", "title", "message", "value"));
            set(effectFields, 4, propertyString(properties, "color", propertyString(properties, "glowColor", "")));
            set(effectFields, 5, firstProperty(properties, "trackId", "track", ""));
            set(effectFields, 6, serializeTriple(getDouble(properties, "x", 0.0), getDouble(properties, "y", 0.0), getDouble(properties, "z", 0.0)));
            set(effectFields, 7, serializeTriple(getDouble(properties, "scaleX", 1.0), getDouble(properties, "scaleY", 1.0), getDouble(properties, "scaleZ", 1.0)));
            set(effectFields, 8, serializeTriple(getDouble(properties, "rotationX", 0.0), getDouble(properties, "rotationY", 0.0), getDouble(properties, "rotationZ", 0.0)));
            set(effectFields, 9, firstProperty(properties, "arena", "target", "weather", "time"));
            set(effectFields, 10, firstProperty(properties, "mode", "state", "type", "action"));
            set(effectFields, 11, serializeExtraProperties(filterExtraEffectProperties(properties)));
            long durationMs = properties.has("duration") ? properties.get("duration").getAsLong() : 0L;
            if (durationMs > 0) {
                double endBeat = screen.state.timing().calcBeat(screen.state.timing().beatToMillis(effect.beat()) + durationMs);
                set(effectFields, 12, format(endBeat - effect.beat()));
            } else {
                set(effectFields, 12, "");
            }
        }

        if (screen.state.selection().bpm() != null) {
            BpmPoint bpm = screen.state.selection().bpm();
            set(bpmFields, 0, format(bpm.beat()));
            set(bpmFields, 1, format(bpm.bpm()));
        }
    }

    boolean applyFieldsToSelection() {
        try {
            switch (screen.state.selection().kind()) {
                case SONG -> applySongFields();
                case META -> applyMetaFields();
                case TRACK -> applyTrackFields();
                case NOTE -> applyNoteFields();
                case EFFECT -> applyEffectFields();
                case BPM -> applyBpmFields();
            }
            screen.state.sortCurrentLevel();
            screen.state.markDirty();
            screen.state.setStatus("Applied changes");
            populateFieldsFromSelection();
            return true;
        } catch (RuntimeException exception) {
            screen.state.setStatus("Apply failed: " + exception.getMessage());
            return false;
        }
    }

    private void applySongFields() {
        SongManifestData manifest = screen.state.project().manifest();
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
        MetaData meta = screen.state.level().meta();
        meta.setUid(parseInt(get(metaFields, 0)));
        meta.setInitialArena(get(metaFields, 1));
        meta.setOffset(parseLong(get(metaFields, 2)));
        meta.setLevel(parseDouble(get(metaFields, 3)));
        replaceStrings(meta.charters(), splitCsv(get(metaFields, 4)));
        replaceStrings(meta.comments(), splitCsv(get(metaFields, 5)));
        screen.state.seekToBeat(screen.state.playheadBeat());
    }

    private void applyTrackFields() {
        TrackData track = screen.state.selection().track();
        track.setId(parseInt(get(trackFields, 0)));
        track.setBeatDivision(parseInt(get(trackFields, 1)));
        trackEvents.applyTrackEventEditors(track);
    }

    private void applyNoteFields() {
        NoteData note = screen.state.selection().note();
        TrackData currentTrack = screen.state.selection().track();
        note.setBeat(parseDouble(get(noteFields, 0)));
        note.setNoteType(NoteType.valueOf(get(noteFields, 1).trim().toUpperCase(Locale.ROOT)));
        double posX = parseDouble(get(noteTransformFields, 0));
        double posY = parseDouble(get(noteTransformFields, 1));
        double posZ = parseDouble(get(noteTransformFields, 2));
        if (note.noteType() == NoteType.HOLD) {
            posZ = -1.0;
        }
        note.pos().set(posX, posY, posZ);
        note.scale().set(parseDouble(get(noteTransformFields, 3)), parseDouble(get(noteTransformFields, 4)), parseDouble(get(noteTransformFields, 5)));
        note.rotation().set(parseDouble(get(noteTransformFields, 6)), parseDouble(get(noteTransformFields, 7)), parseDouble(get(noteTransformFields, 8)));
        note.setHoldGroup(parseInt(get(noteFields, 3)));
        if (note.noteType() == NoteType.HOLD) {
            note.setHoldLengthBeats(screen.snapHoldLength(parseDouble(get(noteFields, 4)), screen.state.selection().track()));
        } else {
            note.setHoldLengthBeats(0.0);
        }
        int targetTrackId = parseInt(get(noteFields, 2));
        TrackData newTrack = screen.state.level().tracks().stream().filter(t -> t.id() == targetTrackId).findFirst().orElse(currentTrack);
        screen.state.moveSelectedNote(note.beat(), newTrack);
    }

    private void applyEffectFields() {
        EffectData effect = screen.state.selection().effect();
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
                case WEATHER, TIME -> properties.addProperty("target", targetValue);
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

        String durationStr = get(effectFields, 12);
        if (!durationStr.isBlank()) {
            double durationBeats = parseDouble(durationStr);
            if (durationBeats > 0) {
                double startMs = screen.state.timing().beatToMillis(effect.beat());
                double endMs = screen.state.timing().beatToMillis(effect.beat() + durationBeats);
                properties.addProperty("duration", (long) (endMs - startMs));
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
        BpmPoint bpm = screen.state.selection().bpm();
        bpm.setBeat(parseDouble(get(bpmFields, 0)));
        bpm.setBpm(parseDouble(get(bpmFields, 1)));
        screen.state.seekToBeat(screen.state.playheadBeat());
    }

    Text propertyTitle() {
        return switch (screen.state.selection().kind()) {
            case SONG -> Text.literal("Song Manifest Properties");
            case META -> Text.literal(screen.state.activeDifficulty().displayName() + " Meta Properties");
            case TRACK -> Text.literal("Track Properties");
            case NOTE -> Text.literal("Note Properties");
            case EFFECT -> Text.literal("Effect Properties");
            case BPM -> Text.literal("BPM Point Properties");
        };
    }

    String selectedObjectSummary() {
        return switch (screen.state.selection().kind()) {
            case SONG -> "Current object: song manifest";
            case META -> "Current object: level meta for " + screen.state.activeDifficulty().displayName();
            case TRACK -> screen.state.selection().track() == null ? "Current object: track" : "Current object: TrackID " + screen.state.selection().track().id();
            case NOTE -> screen.state.selection().note() == null ? "Current object: note" : "Current object: " + screen.state.selection().note().noteType() + " @ beat " + format(screen.state.selection().note().beat());
            case EFFECT -> screen.state.selection().effect() == null ? "Current object: effect" : "Current object: " + screen.state.selection().effect().effectType().name() + " @ beat " + format(screen.state.selection().effect().beat());
            case BPM -> screen.state.selection().bpm() == null ? "Current object: BPM point" : "Current object: BPM " + format(screen.state.selection().bpm().bpm()) + " @ beat " + format(screen.state.selection().bpm().beat());
        };
    }

    String displayLabel(LabeledField field) {
        if (!effectFields.contains(field) || screen.state.selection().kind() != EditorSelection.Kind.EFFECT || screen.state.selection().effect() == null) {
            return field.label;
        }
        int index = effectFields.indexOf(field);
        EffectType type = screen.state.selection().effect().effectType();
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
            case 12 -> "Duration (beats)";
            default -> field.label;
        };
    }

    List<LabeledField> activeFieldGroup() {
        return switch (screen.state.selection().kind()) {
            case SONG -> songFields;
            case META -> metaFields;
            case TRACK -> trackFields;
            case NOTE -> noteFields;
            case EFFECT -> activeEffectFields();
            case BPM -> bpmFields;
        };
    }

    List<LabeledField> activeEffectFields() {
        EffectData effect = screen.state.selection().effect();
        if (effect == null) {
            return effectFields;
        }
        return switch (effect.effectType()) {
            case TEXT_DISPLAY, TEXT_DISPLAY_EFFECT -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(2), effectFields.get(3), effectFields.get(4), effectFields.get(5), effectFields.get(6), effectFields.get(7), effectFields.get(8), effectFields.get(10), effectFields.get(12));
            case TEXT_DISPLAY_REMOVE -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(2));
            case TEXT_DISPLAY_SYNC_TRACK -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(2), effectFields.get(5), effectFields.get(10), effectFields.get(12));
            case TEXT_DISPLAY_DESYNC_TRACK -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(2), effectFields.get(5), effectFields.get(10));
            case TITLE -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(4), effectFields.get(10));
            case MESSAGE -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(4), effectFields.get(10));
            case GLOW_COLOR -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(4), effectFields.get(10));
            case HIDE_NOTES -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(5), effectFields.get(10));
            case ARENA -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(9), effectFields.get(10));
            case TIME -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(9), effectFields.get(10), effectFields.get(12));
            case WEATHER -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(9), effectFields.get(10));
            case FIREWORK -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(4), effectFields.get(6), effectFields.get(7), effectFields.get(8), effectFields.get(9), effectFields.get(10), effectFields.get(11));
            case HOLOGRAM -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(4), effectFields.get(6), effectFields.get(7), effectFields.get(8), effectFields.get(9), effectFields.get(10), effectFields.get(11), effectFields.get(12));
            case REMOVE_HOLOGRAM -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(4), effectFields.get(6), effectFields.get(7), effectFields.get(8), effectFields.get(9), effectFields.get(10), effectFields.get(11));
            case EFFECT -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(4), effectFields.get(6), effectFields.get(7), effectFields.get(8), effectFields.get(9), effectFields.get(10), effectFields.get(11), effectFields.get(12));
            case CLEAR_EFFECT -> List.of(effectFields.get(0), effectFields.get(1), effectFields.get(3), effectFields.get(4), effectFields.get(6), effectFields.get(7), effectFields.get(8), effectFields.get(9), effectFields.get(10), effectFields.get(11));
        };
    }

    List<LabeledField> allFields() {
        List<LabeledField> fields = new ArrayList<>();
        fields.addAll(songFields);
        fields.addAll(metaFields);
        fields.addAll(trackFields);
        fields.addAll(noteFields);
        fields.addAll(noteTransformFields);
        fields.addAll(effectFields);
        fields.addAll(bpmFields);
        return fields;
    }

    List<String> activeTextDisplayLines() {
        Map<String, String> displays = new LinkedHashMap<>();
        for (EffectData effect : screen.state.triggeredEffects()) {
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

    JsonObject filterExtraEffectProperties(JsonObject properties) {
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

    boolean isKnownEffectProperty(String key) {
        return switch (key) {
            case "id", "textId", "text", "content", "title", "message", "value", "color", "glowColor",
                    "trackId", "track", "x", "y", "z", "scaleX", "scaleY", "scaleZ",
                    "rotationX", "rotationY", "rotationZ", "arena", "target", "weather", "time",
                    "mode", "state", "type", "action", "duration" -> true;
            default -> false;
        };
    }

    boolean handlePropertyPanelClick(Click click, double mouseX, double mouseY) {
        for (PropertySectionHeader header : propertySectionHeaders) {
            if (EditorUtils.isInside(mouseX, mouseY, header.x(), header.y(), header.width(), header.height())) {
                if (!collapsedPropertySections.add(header.key())) {
                    collapsedPropertySections.remove(header.key());
                }
                layoutPropertyFields();
                return true;
            }
        }
        return trackEvents.handleClick(click, mouseX, mouseY);
    }

    String propertySectionKey(EditorSelection.Kind kind, String section) {
        return kind.name() + ":" + section;
    }

    boolean isSectionCollapsed(String key) {
        return collapsedPropertySections.contains(key);
    }

    void set(List<LabeledField> fields, int index, String value) {
        fields.get(index).widget.setText(value);
    }

    String get(List<LabeledField> fields, int index) {
        return fields.get(index).widget.getText();
    }

    String format(double value) {
        return EditorUtils.format(value);
    }

    double getDouble(JsonObject properties, String key, double fallback) {
        return EditorUtils.getDouble(properties, key, fallback);
    }

    String shortEffectLabel(EffectType effectType) {
        return EditorUtils.shortEffectLabel(effectType);
    }

    String audioSummary() {
        if (screen.state.audioPath() == null) return screen.state.audioStatus();
        return screen.state.audioPath().getFileName() + " (" + formatMillis(screen.state.audioLengthMillis()) + ")";
    }

    String stateAudioMillis() {
        return formatMillis(Math.max(0L, screen.state.timing().beatToMillis(screen.state.playheadBeat())));
    }

    void drawNoteTransformCard(DrawContext context) {
        if (screen.state.selection().kind() != EditorSelection.Kind.NOTE || screen.state.selection().note() == null || noteTransformFields.isEmpty()) {
            return;
        }
        int panelX = propertyLayout.lastPanelX();
        int y = propertyLayout.lastTransformCardY();
        int width = propertyLayout.lastPanelInnerWidth();
        if (y < 0 || width <= 0) {
            return;
        }
        int headerY = y;
        context.fill(panelX, headerY, panelX + width, headerY + 16, 0xC8161C23);
        context.drawText(screen.getTextRenderer(), Text.literal("Transform"), panelX + 8, headerY + 4, 0x8FD6FF, false);
        context.fill(panelX + 64, headerY + 9, panelX + width - 8, headerY + 10, 0x335F86A1);
        y += 22;
        int colWidth = (width - 24) / 3;
        String[] labels = {"X", "Y", "Z"};
        for (int row = 0; row < 3; row++) {
            String rowLabel = switch (row) {
                case 0 -> "Position";
                case 1 -> "Scale";
                case 2 -> "Rotation";
                default -> "";
            };
            context.drawText(screen.getTextRenderer(), Text.literal(rowLabel), panelX + 8, y + 5, screen.UI_MUTED, false);
            for (int col = 0; col < 3; col++) {
                int fieldIndex = row * 3 + col;
                if (fieldIndex >= noteTransformFields.size()) {
                    continue;
                }
                LabeledField field = noteTransformFields.get(fieldIndex);
                int fieldX = panelX + 64 + col * (colWidth + 4);
                field.widget.setPosition(fieldX, y);
                field.widget.setWidth(colWidth);
                field.widget.visible = true;
                field.widget.active = true;
                context.drawText(screen.getTextRenderer(), Text.literal(labels[col]), fieldX, y - 10, screen.UI_DIM, false);
            }
            y += 28;
        }
        propertyLayout.setLastTransformCardBottom(y);
    }
}

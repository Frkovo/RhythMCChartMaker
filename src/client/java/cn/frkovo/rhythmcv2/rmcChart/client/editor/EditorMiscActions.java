package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartCompatibilityValidator;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.EasingFunctions;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.*;
import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectBrowserScreen;
import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectHubScreen;
import cn.frkovo.rhythmcv2.rmcChart.client.wizard.NewSongWizardScreen;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.*;

final class EditorMiscActions {
    private static final double NOTE_BEAT_EPSILON = 1.0E-5;
    private static final int GROUP_NAME_POPUP_WIDTH = 220;
    private static final int GROUP_NAME_POPUP_HEIGHT = 72;

    private final ChartEditorScreen screen;

    EditorMiscActions(ChartEditorScreen screen) {
        this.screen = screen;
    }

    void openGroupNamePopup() {
        screen.groupNamePopupOpen = true;
        int x = screen.groupNamePopupX();
        int y = screen.groupNamePopupY();
        screen.groupNameField = (TextFieldWidget) screen.publicAddDrawableChild(new TextFieldWidget(screen.getTextRenderer(), x + 10, y + 28, GROUP_NAME_POPUP_WIDTH - 20, 18, Text.literal("Group name")));
        screen.groupNameField.setMaxLength(64);
        screen.groupNameField.setFocused(true);
        screen.groupNameApplyButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> applyGroupNameFromPopup()).dimensions(x + 10, y + 52, 90, 18).build());
        screen.groupNameCancelButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> closeGroupNamePopup()).dimensions(x + 120, y + 52, 90, 18).build());
    }

    void closeGroupNamePopup() {
        screen.groupNamePopupOpen = false;
        if (screen.groupNameField != null) {
            screen.groupNameField.visible = false;
            screen.groupNameField.active = false;
            screen.groupNameField.setFocused(false);
        }
        if (screen.groupNameApplyButton != null) {
            screen.groupNameApplyButton.visible = false;
            screen.groupNameApplyButton.active = false;
        }
        if (screen.groupNameCancelButton != null) {
            screen.groupNameCancelButton.visible = false;
            screen.groupNameCancelButton.active = false;
        }
    }

    void applyGroupNameFromPopup() {
        if (screen.groupNameField == null) {
            closeGroupNamePopup();
            return;
        }
        String name = screen.groupNameField.getText().trim();
        if (name.isBlank()) {
            closeGroupNamePopup();
            return;
        }
        List<SelectedNote> notes = screen.selectionManager.currentSelectedNotes();
        List<SelectedEffect> effects = screen.selectionManager.currentSelectedEffects();
        if (!notes.isEmpty()) {
            List<EditorDraft.NoteGroupEntry> entries = new ArrayList<>();
            for (SelectedNote selectedNote : notes) {
                entries.add(new EditorDraft.NoteGroupEntry(selectedNote.track().id(), selectedNote.note().beat(), selectedNote.note().noteType()));
            }
            screen.state.editorDraft().createNoteGroup(name, entries);
        }
        if (!effects.isEmpty()) {
            List<EffectData> effectList = new ArrayList<>();
            for (SelectedEffect selectedEffect : effects) {
                effectList.add(selectedEffect.effect());
            }
            screen.state.editorDraft().createEffectGroup(name, effectList);
        }
        screen.state.setStatus("Grouped " + notes.size() + " note(s) and " + effects.size() + " effect(s) as '" + name + "'");
        closeGroupNamePopup();
    }

    void saveProject() {
        screen.state.saveProject(screen.state.project().projectPath());
    }

    void openNewProjectWizard() {
        if (!canLeaveEditorSession()) {
            return;
        }
        MinecraftClient.getInstance().setScreen(new NewSongWizardScreen(screen));
    }

    void openProjectBrowser() {
        if (!canLeaveEditorSession()) {
            return;
        }
        MinecraftClient.getInstance().setScreen(new ProjectBrowserScreen(screen));
    }

    void openProjectHub() {
        if (!canLeaveEditorSession()) {
            return;
        }
        MinecraftClient.getInstance().setScreen(new ProjectHubScreen(screen));
    }

    void openRawJsonEditor() {
        MinecraftClient.getInstance().setScreen(new RawLevelJsonEditorScreen(screen.state, screen));
    }

    boolean canLeaveEditorSession() {
        if (!RmcChartClient.isEditorSessionSaved() || screen.state.projectDirty()) {
            screen.state.setStatus("Save the project before leaving the editor session");
            return false;
        }
        return true;
    }

    void returnToTitle() {
        if (!canLeaveEditorSession()) {
            return;
        }
        MinecraftClient.getInstance().disconnect(new TitleScreen(), false);
    }

    void reloadAudio() {
        screen.state.reloadAudio();
    }

    void togglePlaybackFromToolbar() {
        screen.state.togglePlayback();
    }

    void stopPlaybackFromToolbar() {
        screen.state.stopPlayback();
    }

    void selectSong() {
        screen.state.setSelection(EditorSelection.song());
    }

    void selectMeta() {
        screen.state.setSelection(EditorSelection.meta());
    }

    void runCompatibilityCheck() {
        ChartCompatibilityValidator.Report report = ChartCompatibilityValidator.validate(screen.state.level());
        if (report.ok()) {
            screen.state.setStatus(report.summary());
            return;
        }
        ChartCompatibilityValidator.Issue first = report.issues().getFirst();
        screen.state.setStatus(report.summary() + " | " + first.location() + ": " + first.message());
    }

    void cycleSelectedNoteType() {
        if (screen.state.selection().note() == null) {
            return;
        }
        NoteData note = screen.state.selection().note();
        NoteType[] values = NoteType.values();
        note.setNoteType(values[(note.noteType().ordinal() + 1) % values.length]);
        if (note.noteType() == NoteType.HOLD) {
            note.pos().set(note.pos().x(), note.pos().y(), -1.0);
            note.setHoldLengthBeats(Math.max(screen.trackGridStep(screen.state.selection().track()), note.holdLengthBeats()));
        } else {
            note.setHoldLengthBeats(0.0);
        }
        screen.state.markDirty();
        screen.propertyPanel.populateFieldsFromSelection();
        screen.propertyPanel.layoutPropertyFields();
        screen.state.setStatus("Note type: " + EditorUtils.shortNoteLabel(note.noteType()));
    }

    void moveSelectedNoteTrack(int delta) {
        if (screen.state.selection().track() == null || screen.state.selection().note() == null) {
            return;
        }
        int index = EditorUtils.clamp(screen.state.trackIndex(screen.state.selection().track()) + delta, 0, Math.max(0, screen.state.tracks().size() - 1));
        TrackData targetTrack = screen.state.tracks().get(index);
        screen.state.moveSelectedNote(screen.state.selection().note().beat(), targetTrack);
        screen.state.markDirty();
        screen.propertyPanel.populateFieldsFromSelection();
        screen.propertyPanel.layoutPropertyFields();
        screen.state.setStatus("Moved note to Track " + targetTrack.id());
    }

    void cycleSelectedEffectType() {
        if (screen.state.selection().effect() == null) {
            return;
        }
        EffectData effect = screen.state.selection().effect();
        EffectType[] values = EffectType.values();
        effect.setEffectType(values[(effect.effectType().ordinal() + 1) % values.length]);
        screen.state.markDirty();
        screen.propertyPanel.populateFieldsFromSelection();
        screen.propertyPanel.layoutPropertyFields();
        screen.state.setStatus("Effect type: " + effect.effectType().name());
    }

    void deleteCurrentSelection() {
        if (screen.propertyPanel.trackEvents.deleteSelectedTrackEventRows(null)) {
            return;
        }
        int deletedNotes = deleteSelectedNotes();
        int deletedEffects = deleteSelectedEffects();
        int deletedClips = deleteSelectedEventClips();
        boolean deletedStateObject = false;
        if (deletedNotes == 0 && deletedEffects == 0 && deletedClips == 0) {
            EditorSelection.Kind kind = screen.state.selection().kind();
            if (kind == EditorSelection.Kind.TRACK || kind == EditorSelection.Kind.NOTE || kind == EditorSelection.Kind.EFFECT || kind == EditorSelection.Kind.BPM) {
                screen.state.deleteSelection();
                deletedStateObject = true;
            }
        } else {
            if (screen.state.selection().note() != null && !screen.state.selection().track().notes().contains(screen.state.selection().note())) {
                screen.state.setSelection(EditorSelection.track(screen.state.selection().track()));
            }
            if (screen.state.selection().effect() != null && !screen.state.level().effects().contains(screen.state.selection().effect())) {
                screen.state.setSelection(EditorSelection.meta());
            }
            screen.state.sortCurrentLevel();
            screen.state.markDirty();
            screen.state.setStatus("Deleted " + deletedNotes + " note(s), " + deletedEffects + " effect(s), " + deletedClips + " event clip(s)");
        }
        if (deletedNotes > 0 || deletedEffects > 0 || deletedClips > 0 || deletedStateObject) {
            screen.selectionManager.clearTimelineSelections();
            screen.propertyPanel.populateFieldsFromSelection();
            screen.history.captureHistorySnapshotIfNeeded();
        }
    }

    void duplicateSelectedObjects() {
        if (!screen.selectionManager.selectedNotes.isEmpty()) {
            duplicateSelectedNotes();
        } else if (!screen.selectionManager.selectedEffects.isEmpty()) {
            duplicateSelectedEffects();
        } else {
            screen.state.setStatus("Nothing selected to duplicate");
        }
    }

    private void duplicateSelectedNotes() {
        List<SelectedNote> notes = screen.selectionManager.currentSelectedNotes();
        if (notes.isEmpty()) {
            return;
        }
        double minBeat = notes.stream().mapToDouble(n -> n.note().beat()).min().orElse(screen.state.playheadBeat());
        double maxEnd = notes.stream().mapToDouble(n -> n.note().beat() + (n.note().noteType() == NoteType.HOLD ? n.note().holdLengthBeats() : 0.0)).max().orElse(minBeat);
        TrackData firstTrack = notes.getFirst().track();
        double offset = Math.max(maxEnd - minBeat, screen.trackGridStep(firstTrack));
        List<SelectedNote> duplicates = new ArrayList<>();
        for (SelectedNote selected : notes) {
            NoteData original = selected.note();
            NoteData copy = new NoteData(
                    original.noteType(),
                    original.beat() + offset,
                    original.pos().copy(),
                    original.scale().copy(),
                    original.rotation().copy(),
                    -1,
                    original.holdLengthBeats()
            );
            if (copy.noteType() == NoteType.HOLD) {
                copy.pos().set(copy.pos().x(), copy.pos().y(), -1.0);
            }
            selected.track().notes().add(copy);
            duplicates.add(new SelectedNote(selected.track(), copy));
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.selectionManager.replaceSelectedNotes(duplicates);
        if (!duplicates.isEmpty()) {
            screen.state.setSelection(EditorSelection.note(duplicates.getFirst().track(), duplicates.getFirst().note()));
        }
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        screen.state.setStatus("Duplicated " + duplicates.size() + " note(s)");
    }

    private void duplicateSelectedEffects() {
        List<SelectedEffect> effects = screen.selectionManager.currentSelectedEffects();
        if (effects.isEmpty()) {
            return;
        }
        double minBeat = effects.stream().mapToDouble(e -> e.effect().beat()).min().orElse(screen.state.playheadBeat());
        double maxBeat = effects.stream().mapToDouble(e -> e.effect().beat()).max().orElse(minBeat);
        double offset = Math.max(maxBeat - minBeat, 1.0);
        List<SelectedEffect> duplicates = new ArrayList<>();
        for (SelectedEffect selected : effects) {
            EffectData original = selected.effect();
            JsonObject properties = original.properties() == null ? new JsonObject() : original.properties().deepCopy();
            EffectData copy = new EffectData(original.effectType(), original.beat() + offset, properties);
            screen.state.level().effects().add(copy);
            duplicates.add(new SelectedEffect(copy));
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.selectionManager.replaceSelectedEffects(duplicates);
        if (!duplicates.isEmpty()) {
            screen.state.setSelection(EditorSelection.effect(duplicates.getFirst().effect()));
        }
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        screen.state.setStatus("Duplicated " + duplicates.size() + " effect(s)");
    }

    private int deleteSelectedNotes() {
        int deleted = 0;
        for (SelectedNote selectedNote : new ArrayList<>(screen.selectionManager.selectedNotes)) {
            if (selectedNote.track().notes().remove(selectedNote.note())) {
                deleted++;
            }
        }
        return deleted;
    }

    private int deleteSelectedEventClips() {
        int deleted = 0;
        List<SelectedEventClip> clips = new ArrayList<>(screen.selectionManager.selectedEventClips);
        clips.sort(Comparator.comparingInt((SelectedEventClip clip) -> clip.track().id())
                .thenComparingInt(clip -> clip.eventType().ordinal())
                .thenComparing(SelectedEventClip::eventIndex)
                .reversed());
        for (SelectedEventClip clip : clips) {
            List<NumEventData> events = screen.eventsForLane(clip.track(), clip.eventType());
            NumEventData event = screen.eventForClip(clip);
            if (event != null && events.remove(event)) {
                deleted++;
            }
        }
        return deleted;
    }

    private int deleteSelectedEffects() {
        int deleted = 0;
        List<EffectData> effects = new ArrayList<>();
        for (SelectedEffect selectedEffect : screen.selectionManager.selectedEffects) {
            effects.add(selectedEffect.effect());
        }
        for (EffectData effect : effects) {
            if (screen.state.level().effects().remove(effect)) {
                deleted++;
            }
        }
        return deleted;
    }

    void copySelectedNotesToClipboard() {
        List<SelectedNote> notes = screen.selectionManager.currentSelectedNotes();
        if (notes.isEmpty()) {
            screen.state.setStatus("No selected notes to copy");
            return;
        }
        notes.sort(Comparator.comparingInt((SelectedNote note) -> screen.state.trackIndex(note.track())).thenComparingDouble(note -> note.note().beat()));
        int baseTrackIndex = screen.state.trackIndex(notes.getFirst().track());
        double baseBeat = notes.stream().mapToDouble(note -> note.note().beat()).min().orElse(screen.state.playheadBeat());
        List<NoteClipboardEntry> entries = new ArrayList<>();
        for (SelectedNote selectedNote : notes) {
            NoteData note = selectedNote.note();
            entries.add(new NoteClipboardEntry(
                    screen.state.trackIndex(selectedNote.track()) - baseTrackIndex,
                    note.beat() - baseBeat,
                    note.noteType(),
                    note.pos().copy(),
                    note.scale().copy(),
                    note.rotation().copy(),
                    note.holdGroup(),
                    note.holdLengthBeats()
            ));
        }
        screen.selectionManager.noteClipboard = new NoteClipboard(entries, baseTrackIndex, baseBeat);
        screen.state.setStatus("Copied " + entries.size() + " note(s)");
    }

    void cutSelectedNotesToClipboard() {
        List<SelectedNote> notes = screen.selectionManager.currentSelectedNotes();
        if (notes.isEmpty()) {
            screen.state.setStatus("No selected notes to cut");
            return;
        }
        copySelectedNotesToClipboard();
        int deleted = deleteSelectedNotes();
        if (deleted <= 0) {
            return;
        }
        screen.selectionManager.clearTimelineSelections();
        screen.selectionManager.selectedEventClips.clear();
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        if (screen.state.selectedTrack() != null) {
            screen.state.setSelection(EditorSelection.track(screen.state.selectedTrack()));
        }
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        screen.state.setStatus("Cut " + deleted + " note(s)");
    }

    void pasteClipboardNotes() {
        if (screen.selectionManager.noteClipboard == null || screen.selectionManager.noteClipboard.entries().isEmpty()) {
            screen.state.setStatus("Clipboard has no notes");
            return;
        }
        screen.selectionManager.selectedEventClips.clear();
        screen.selectionManager.selectedEffects.clear();
        int anchorTrackIndex = screen.state.selectedTrack() == null ? screen.selectionManager.noteClipboard.baseTrackIndex() : screen.state.trackIndex(screen.state.selectedTrack());
        double anchorBeat = screen.state.playheadBeat();
        List<SelectedNote> pastedNotes = new ArrayList<>();
        for (NoteClipboardEntry entry : screen.selectionManager.noteClipboard.entries()) {
            int targetTrackIndex = EditorUtils.clamp(anchorTrackIndex + entry.trackOffset(), 0, Math.max(0, screen.state.tracks().size() - 1));
            TrackData track = screen.state.tracks().get(targetTrackIndex);
            NoteData note = new NoteData(entry.noteType(), anchorBeat + entry.beatOffset(), entry.pos().copy(), entry.scale().copy(), entry.rotation().copy(), entry.holdGroup(), entry.holdLengthBeats());
            track.notes().add(note);
            pastedNotes.add(new SelectedNote(track, note));
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.selectionManager.replaceSelectedNotes(pastedNotes);
        if (!pastedNotes.isEmpty()) {
            screen.state.setSelection(EditorSelection.note(pastedNotes.getFirst().track(), pastedNotes.getFirst().note()));
        }
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        screen.state.setStatus("Pasted " + pastedNotes.size() + " note(s)");
    }

    void copySelectedEffectsToClipboard() {
        List<SelectedEffect> effects = screen.selectionManager.currentSelectedEffects();
        if (effects.isEmpty()) {
            screen.state.setStatus("No selected effects to copy");
            return;
        }
        effects.sort(Comparator.comparingDouble(selectedEffect -> selectedEffect.effect().beat()));
        double baseBeat = effects.getFirst().effect().beat();
        List<EffectClipboardEntry> entries = new ArrayList<>();
        for (SelectedEffect selectedEffect : effects) {
            EffectData effect = selectedEffect.effect();
            JsonObject properties = effect.properties() == null ? new JsonObject() : effect.properties().deepCopy();
            entries.add(new EffectClipboardEntry(
                    effect.beat() - baseBeat,
                    effect.effectType(),
                    properties
            ));
        }
        screen.selectionManager.effectClipboard = new EffectClipboard(entries, baseBeat);
        screen.state.setStatus("Copied " + entries.size() + " effect(s)");
    }

    void cutSelectedEffectsToClipboard() {
        List<SelectedEffect> effects = screen.selectionManager.currentSelectedEffects();
        if (effects.isEmpty()) {
            screen.state.setStatus("No selected effects to cut");
            return;
        }
        copySelectedEffectsToClipboard();
        int deleted = deleteSelectedEffects();
        if (deleted <= 0) {
            return;
        }
        screen.selectionManager.clearTimelineSelections();
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.state.setSelection(EditorSelection.meta());
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        screen.state.setStatus("Cut " + deleted + " effect(s)");
    }

    void pasteClipboardEffects() {
        if (screen.selectionManager.effectClipboard == null || screen.selectionManager.effectClipboard.entries().isEmpty()) {
            screen.state.setStatus("Clipboard has no effects");
            return;
        }
        screen.selectionManager.selectedNotes.clear();
        screen.selectionManager.selectedEventClips.clear();
        double anchorBeat = screen.state.playheadBeat();
        List<SelectedEffect> pastedEffects = new ArrayList<>();
        for (EffectClipboardEntry entry : screen.selectionManager.effectClipboard.entries()) {
            EffectData effect = new EffectData(entry.effectType(), anchorBeat + entry.beatOffset(), entry.properties().deepCopy());
            screen.state.level().effects().add(effect);
            pastedEffects.add(new SelectedEffect(effect));
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.selectionManager.replaceSelectedEffects(pastedEffects);
        if (!pastedEffects.isEmpty()) {
            screen.state.setSelection(EditorSelection.effect(pastedEffects.getFirst().effect()));
        }
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        screen.state.setStatus("Pasted " + pastedEffects.size() + " effect(s)");
    }

    void groupSelectedObjects() {
        List<SelectedNote> notes = screen.selectionManager.currentSelectedNotes();
        List<SelectedEffect> effects = screen.selectionManager.currentSelectedEffects();
        if (notes.isEmpty() && effects.isEmpty()) {
            screen.state.setStatus("Select notes or effects to group");
            return;
        }
        openGroupNamePopup();
    }

    void selectAllNotes() {
        screen.selectionManager.selectedEventClips.clear();
        List<SelectedNote> notes = new ArrayList<>();
        for (TrackData track : screen.state.tracks()) {
            for (NoteData note : track.notes()) {
                notes.add(new SelectedNote(track, note));
            }
        }
        if (notes.isEmpty()) {
            screen.state.setStatus("No notes to select");
            return;
        }
        screen.selectionManager.replaceSelectedNotes(notes);
        screen.state.setSelection(EditorSelection.note(notes.getFirst().track(), notes.getFirst().note()));
        screen.state.setStatus("Selected " + notes.size() + " note(s)");
    }

    void moveSelectedNotesByShortcut(double beatDelta, int trackDelta) {
        List<SelectedNote> notes = screen.selectionManager.currentSelectedNotes();
        if (notes.isEmpty()) {
            return;
        }
        LinkedHashSet<SelectedNote> movedNotes = new LinkedHashSet<>();
        for (SelectedNote selectedNote : notes) {
            TrackData sourceTrack = selectedNote.track();
            int targetTrackIndex = EditorUtils.clamp(screen.state.trackIndex(sourceTrack) + trackDelta, 0, Math.max(0, screen.state.tracks().size() - 1));
            TrackData targetTrack = screen.state.tracks().get(targetTrackIndex);
            NoteData note = selectedNote.note();
            note.setBeat(Math.max(-64.0, note.beat() + beatDelta));
            if (sourceTrack != targetTrack) {
                sourceTrack.notes().remove(note);
                targetTrack.notes().add(note);
            }
            movedNotes.add(new SelectedNote(targetTrack, note));
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.selectionManager.replaceSelectedNotes(new ArrayList<>(movedNotes));
        if (!movedNotes.isEmpty()) {
            SelectedNote primary = movedNotes.getFirst();
            screen.state.setSelection(EditorSelection.note(primary.track(), primary.note()));
        }
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        screen.state.setStatus("Moved " + movedNotes.size() + " note(s)");
    }

    double noteShortcutBeatStep() {
        return screen.isShiftDown() ? 1.0 : 0.25;
    }

    void createNoteFromShortcut(NoteType noteType) {
        TrackData track = screen.state.selectedTrack();
        if (track == null) {
            track = screen.state.addTrack();
        }
        double beat = screen.snapBeat(screen.state.playheadBeat(), track);
        NoteData note = screen.state.addNote(track, beat);
        note.setNoteType(noteType);
        if (noteType == NoteType.HOLD) {
            note.pos().set(note.pos().x(), note.pos().y(), -1.0);
            note.setHoldLengthBeats(screen.trackGridStep(track));
        } else {
            note.setHoldLengthBeats(0.0);
        }
        screen.selectionManager.replaceSelectedNotes(List.of(new SelectedNote(track, note)));
        screen.propertyPanel.populateFieldsFromSelection();
        screen.propertyPanel.layoutPropertyFields();
        screen.state.setStatus("Created " + EditorUtils.shortNoteLabel(noteType) + " on Track " + track.id() + " at " + screen.subdivisionLabel(beat, track));
        screen.history.captureHistorySnapshotIfNeeded();
    }
}

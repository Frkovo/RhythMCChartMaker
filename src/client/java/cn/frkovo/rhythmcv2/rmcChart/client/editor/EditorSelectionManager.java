package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

final class EditorSelectionManager {
    private final ChartEditorScreen screen;

    final LinkedHashSet<SelectedNote> selectedNotes = new LinkedHashSet<>();
    final LinkedHashSet<SelectedEffect> selectedEffects = new LinkedHashSet<>();
    final LinkedHashSet<SelectedEventClip> selectedEventClips = new LinkedHashSet<>();

    NoteClipboard noteClipboard;
    EffectClipboard effectClipboard;

    EditorSelectionManager(ChartEditorScreen screen) {
        this.screen = screen;
    }

    boolean hasSelectedNotes() {
        return !currentSelectedNotes().isEmpty();
    }

    List<SelectedNote> currentSelectedNotes() {
        if (!selectedNotes.isEmpty()) {
            return new ArrayList<>(selectedNotes);
        }
        if (screen.state.selection().track() != null && screen.state.selection().note() != null) {
            return new ArrayList<>(List.of(new SelectedNote(screen.state.selection().track(), screen.state.selection().note())));
        }
        return new ArrayList<>();
    }

    void selectAllNotes() {
        selectedEventClips.clear();
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
        replaceSelectedNotes(notes);
        screen.state.setSelection(EditorSelection.note(notes.getFirst().track(), notes.getFirst().note()));
        screen.state.setStatus("Selected " + notes.size() + " note(s)");
    }

    void copySelectedNotesToClipboard() {
        List<SelectedNote> notes = currentSelectedNotes();
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
        noteClipboard = new NoteClipboard(entries, baseTrackIndex, baseBeat);
        screen.state.setStatus("Copied " + entries.size() + " note(s)");
    }

    void cutSelectedNotesToClipboard() {
        List<SelectedNote> notes = currentSelectedNotes();
        if (notes.isEmpty()) {
            screen.state.setStatus("No selected notes to cut");
            return;
        }
        copySelectedNotesToClipboard();
        int deleted = deleteSelectedNotes();
        if (deleted <= 0) {
            return;
        }
        clearTimelineSelections();
        selectedEventClips.clear();
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
        if (noteClipboard == null || noteClipboard.entries().isEmpty()) {
            screen.state.setStatus("Clipboard has no notes");
            return;
        }
        selectedEventClips.clear();
        selectedEffects.clear();
        int anchorTrackIndex = screen.state.selectedTrack() == null ? noteClipboard.baseTrackIndex() : screen.state.trackIndex(screen.state.selectedTrack());
        double anchorBeat = screen.state.playheadBeat();
        List<SelectedNote> pastedNotes = new ArrayList<>();
        for (NoteClipboardEntry entry : noteClipboard.entries()) {
            int targetTrackIndex = EditorUtils.clamp(anchorTrackIndex + entry.trackOffset(), 0, Math.max(0, screen.state.tracks().size() - 1));
            TrackData track = screen.state.tracks().get(targetTrackIndex);
            NoteData note = new NoteData(entry.noteType(), anchorBeat + entry.beatOffset(), entry.pos().copy(), entry.scale().copy(), entry.rotation().copy(), entry.holdGroup(), entry.holdLengthBeats());
            track.notes().add(note);
            pastedNotes.add(new SelectedNote(track, note));
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        replaceSelectedNotes(pastedNotes);
        if (!pastedNotes.isEmpty()) {
            screen.state.setSelection(EditorSelection.note(pastedNotes.getFirst().track(), pastedNotes.getFirst().note()));
        }
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        screen.state.setStatus("Pasted " + pastedNotes.size() + " note(s)");
    }

    List<SelectedEffect> currentSelectedEffects() {
        if (!selectedEffects.isEmpty()) {
            return new ArrayList<>(selectedEffects);
        }
        if (screen.state.selection().effect() != null) {
            return new ArrayList<>(List.of(new SelectedEffect(screen.state.selection().effect())));
        }
        return new ArrayList<>();
    }

    void copySelectedEffectsToClipboard() {
        List<SelectedEffect> effects = currentSelectedEffects();
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
        effectClipboard = new EffectClipboard(entries, baseBeat);
        screen.state.setStatus("Copied " + entries.size() + " effect(s)");
    }

    void cutSelectedEffectsToClipboard() {
        List<SelectedEffect> effects = currentSelectedEffects();
        if (effects.isEmpty()) {
            screen.state.setStatus("No selected effects to cut");
            return;
        }
        copySelectedEffectsToClipboard();
        int deleted = deleteSelectedEffects();
        if (deleted <= 0) {
            return;
        }
        clearTimelineSelections();
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.state.setSelection(EditorSelection.meta());
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        screen.state.setStatus("Cut " + deleted + " effect(s)");
    }

    void pasteClipboardEffects() {
        if (effectClipboard == null || effectClipboard.entries().isEmpty()) {
            screen.state.setStatus("Clipboard has no effects");
            return;
        }
        selectedNotes.clear();
        selectedEventClips.clear();
        double anchorBeat = screen.state.playheadBeat();
        List<SelectedEffect> pastedEffects = new ArrayList<>();
        for (EffectClipboardEntry entry : effectClipboard.entries()) {
            EffectData effect = new EffectData(entry.effectType(), anchorBeat + entry.beatOffset(), entry.properties().deepCopy());
            screen.state.level().effects().add(effect);
            pastedEffects.add(new SelectedEffect(effect));
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        replaceSelectedEffects(pastedEffects);
        if (!pastedEffects.isEmpty()) {
            screen.state.setSelection(EditorSelection.effect(pastedEffects.getFirst().effect()));
        }
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        screen.state.setStatus("Pasted " + pastedEffects.size() + " effect(s)");
    }

    void moveSelectedNotesByShortcut(double beatDelta, int trackDelta) {
        List<SelectedNote> notes = currentSelectedNotes();
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
        replaceSelectedNotes(new ArrayList<>(movedNotes));
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

    void clearTimelineSelections() {
        selectedNotes.clear();
        selectedEffects.clear();
        selectedEventClips.clear();
        screen.selectedEventClip = null;
        screen.selectedEventHandle = null;
    }

    int deleteSelectedNotes() {
        int deleted = 0;
        for (SelectedNote selectedNote : new ArrayList<>(selectedNotes)) {
            if (selectedNote.track().notes().remove(selectedNote.note())) {
                deleted++;
            }
        }
        return deleted;
    }

    int deleteSelectedEffects() {
        int deleted = 0;
        List<EffectData> effects = new ArrayList<>();
        for (SelectedEffect selectedEffect : selectedEffects) {
            effects.add(selectedEffect.effect());
        }
        for (EffectData effect : effects) {
            if (screen.state.level().effects().remove(effect)) {
                deleted++;
            }
        }
        return deleted;
    }

    int deleteSelectedEventClips() {
        int deleted = 0;
        List<SelectedEventClip> clips = new ArrayList<>(selectedEventClips);
        clips.sort(Comparator.comparingInt((SelectedEventClip clip) -> clip.track().id())
                .thenComparingInt(clip -> clip.eventType().ordinal())
                .thenComparing(SelectedEventClip::eventIndex)
                .reversed());
        for (SelectedEventClip clip : clips) {
            List<cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData> events = screen.eventsForLane(clip.track(), clip.eventType());
            cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData event = screen.eventForClip(clip);
            if (event != null && events.remove(event)) {
                deleted++;
            }
        }
        return deleted;
    }

    void replaceSelectedNotes(List<SelectedNote> notes) {
        selectedNotes.clear();
        selectedNotes.addAll(notes);
    }

    void replaceSelectedEffects(List<SelectedEffect> effects) {
        selectedEffects.clear();
        selectedEffects.addAll(effects);
    }

    void replaceSelectedEventClips(List<SelectedEventClip> clips) {
        selectedEventClips.clear();
        selectedEventClips.addAll(clips);
    }

    boolean isEffectSelected(EffectData effect) {
        return effect == screen.state.selection().effect() || selectedEffects.contains(new SelectedEffect(effect));
    }

    boolean isNoteSelected(TrackData track, NoteData note) {
        return note == screen.state.selection().note() || selectedNotes.contains(new SelectedNote(track, note));
    }

    void updateNoteSelectionFromClick(SelectedNote selectedNote, boolean additive) {
        if (!additive) {
            selectedEffects.clear();
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

    void updateEffectSelectionFromClick(SelectedEffect selectedEffect, boolean additive) {
        if (!additive) {
            selectedNotes.clear();
            selectedEventClips.clear();
            replaceSelectedEffects(List.of(selectedEffect));
            return;
        }
        if (!selectedEffects.add(selectedEffect)) {
            selectedEffects.remove(selectedEffect);
        }
        if (selectedEffects.isEmpty()) {
            selectedEffects.add(selectedEffect);
        }
    }

    void updateEventClipSelectionFromClick(SelectedEventClip clip, boolean additive) {
        if (!additive) {
            selectedNotes.clear();
            selectedEffects.clear();
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
}

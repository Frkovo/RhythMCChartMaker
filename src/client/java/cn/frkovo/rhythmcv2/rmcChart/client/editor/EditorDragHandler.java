package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.core.EasingFunctions;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.*;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.util.InputUtil;

import java.util.*;

final class EditorDragHandler {
    static final int TIMELINE_ZOOM_BAR_WIDTH = 126;
    static final int TIMELINE_ZOOM_BAR_HEIGHT = 8;

    private final ChartEditorScreen screen;

    EditorDragHandler(ChartEditorScreen screen) {
        this.screen = screen;
    }

    boolean handleTimelineClick(Click click, int x, int y, int width, int height) {
        double mouseX = click.x();
        double mouseY = click.y();
        int contentX = x + ChartEditorScreen.TIMELINE_LABEL_WIDTH;
        int contentWidth = Math.max(48, width - ChartEditorScreen.TIMELINE_LABEL_WIDTH);
        int audioY = y + height - ChartEditorScreen.TIMELINE_AUDIO_STRIP_HEIGHT;
        if (mouseY < y + ChartEditorScreen.TIMELINE_RULER_HEIGHT) {
            if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT && isInsideTimelineZoomBar(mouseX, mouseY, contentX, contentWidth, y)) {
                setTimelineZoomFromMouseX(mouseX, contentX, contentWidth);
                screen.dragMode = DragMode.TIMELINE_ZOOM;
            } else {
                screen.state.seekToBeat(screen.snapBeat(screen.screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX))));
            }
            return true;
        }
        if (mouseY >= audioY) {
            screen.state.seekToBeat(screen.snapBeat(screen.screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX))));
            return true;
        }
        double rawBeat = screen.screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX));
        double beat = screen.snapBeat(rawBeat);
        List<TimelineLaneLayout> layouts = screen.timeline.buildTimelineLaneLayouts(y + ChartEditorScreen.TIMELINE_RULER_HEIGHT);
        TimelineLaneLayout layout = screen.timeline.timelineLaneAt(layouts, mouseY);
        if (layout == null) {
            return false;
        }
        TimelineLane lane = layout.lane();
        if (lane.track != null) {
            beat = screen.snapBeat(rawBeat, lane.track);
        }
        if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_RIGHT && lane.track != null && lane.type == LaneType.EVENTS && lane.eventType != null) {
            if (splitEventAtBeat(lane.track, lane.eventType, beat)) {
                screen.propertyPanel.populateFieldsFromSelection();
            } else {
                screen.state.setStatus("Right-click inside an event clip to split it");
            }
            return true;
        }
        if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_RIGHT && lane.track != null && lane.type == LaneType.NOTES) {
            double addBeat = beat >= 0.0 ? beat : screen.snapBeat(screen.state.playheadBeat(), lane.track);
            NoteData note = screen.state.addNote(lane.track, addBeat);
            note.pos().set(0.0, 0.0, note.noteType() == NoteType.HOLD ? -1.0 : note.pos().z());
            screen.state.markDirty();
            screen.selectionManager.replaceSelectedNotes(List.of(new SelectedNote(lane.track, note)));
            screen.propertyPanel.populateFieldsFromSelection();
            screen.state.setStatus("Added note on TrackID " + lane.track.id() + " at " + screen.propertyPanel.format(addBeat));
            return true;
        }
        if (lane.type == LaneType.BPM) {
            screen.selectionManager.selectedNotes.clear();
            screen.selectionManager.selectedEffects.clear();
            screen.selectionManager.selectedEventClips.clear();
            BpmPoint bpm = findNearestBpm(contentX, contentWidth, mouseX);
            if (bpm != null) {
                screen.state.setSelection(EditorSelection.bpm(bpm));
                screen.dragMode = DragMode.BPM;
            } else {
                screen.state.seekToBeat(beat);
            }
            return true;
        }
        if (lane.type == LaneType.FX_TRACK_HEADER) {
            if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_RIGHT) {
                EffectData effect = screen.state.addEffect();
                screen.selectionManager.replaceSelectedEffects(List.of(new SelectedEffect(effect)));
                screen.state.setSelection(EditorSelection.effect(effect));
                screen.propertyPanel.populateFieldsFromSelection();
                screen.state.setStatus("Added effect at playhead " + screen.propertyPanel.format(screen.state.playheadBeat()));
                return true;
            }
            if (mouseX < x + 24) {
                screen.state.toggleFxTrackExpanded();
            } else {
                screen.selectionManager.selectedNotes.clear();
                screen.selectionManager.selectedEffects.clear();
                screen.selectionManager.selectedEventClips.clear();
                screen.state.setSelection(EditorSelection.meta());
            }
            return true;
        }
        if (lane.type == LaneType.FX_EFFECT_CLIP && lane.effect != null) {
            screen.selectionManager.selectedNotes.clear();
            screen.selectionManager.selectedEventClips.clear();
            SelectedEffect selectedEffect = new SelectedEffect(lane.effect);
            screen.selectionManager.updateEffectSelectionFromClick(selectedEffect, screen.isControlDown());
            screen.state.setSelection(EditorSelection.effect(lane.effect));
            initializeEffectClipDrag(beat);
            screen.dragMode = DragMode.EFFECT;
            return true;
        }
        if (lane.track != null && lane.type == LaneType.TRACK_HEADER) {
            screen.selectionManager.selectedNotes.clear();
            screen.selectionManager.selectedEventClips.clear();
            if (mouseX < x + 24) {
                screen.state.toggleTrackExpanded(lane.track);
            } else {
                screen.state.setSelection(EditorSelection.track(lane.track));
            }
            return true;
        }
        if (lane.track != null && lane.type == LaneType.EVENT_GROUP_HEADER && lane.eventGroup != null) {
            screen.selectionManager.selectedNotes.clear();
            screen.selectionManager.selectedEventClips.clear();
            screen.state.setSelection(EditorSelection.track(lane.track));
            screen.state.toggleTrackEventGroupExpanded(lane.track, lane.eventGroup);
            screen.state.setStatus((screen.state.isTrackEventGroupExpanded(lane.track, lane.eventGroup) ? "Expanded " : "Collapsed ")
                    + lane.eventGroup + " lanes for TrackID " + lane.track.id());
            return true;
        }
        if (lane.track != null && lane.type == LaneType.NOTES) {
            TrackData track = lane.track;
            SelectedNote holdLengthHandle = findHoldLengthHandleAt(track, contentX, contentWidth, mouseX, layout.top(), layout.height(), mouseY);
            if (holdLengthHandle != null) {
                screen.draggingHoldLengthNote = holdLengthHandle;
                screen.dragAnchorBeat = beat;
                screen.dragAnchorHoldLength = holdLengthHandle.note().holdLengthBeats();
                screen.selectionManager.replaceSelectedNotes(List.of(holdLengthHandle));
                screen.state.setSelection(EditorSelection.note(holdLengthHandle.track(), holdLengthHandle.note()));
                screen.dragMode = DragMode.HOLD_LENGTH;
                screen.state.setStatus("Drag hold tail, snapped to Track " + track.id() + " 1/" + track.beatDivision());
                return true;
            }
            NoteData note = findNearestNote(track, contentX, contentWidth, mouseX, layout.top(), layout.height(), mouseY);
            if (note != null) {
                SelectedNote selectedNote = new SelectedNote(track, note);
                screen.selectionManager.updateNoteSelectionFromClick(selectedNote, screen.isControlDown());
                screen.state.setSelection(EditorSelection.note(track, note));
                initializeNoteDrag(layout, beat, mouseY);
                screen.dragMode = DragMode.NOTE;
            } else {
                screen.state.setSelection(EditorSelection.track(track));
                if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT) {
                    screen.selectionBox = new SelectionBox((int) mouseX, (int) mouseY, (int) mouseX, (int) mouseY);
                    screen.selectionBoxAdditive = screen.isControlDown();
                    screen.dragMode = DragMode.BOX_SELECT;
                }
                screen.state.seekToBeat(beat);
            }
            return true;
        }
        if (lane.track != null && lane.type == LaneType.EVENTS) {
            SelectedEventHandle handle = findNearestEventHandle(lane.track, lane.eventType, contentX, layout.top(), layout.height(), contentWidth, mouseX, mouseY);
            if (handle != null) {
                screen.selectedEventHandle = handle;
                screen.selectedEventClip = new SelectedEventClip(handle.track, handle.eventType, handle.eventIndex);
                screen.selectionManager.updateEventClipSelectionFromClick(screen.selectedEventClip, screen.isControlDown());
                initializeEventClipDrag(beat);
                screen.dragMode = DragMode.EVENT_HANDLE;
                screen.state.setSelection(EditorSelection.track(lane.track));
                screen.state.setStatus("Editing TrackID " + lane.track.id() + " " + lane.eventType.label + " " + handle.anchor.label);
                return true;
            }
            SelectedEventClip clip = findEventClipAt(lane.track, lane.eventType, contentX, layout.top(), layout.height(), contentWidth, mouseX, mouseY);
            if (clip != null) {
                screen.selectedEventClip = clip;
                screen.selectedEventHandle = null;
                screen.selectionManager.updateEventClipSelectionFromClick(clip, screen.isControlDown());
                initializeEventClipDrag(beat);
                screen.state.setSelection(EditorSelection.track(lane.track));
                screen.dragMode = DragMode.EVENT_CLIP;
                screen.state.setStatus("Selected TrackID " + lane.track.id() + " " + lane.eventType.label + " clip");
                return true;
            }
            screen.state.setSelection(EditorSelection.track(lane.track));
            screen.selectedEventClip = null;
            screen.selectedEventHandle = null;
            screen.state.setStatus(AutomationCardUi.timelineLaneStatus(lane.track.id(), lane.eventType.label));
            return true;
        }
        return false;
    }

    void handleTimelineDrag(double mouseX, double mouseY) {
        EditorLayout layout = screen.editorLayout();
        int contentX = layout.centerX() + ChartEditorScreen.TIMELINE_LABEL_WIDTH;
        int contentWidth = Math.max(48, layout.centerWidth() - ChartEditorScreen.TIMELINE_LABEL_WIDTH);
        double beat = screen.snapBeat(screen.screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX)));
        List<TimelineLaneLayout> layouts = screen.timeline.buildTimelineLaneLayouts(layout.timelineY() + ChartEditorScreen.TIMELINE_RULER_HEIGHT);
        int laneBottom = layout.timelineY() + layout.timelineHeight() - ChartEditorScreen.TIMELINE_AUDIO_STRIP_HEIGHT;
        TimelineLaneLayout hoveredLayout = mouseY >= laneBottom ? null : screen.timeline.timelineLaneAt(layouts, mouseY);
        switch (screen.dragMode) {
            case BPM -> screen.state.moveSelectedBpm(beat);
            case EFFECT -> {
                double snappedBeat = screen.snapBeat(screen.screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX)));
                applyDraggedEffects(snappedBeat - screen.dragAnchorBeat);
            }
            case NOTE -> {
                TimelineLane lane = hoveredLayout == null ? null : hoveredLayout.lane();
                TrackData targetTrack = lane != null && lane.track != null ? lane.track : screen.state.selection().track();
                double snappedBeat = applyTimelineBeatSnap(screen.screenToBeat(contentX, contentWidth, Math.max(contentX, mouseX)), contentX, contentWidth, selectedNoteBeats(), selectedEventClipEdges(), targetTrack);
                double beatDelta = snappedBeat - screen.dragAnchorBeat;
                double projectionDelta = 0.0;
                if (hoveredLayout != null && hoveredLayout.lane().type == LaneType.NOTES) {
                    double range = EditorUtils.noteLaneProjectionRange(targetTrack);
                    double anchorValue = EditorUtils.noteProjectionScreenToValue(hoveredLayout.top(), hoveredLayout.height(), range, screen.dragAnchorNoteProjectionY);
                    double currentValue = EditorUtils.noteProjectionScreenToValue(hoveredLayout.top(), hoveredLayout.height(), range, mouseY);
                    projectionDelta = currentValue - anchorValue;
                }
                applyDraggedNotes(beatDelta, projectionDelta, targetTrack, screen.selectionManager.selectedNotes.size() == 1 ? targetTrack : null);
            }
            case EVENT_HANDLE -> dragSelectedEventHandle(mouseX, mouseY, contentX, contentWidth, hoveredLayout);
            case EVENT_CLIP -> dragSelectedEventClip(mouseX, contentX, contentWidth);
            case HOLD_LENGTH -> dragHoldLength(mouseX, contentX, contentWidth);
            case BOX_SELECT -> screen.selectionBox = screen.selectionBox == null
                    ? new SelectionBox((int) mouseX, (int) mouseY, (int) mouseX, (int) mouseY)
                    : new SelectionBox(screen.selectionBox.startX(), screen.selectionBox.startY(), (int) mouseX, (int) mouseY);
            default -> {}
        }
        screen.propertyPanel.populateFieldsFromSelection();
    }

    boolean handlePreviewClick(Click click, int x, int y, int width, int height) {
        if (click.button() != InputUtil.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        int transportX = x + 10;
        int transportY = y + 6;
        if (EditorUtils.isInside(click.x(), click.y(), transportX, transportY, 52, 18)) {
            screen.actions.togglePlaybackFromToolbar();
            return true;
        }
        if (EditorUtils.isInside(click.x(), click.y(), transportX + 58, transportY, 52, 18)) {
            screen.actions.stopPlaybackFromToolbar();
            screen.state.seekToBeat(screen.state.playbackStartBeat());
            screen.state.togglePlayback();
            return true;
        }
        if (EditorUtils.isInside(click.x(), click.y(), transportX + 116, transportY, 64, 18)) {
            if (screen.state.serverPreviewRunning()) {
                screen.previewBridge.stopServerPreview();
            } else {
                screen.previewBridge.startWorldPreviewFromEditor(PendingPreviewAction.START);
            }
            return true;
        }
        if (EditorUtils.isInside(click.x(), click.y(), transportX + 186, transportY, 46, 18)) {
            screen.previewBridge.stopServerPreview();
            return true;
        }
        return false;
    }

    boolean handleTrackBarClick(double mouseX, double mouseY, int x, int y, int width, int height) {
        int cursorX = x + 6;
        for (TrackData track : screen.state.visibleTracks()) {
            if (EditorUtils.isInside(mouseX, mouseY, cursorX + 4, y + 6, 12, 12)) {
                screen.state.toggleTrackExpanded(track);
                return true;
            }
            if (EditorUtils.isInside(mouseX, mouseY, cursorX, y + 4, 88, height - 8)) {
                screen.state.setSelection(EditorSelection.track(track));
                return true;
            }
            cursorX += 88 + 4;
        }
        int addWidth = 70;
        if (EditorUtils.isInside(mouseX, mouseY, x + width - addWidth - 6, y + 4, addWidth, height - 8)) {
            screen.state.addTrack();
            return true;
        }
        return false;
    }

    boolean handleLeftPanelClick(double mouseX, double mouseY, int x, int y) {
        if (screen.sceneMap.handleSceneMapAreaClick(mouseX, mouseY, x, y)) {
            return true;
        }
        int rowY = y + 28 + (screen.leftPanelWidth() - 20) + 12;
        for (TrackData track : screen.state.tracks()) {
            if (EditorUtils.isInside(mouseX, mouseY, x + 10, rowY + 4, 14, 14)) {
                screen.state.toggleTrackExpanded(track);
                return true;
            }
            if (EditorUtils.isInside(mouseX, mouseY, x + 6, rowY, screen.leftPanelWidth() - 12, 22)) {
                screen.state.setSelection(EditorSelection.track(track));
                return true;
            }
            rowY += 30;
        }
        return false;
    }


    void applySelectionBox() {
        if (screen.selectionBox == null) {
            return;
        }
        EditorLayout editorLayout = screen.editorLayout();
        int timelineY = editorLayout.timelineY();
        int contentX = editorLayout.centerX() + ChartEditorScreen.TIMELINE_LABEL_WIDTH;
        int contentWidth = Math.max(48, editorLayout.centerWidth() - ChartEditorScreen.TIMELINE_LABEL_WIDTH);
        int contentBottom = timelineY + editorLayout.timelineHeight() - ChartEditorScreen.TIMELINE_AUDIO_STRIP_HEIGHT;
        List<TimelineLaneLayout> layouts = screen.timeline.buildTimelineLaneLayouts(timelineY + ChartEditorScreen.TIMELINE_RULER_HEIGHT);
        int left = Math.min(screen.selectionBox.startX(), screen.selectionBox.endX());
        int right = Math.max(screen.selectionBox.startX(), screen.selectionBox.endX());
        int top = Math.max(timelineY + ChartEditorScreen.TIMELINE_RULER_HEIGHT, Math.min(screen.selectionBox.startY(), screen.selectionBox.endY()));
        int bottom = Math.min(contentBottom, Math.max(screen.selectionBox.startY(), screen.selectionBox.endY()));
        LinkedHashSet<SelectedNote> hitNotes = screen.selectionBoxAdditive ? new LinkedHashSet<>(screen.selectionManager.selectedNotes) : new LinkedHashSet<>();
        LinkedHashSet<SelectedEffect> hitEffects = screen.selectionBoxAdditive ? new LinkedHashSet<>(screen.selectionManager.selectedEffects) : new LinkedHashSet<>();
        LinkedHashSet<SelectedEventClip> hitClips = screen.selectionBoxAdditive ? new LinkedHashSet<>(screen.selectionManager.selectedEventClips) : new LinkedHashSet<>();
        SelectedNote firstNote = null;
        SelectedEffect firstEffect = null;
        SelectedEventClip firstClip = null;
        for (TimelineLaneLayout layout : layouts) {
            TimelineLane lane = layout.lane();
            if (lane.type == LaneType.NOTES && lane.track() != null) {
                double range = EditorUtils.noteLaneProjectionRange(lane.track());
                for (NoteData note : lane.track().notes()) {
                    int noteX = screen.beatToScreen(contentX, contentWidth, note.beat());
                    int noteY = EditorUtils.noteProjectionToScreenY(layout.top(), layout.height(), range, EditorUtils.noteProjectionValue(note));
                    if (noteX >= left && noteX <= right && noteY >= top && noteY <= bottom) {
                        SelectedNote selectedNote = new SelectedNote(lane.track(), note);
                        hitNotes.add(selectedNote);
                        if (firstNote == null) {
                            firstNote = selectedNote;
                        }
                    }
                }
            } else if (lane.type == LaneType.FX_EFFECT_CLIP && lane.effect != null) {
                int startX = screen.beatToScreen(contentX, contentWidth, lane.effect.beat());
                int endX = screen.beatToScreen(contentX, contentWidth, screen.effectEndBeat(lane.effect));
                int clipLeft = Math.min(startX, endX);
                int clipRight = Math.max(startX, endX);
                if (EditorUtils.rectanglesIntersect(left, top, right, bottom, clipLeft, layout.top() + 4, clipRight, layout.top() + layout.height() - 4)) {
                    SelectedEffect selectedEffect = new SelectedEffect(lane.effect);
                    hitEffects.add(selectedEffect);
                    if (firstEffect == null) {
                        firstEffect = selectedEffect;
                    }
                }
            } else if (lane.type == LaneType.EVENTS && lane.track() != null && lane.eventType() != null) {
                List<NumEventData> events = screen.eventsForLane(lane.track(), lane.eventType());
                for (int index = 0; index < events.size(); index++) {
                    NumEventData event = events.get(index);
                    int startX = screen.beatToScreen(contentX, contentWidth, event.startBeat());
                    int endX = Math.max(startX + 2, screen.beatToScreen(contentX, contentWidth, event.endBeat()));
                    if (EditorUtils.rectanglesIntersect(left, top, right, bottom, startX, layout.top() + 4, endX, layout.top() + layout.height() - 4)) {
                        SelectedEventClip clip = new SelectedEventClip(lane.track(), lane.eventType(), index);
                        hitClips.add(clip);
                        if (firstClip == null) {
                            firstClip = clip;
                        }
                    }
                }
            }
        }
        screen.selectionManager.replaceSelectedNotes(new ArrayList<>(hitNotes));
        screen.selectionManager.replaceSelectedEffects(new ArrayList<>(hitEffects));
        screen.selectionManager.replaceSelectedEventClips(new ArrayList<>(hitClips));
        if (firstNote != null) {
            screen.state.setSelection(EditorSelection.note(firstNote.track(), firstNote.note()));
            screen.state.setStatus("Selected " + hitNotes.size() + " note(s)");
        } else if (firstEffect != null) {
            screen.state.setSelection(EditorSelection.effect(firstEffect.effect()));
            screen.state.setStatus("Selected " + hitEffects.size() + " effect clip(s)");
        } else if (firstClip != null) {
            screen.selectedEventClip = firstClip;
            screen.state.setSelection(EditorSelection.track(firstClip.track()));
            screen.state.setStatus("Selected " + hitClips.size() + " event clip(s)");
        } else if (!screen.selectionBoxAdditive) {
            screen.selectionManager.clearTimelineSelections();
        }
    }

    BpmPoint findNearestBpm(int x, int width, double mouseX) {
        return screen.state.level().meta().bpms().stream()
                .min(Comparator.comparingDouble(bpm -> Math.abs(screen.beatToScreen(x, width, bpm.beat()) - mouseX)))
                .filter(bpm -> Math.abs(screen.beatToScreen(x, width, bpm.beat()) - mouseX) <= 6.0)
                .orElse(null);
    }

    NoteData findNearestNote(TrackData track, int x, int width, double mouseX, int rowTop, int laneHeight, double mouseY) {
        double range = EditorUtils.noteLaneProjectionRange(track);
        NoteData best = null;
        double bestDistance = Double.MAX_VALUE;
        for (NoteData note : track.notes()) {
            int noteX = screen.beatToScreen(x, width, note.beat());
            int noteY = EditorUtils.noteProjectionToScreenY(rowTop, laneHeight, range, EditorUtils.noteProjectionValue(note));
            double distance = EditorUtils.squaredDistance(mouseX, mouseY, noteX, noteY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = note;
            }
        }
        return bestDistance <= 256.0 ? best : null;
    }

    SelectedNote findHoldLengthHandleAt(TrackData track, int x, int width, double mouseX, int rowTop, int laneHeight, double mouseY) {
        if (track == null) {
            return null;
        }
        double range = EditorUtils.noteLaneProjectionRange(track);
        NoteData best = null;
        double bestDistance = Double.MAX_VALUE;
        for (NoteData note : track.notes()) {
            if (note.noteType() != NoteType.HOLD) {
                continue;
            }
            int handleX = screen.beatToScreen(x, width, note.beat() + note.holdLengthBeats());
            int handleY = EditorUtils.noteProjectionToScreenY(rowTop, laneHeight, range, EditorUtils.noteProjectionValue(note));
            double distance = EditorUtils.squaredDistance(mouseX, mouseY, handleX, handleY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = note;
            }
        }
        return best != null && bestDistance <= 144.0 ? new SelectedNote(track, best) : null;
    }

    void initializeNoteDrag(TimelineLaneLayout layout, double pointerBeat, double pointerY) {
        screen.noteDragSnapshots.clear();
        if (screen.selectionManager.selectedNotes.isEmpty() && screen.state.selection().track() != null && screen.state.selection().note() != null) {
            screen.selectionManager.selectedNotes.add(new SelectedNote(screen.state.selection().track(), screen.state.selection().note()));
        }
        screen.dragAnchorBeat = pointerBeat;
        screen.dragAnchorNoteProjectionY = pointerY;
        for (SelectedNote selectedNote : screen.selectionManager.selectedNotes) {
            NoteData note = selectedNote.note();
            double projection = EditorUtils.noteProjectionValue(note);
            double radius = Math.hypot(note.pos().x(), note.pos().y());
            screen.noteDragSnapshots.put(selectedNote, new NoteDragSnapshot(selectedNote.track(), note.beat(), projection, radius));
        }
    }

    void initializeEventClipDrag(double pointerBeat) {
        screen.eventClipDragSnapshots.clear();
        screen.dragAnchorBeat = pointerBeat;
        for (SelectedEventClip clip : screen.selectionManager.selectedEventClips) {
            NumEventData event = screen.eventForClip(clip);
            if (event != null) {
                screen.eventClipDragSnapshots.put(clip, new EventClipDragSnapshot(event, event.startBeat(), event.endBeat()));
            }
        }
    }

    void initializeEffectClipDrag(double pointerBeat) {
        screen.effectDragSnapshots.clear();
        screen.dragAnchorBeat = pointerBeat;
        for (SelectedEffect selectedEffect : screen.selectionManager.selectedEffects) {
            screen.effectDragSnapshots.put(selectedEffect, new EffectDragSnapshot(selectedEffect.effect().beat(), screen.effectEndBeat(selectedEffect.effect())));
        }
    }

    void applyDraggedEffects(double beatDelta) {
        if (screen.effectDragSnapshots.isEmpty()) {
            return;
        }
        double minStart = Double.POSITIVE_INFINITY;
        for (EffectDragSnapshot snapshot : screen.effectDragSnapshots.values()) {
            minStart = Math.min(minStart, snapshot.startBeat() + beatDelta);
        }
        if (minStart < -64.0) {
            beatDelta += -64.0 - minStart;
        }
        for (Map.Entry<SelectedEffect, EffectDragSnapshot> entry : screen.effectDragSnapshots.entrySet()) {
            EffectData effect = entry.getKey().effect();
            double newBeat = Math.max(-64.0, entry.getValue().startBeat() + beatDelta);
            effect.setBeat(newBeat);
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.state.setStatus("Moved " + screen.effectDragSnapshots.size() + " effect clip(s)");
    }

    void applyDraggedNotes(double beatDelta, double projectionDelta, TrackData hoveredTrack, TrackData singleMoveTarget) {
        if (screen.noteDragSnapshots.isEmpty()) {
            return;
        }
        LinkedHashSet<SelectedNote> updatedSelection = new LinkedHashSet<>();
        SelectedNote primary = null;
        for (Map.Entry<SelectedNote, NoteDragSnapshot> entry : screen.noteDragSnapshots.entrySet()) {
            SelectedNote selectedNote = entry.getKey();
            NoteDragSnapshot snapshot = entry.getValue();
            NoteData note = selectedNote.note();
            TrackData targetTrack = screen.noteDragSnapshots.size() == 1 && singleMoveTarget != null ? singleMoveTarget : snapshot.track();
            note.setBeat(snapshot.beat() + beatDelta);
            double newProjection = snapshot.projectionValue() + projectionDelta;
            double newAngle = newProjection * Math.PI;
            double radius = snapshot.radius();
            if (radius > 1.0E-6) {
                double newX = radius * Math.cos(newAngle);
                double newY = radius * Math.sin(newAngle);
                note.pos().set(newX, newY, note.noteType() == NoteType.HOLD ? -1.0 : note.pos().z());
            }
            if (snapshot.track() != targetTrack) {
                snapshot.track().notes().remove(note);
                targetTrack.notes().add(note);
            }
            SelectedNote updated = new SelectedNote(targetTrack, note);
            updatedSelection.add(updated);
            if (primary == null || note == screen.state.selection().note()) {
                primary = updated;
            }
        }
        screen.selectionManager.replaceSelectedNotes(new ArrayList<>(updatedSelection));
        if (primary != null) {
            screen.state.setSelection(EditorSelection.note(primary.track(), primary.note()));
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.snapGuideBeat = Double.isFinite(screen.dragAnchorBeat + beatDelta) ? screen.dragAnchorBeat + beatDelta : Double.NaN;
    }

    void dragHoldLength(double mouseX, int timelineX, int timelineWidth) {
        if (screen.draggingHoldLengthNote == null || screen.draggingHoldLengthNote.note() == null) {
            return;
        }
        NoteData note = screen.draggingHoldLengthNote.note();
        TrackData track = screen.draggingHoldLengthNote.track();
        double rawTailBeat = Math.max(note.beat() + screen.trackGridStep(track), screen.screenToBeat(timelineX, timelineWidth, mouseX));
        double snappedTailBeat = screen.snapBeat(rawTailBeat, track);
        double length = screen.snapHoldLength(snappedTailBeat - note.beat(), track);
        note.setHoldLengthBeats(length);
        note.pos().set(note.pos().x(), note.pos().y(), -1.0);
        screen.state.markDirty();
        screen.snapGuideBeat = note.beat() + length;
        screen.state.setStatus("Hold length " + screen.propertyPanel.format(length) + " beats (" + screen.timeline.subdivisionLabel(note.beat() + length, track) + ")");
    }

    void dragSelectedEventClip(double mouseX, int timelineX, int timelineWidth) {
        if (screen.eventClipDragSnapshots.isEmpty()) {
            return;
        }
        double snappedBeat = applyTimelineBeatSnap(screen.screenToBeat(timelineX, timelineWidth, mouseX), timelineX, timelineWidth, selectedNoteBeats(), selectedEventClipEdges(), screen.state.selectedTrack());
        double beatDelta = snappedBeat - screen.dragAnchorBeat;
        double minStart = Double.POSITIVE_INFINITY;
        for (EventClipDragSnapshot snapshot : screen.eventClipDragSnapshots.values()) {
            minStart = Math.min(minStart, snapshot.startBeat() + beatDelta);
        }
        if (minStart < -64.0) {
            beatDelta += -64.0 - minStart;
        }
        for (Map.Entry<SelectedEventClip, EventClipDragSnapshot> entry : screen.eventClipDragSnapshots.entrySet()) {
            EventClipDragSnapshot snapshot = entry.getValue();
            snapshot.event().setStartBeat(snapshot.startBeat() + beatDelta);
            snapshot.event().setEndBeat(snapshot.endBeat() + beatDelta);
        }
        LinkedHashSet<SelectedEventClip> updatedClips = new LinkedHashSet<>();
        SelectedEventClip primary = null;
        for (Map.Entry<SelectedEventClip, EventClipDragSnapshot> entry : screen.eventClipDragSnapshots.entrySet()) {
            SelectedEventClip clip = entry.getKey();
            EventClipDragSnapshot snapshot = entry.getValue();
            List<NumEventData> events = screen.eventsForLane(clip.track(), clip.eventType());
            int updatedIndex = events.indexOf(snapshot.event());
            if (updatedIndex >= 0) {
                SelectedEventClip updated = new SelectedEventClip(clip.track(), clip.eventType(), updatedIndex);
                updatedClips.add(updated);
                if (primary == null || clip.equals(screen.selectedEventClip)) {
                    primary = updated;
                }
            }
        }
        screen.selectionManager.replaceSelectedEventClips(new ArrayList<>(updatedClips));
        if (primary != null) {
            screen.selectedEventClip = primary;
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.state.setStatus("Moved " + updatedClips.size() + " event clip(s)");
    }

    double applyTimelineBeatSnap(double rawBeat, int timelineX, int timelineWidth, Set<Double> excludedNoteBeats, Set<Double> excludedClipBeats, TrackData snapTrack) {
        if (screen.isAltDown()) {
            screen.snapGuideBeat = Double.NaN;
            return rawBeat;
        }
        double bestBeat = screen.snapBeat(rawBeat, snapTrack);
        double bestDistance = Math.abs(screen.beatToScreen(timelineX, timelineWidth, bestBeat) - screen.beatToScreen(timelineX, timelineWidth, rawBeat));
        for (double candidate : collectSnapCandidateBeats()) {
            if (isExcludedSnapBeat(candidate, excludedNoteBeats, excludedClipBeats)) {
                continue;
            }
            double distance = Math.abs(screen.beatToScreen(timelineX, timelineWidth, candidate) - screen.beatToScreen(timelineX, timelineWidth, rawBeat));
            if (distance <= 10.0 && distance < bestDistance) {
                bestDistance = distance;
                bestBeat = candidate;
            }
        }
        screen.snapGuideBeat = bestBeat;
        return bestBeat;
    }

    boolean isExcludedSnapBeat(double candidate, Set<Double> excludedNoteBeats, Set<Double> excludedClipBeats) {
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

    List<Double> collectSnapCandidateBeats() {
        List<Double> candidates = new ArrayList<>();
        for (TrackData track : screen.state.tracks()) {
            for (NoteData note : track.notes()) {
                candidates.add(note.beat());
            }
            for (EventLaneType eventType : EventLaneType.values()) {
                for (NumEventData event : screen.eventsForLane(track, eventType)) {
                    candidates.add(event.startBeat());
                    candidates.add(event.endBeat());
                }
            }
        }
        for (BpmPoint bpm : screen.state.level().meta().bpms()) {
            candidates.add(bpm.beat());
        }
        for (EffectData effect : screen.state.level().effects()) {
            candidates.add(effect.beat());
        }
        return candidates;
    }

    Set<Double> selectedNoteBeats() {
        LinkedHashSet<Double> beats = new LinkedHashSet<>();
        for (SelectedNote selectedNote : screen.selectionManager.selectedNotes) {
            beats.add(selectedNote.note().beat());
        }
        return beats;
    }

    Set<Double> selectedEventClipEdges() {
        LinkedHashSet<Double> beats = new LinkedHashSet<>();
        for (SelectedEventClip clip : screen.selectionManager.selectedEventClips) {
            NumEventData event = screen.eventForClip(clip);
            if (event != null) {
                beats.add(event.startBeat());
                beats.add(event.endBeat());
            }
        }
        return beats;
    }

    SelectedEventHandle findNearestEventHandle(TrackData track, EventLaneType eventType, int x, int rowTop, int laneHeight, int width, double mouseX, double mouseY) {
        List<NumEventData> events = screen.eventsForLane(track, eventType);
        SelectedEventHandle best = null;
        double bestDistance = Double.MAX_VALUE;
        final double maxEdgeDistance = 8.0;
        final double maxCenterDistance = 12.0;
        for (int index = 0; index < events.size(); index++) {
            NumEventData event = events.get(index);
            int startX = screen.beatToScreen(x, width, event.startBeat());
            int endX = screen.beatToScreen(x, width, event.endBeat());
            boolean insideVertically = mouseY >= rowTop + 4 && mouseY <= rowTop + laneHeight - 4;
            if (insideVertically) {
                double startEdgeDistance = Math.abs(mouseX - startX);
                if (startEdgeDistance <= maxEdgeDistance && startEdgeDistance < bestDistance) {
                    bestDistance = startEdgeDistance;
                    best = new SelectedEventHandle(track, eventType, index, EventAnchor.START);
                }
                double endEdgeDistance = Math.abs(mouseX - endX);
                if (endEdgeDistance <= maxEdgeDistance && endEdgeDistance < bestDistance) {
                    bestDistance = endEdgeDistance;
                    best = new SelectedEventHandle(track, eventType, index, EventAnchor.END);
                }
            }
            double startCenterDistance = EditorUtils.squaredDistance(mouseX, mouseY, startX, rowTop + laneHeight / 2);
            if (startCenterDistance <= maxCenterDistance * maxCenterDistance && startCenterDistance < bestDistance * bestDistance) {
                bestDistance = Math.sqrt(startCenterDistance);
                best = new SelectedEventHandle(track, eventType, index, EventAnchor.START);
            }
            double endCenterDistance = EditorUtils.squaredDistance(mouseX, mouseY, endX, rowTop + laneHeight / 2);
            if (endCenterDistance <= maxCenterDistance * maxCenterDistance && endCenterDistance < bestDistance * bestDistance) {
                bestDistance = Math.sqrt(endCenterDistance);
                best = new SelectedEventHandle(track, eventType, index, EventAnchor.END);
            }
        }
        return best;
    }

    SelectedEventClip findEventClipAt(TrackData track, EventLaneType eventType, int x, int rowTop, int laneHeight, int width, double mouseX, double mouseY) {
        List<NumEventData> events = screen.eventsForLane(track, eventType);
        for (int index = 0; index < events.size(); index++) {
            NumEventData event = events.get(index);
            int startX = screen.beatToScreen(x, width, event.startBeat());
            int endX = Math.max(startX + 2, screen.beatToScreen(x, width, event.endBeat()));
            if (mouseX >= startX && mouseX <= endX && mouseY >= rowTop + 4 && mouseY <= rowTop + laneHeight - 4) {
                return new SelectedEventClip(track, eventType, index);
            }
        }
        return null;
    }

    void dragSelectedEventHandle(double mouseX, double mouseY, int timelineX, int timelineWidth, TimelineLaneLayout hoveredLayout) {
        if (screen.selectedEventHandle == null || hoveredLayout == null) {
            return;
        }
        TimelineLane lane = hoveredLayout.lane();
        if (lane.track != screen.selectedEventHandle.track || lane.eventType != screen.selectedEventHandle.eventType) {
            return;
        }
        List<NumEventData> events = screen.eventsForLane(screen.selectedEventHandle.track, screen.selectedEventHandle.eventType);
        if (screen.selectedEventHandle.eventIndex < 0 || screen.selectedEventHandle.eventIndex >= events.size()) {
            return;
        }
        NumEventData event = events.get(screen.selectedEventHandle.eventIndex);
        double[] range = screen.timeline.eventValueRange(events);
        double beat = applyTimelineBeatSnap(screen.screenToBeat(timelineX, timelineWidth, mouseX), timelineX, timelineWidth, selectedNoteBeats(), selectedEventClipEdges(), screen.selectedEventHandle.track);
        double progress = 1.0 - ((mouseY - (hoveredLayout.top() + 5.0)) / (hoveredLayout.height() - 10.0));
        progress = Math.max(0.0, Math.min(1.0, progress));
        double value = range[0] + (range[1] - range[0]) * progress;
        if (screen.selectedEventHandle.anchor == EventAnchor.START) {
            event.setStartBeat(Math.min(beat, event.endBeat() - 0.01));
            event.setStartValue(value);
        } else {
            event.setEndBeat(Math.max(beat, event.startBeat() + 0.01));
            event.setEndValue(value);
        }
        screen.state.sortCurrentLevel();
        int updatedIndex = screen.eventsForLane(screen.selectedEventHandle.track, screen.selectedEventHandle.eventType).indexOf(event);
        if (updatedIndex >= 0) {
            screen.selectedEventHandle = new SelectedEventHandle(screen.selectedEventHandle.track, screen.selectedEventHandle.eventType, updatedIndex, screen.selectedEventHandle.anchor);
            screen.selectedEventClip = new SelectedEventClip(screen.selectedEventHandle.track, screen.selectedEventHandle.eventType, updatedIndex);
        }
        screen.state.markDirty();
        screen.state.setStatus("Edited " + screen.selectedEventHandle.eventType.label + " " + screen.selectedEventHandle.anchor.label + " handle");
    }

    boolean splitEventAtBeat(TrackData track, EventLaneType eventType, double beat) {
        List<NumEventData> events = screen.eventsForLane(track, eventType);
        for (int index = 0; index < events.size(); index++) {
            NumEventData event = events.get(index);
            if (beat <= event.startBeat() + 0.01 || beat >= event.endBeat() - 0.01) {
                continue;
            }
            double progress = (beat - event.startBeat()) / Math.max(0.01, event.endBeat() - event.startBeat());
            double splitValue = EasingFunctions.getEase(event.startValue(), event.endValue(), progress, event.easingType());
            NumEventData tail = new NumEventData(beat, event.endBeat(), splitValue, event.endValue(), event.easingType());
            event.setEndBeat(beat);
            event.setEndValue(splitValue);
            events.add(index + 1, tail);
            screen.selectedEventHandle = new SelectedEventHandle(track, eventType, index + 1, EventAnchor.START);
            screen.selectedEventClip = new SelectedEventClip(track, eventType, index + 1);
            screen.state.setSelection(EditorSelection.track(track));
            screen.state.sortCurrentLevel();
            screen.state.markDirty();
            screen.state.setStatus("Split TrackID " + track.id() + " " + eventType.label + " at beat " + screen.propertyPanel.format(beat));
            return true;
        }
        return false;
    }

    private boolean isInsideTimelineZoomBar(double mouseX, double mouseY, int contentX, int contentWidth, int rulerY) {
        int zoomBarWidth = Math.min(TIMELINE_ZOOM_BAR_WIDTH, contentWidth - 10);
        return EditorUtils.isInside(mouseX, mouseY, contentX, rulerY + 2, zoomBarWidth, TIMELINE_ZOOM_BAR_HEIGHT);
    }

    void setTimelineZoomFromMouseX(double mouseX, int contentX, int contentWidth) {
        double progress = Math.max(0.1, Math.min(1.0, (mouseX - contentX) / Math.max(1.0, contentWidth)));
        double targetBeats = 4.0 + (1.0 - progress) * (128.0 - 4.0);
        double factor = targetBeats / screen.state.beatsPerScreen();
        screen.state.zoom(factor);
    }

    void startTimelineScrollbarDrag(double mouseX) {
        screen.timeline.startTimelineScrollbarDrag(mouseX);
    }

    void handleTimelineScrollbarDrag(double mouseX) {
        screen.timeline.handleTimelineScrollbarDrag(mouseX);
    }

    void clearDragState() {
        screen.dragMode = DragMode.NONE;
        screen.selectionBox = null;
        screen.snapGuideBeat = Double.NaN;
        screen.noteDragSnapshots.clear();
        screen.effectDragSnapshots.clear();
        screen.eventClipDragSnapshots.clear();
        screen.mapNoteDragOrigins.clear();
        screen.mapEffectDragOrigins.clear();
        screen.draggingHoldLengthNote = null;
        screen.dragAnchorHoldLength = 0.0;
        screen.draggingTrackEventEditor = null;
        screen.draggingTrackEventSourceIndex = -1;
        screen.draggingTrackEventTargetIndex = -1;
    }

    void endDrag() {
        screen.dragMode = DragMode.NONE;
        screen.selectionBox = null;
        screen.snapGuideBeat = Double.NaN;
        screen.noteDragSnapshots.clear();
        screen.effectDragSnapshots.clear();
        screen.eventClipDragSnapshots.clear();
        screen.mapNoteDragOrigins.clear();
        screen.mapEffectDragOrigins.clear();
        screen.draggingTrackEventEditor = null;
        screen.draggingTrackEventSourceIndex = -1;
        screen.draggingTrackEventTargetIndex = -1;
        screen.draggingHoldLengthNote = null;
        screen.dragAnchorHoldLength = 0.0;
    }
}

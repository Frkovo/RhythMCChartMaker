package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.core.EasingFunctions;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.Vec3Data;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class EditorSmoothingEngine {
    private final ChartEditorScreen screen;

    TextFieldWidget smoothingDivisionField;
    ButtonWidget smoothingKindButton;
    ButtonWidget smoothingNoteTypeButton;
    ButtonWidget smoothingFillButton;
    ButtonWidget smoothingApplyButton;
    ButtonWidget smoothingCancelButton;

    boolean smoothingPopupOpen;
    boolean smoothingUseCatmull = true;
    boolean smoothingUseCircle = false;
    EasingType smoothingEasingType = EasingType.LINEAR;
    NoteType smoothingNoteType = NoteType.TAP;
    boolean smoothingFillEnabled = true;

    EditorSmoothingEngine(ChartEditorScreen screen) {
        this.screen = screen;
    }

    void openSmoothingPopup() {
        List<SelectedNote> notes = screen.selectionManager.currentSelectedNotes();
        if (notes.size() < 2) {
            screen.state.setStatus("Select at least 2 notes before smoothing");
            return;
        }
        smoothingNoteType = notes.getFirst().note().noteType();
        TrackData track = notes.getFirst().track();
        smoothingDivisionField.setText(Integer.toString(track == null ? 16 : track.beatDivision()));
        smoothingKindButton.setMessage(Text.literal("Curve: " + smoothingCurveLabel()));
        smoothingNoteTypeButton.setMessage(Text.literal("Note: " + EditorUtils.shortNoteLabel(smoothingNoteType)));
        smoothingFillButton.setMessage(Text.literal(fillSmoothingLabel()));
        positionSmoothingPopupWidgets();
        smoothingPopupOpen = true;
        setSmoothingPopupWidgetsVisible(true);
        smoothingDivisionField.setFocused(true);
        screen.state.setStatus("Smoothing: choose curve, note type, and fill division");
    }

    void closeSmoothingPopup() {
        smoothingPopupOpen = false;
        setSmoothingPopupWidgetsVisible(false);
        smoothingDivisionField.setFocused(false);
    }

    void applySmoothingFromPopup() {
        List<SelectedNote> notes = screen.selectionManager.currentSelectedNotes();
        if (notes.size() < 2) {
            screen.state.setStatus("Smoothing needs at least 2 selected notes");
            closeSmoothingPopup();
            return;
        }
        int division = EditorUtils.clamp(EditorUtils.parseIntOrDefault(smoothingDivisionField.getText(), 16), 1, 256);
        double step = 4.0 / division;
        Map<TrackData, List<SelectedNote>> byTrack = new java.util.LinkedHashMap<>();
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
                for (double beat = Math.ceil(startBeat / step) * step; beat <= endBeat + ChartEditorScreen.NOTE_BEAT_EPSILON; beat += step) {
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
            screen.state.setStatus("Smoothing skipped: each track needs at least 2 selected notes");
            closeSmoothingPopup();
            return;
        }
        screen.state.sortCurrentLevel();
        screen.state.markDirty();
        screen.selectionManager.replaceSelectedNotes(new ArrayList<>(smoothedSelection));
        if (!smoothedSelection.isEmpty()) {
            SelectedNote primary = smoothedSelection.getFirst();
            screen.state.setSelection(EditorSelection.note(primary.track(), primary.note()));
        }
        screen.propertyPanel.populateFieldsFromSelection();
        screen.history.captureHistorySnapshotIfNeeded();
        closeSmoothingPopup();
        screen.state.setStatus("Smoothed " + changed + " note(s), filled " + filled + " note(s) at 1/" + division);
    }

    void relaxSelectedNotePositions(List<SelectedNote> group) {
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

    NoteData sampleSmoothedNote(List<SelectedNote> anchors, double beat, double step) {
        int segment = findSmoothingSegment(anchors, beat);
        NoteData left = anchors.get(segment).note();
        NoteData right = anchors.get(segment + 1).note();
        double span = Math.max(ChartEditorScreen.NOTE_BEAT_EPSILON, right.beat() - left.beat());
        double t = Math.max(0.0, Math.min(1.0, (beat - left.beat()) / span));
        double easedT = smoothingEase(t);
        Vec3Data pos = smoothingUseCircle
                ? sampleCirclePosition(anchors, buildSmoothingCircle(anchors), beat)
                : smoothingUseCatmull ? sampleCatmullPosition(anchors, segment, t) : EditorUtils.lerpVec(left.pos(), right.pos(), easedT);
        Vec3Data scale = EditorUtils.lerpVec(left.scale(), right.scale(), easedT);
        Vec3Data rotation = EditorUtils.lerpVec(left.rotation(), right.rotation(), easedT);
        if (smoothingNoteType == NoteType.HOLD) {
            pos.set(pos.x(), pos.y(), -1.0);
        }
        NoteData note = new NoteData(smoothingNoteType, beat, pos, scale, rotation, -1, smoothingNoteType == NoteType.HOLD ? step : 0.0);
        applySmoothingNoteType(note, step);
        return note;
    }

    int findSmoothingSegment(List<SelectedNote> anchors, double beat) {
        for (int index = 0; index < anchors.size() - 1; index++) {
            if (beat <= anchors.get(index + 1).note().beat() + ChartEditorScreen.NOTE_BEAT_EPSILON) {
                return index;
            }
        }
        return Math.max(0, anchors.size() - 2);
    }

    Vec3Data sampleCatmullPosition(List<SelectedNote> anchors, int segment, double t) {
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

    void applyCircleSelectedNotePositions(List<SelectedNote> group) {
        SmoothingCircle circle = buildSmoothingCircle(group);
        for (SelectedNote selectedNote : group) {
            Vec3Data pos = sampleCirclePosition(group, circle, selectedNote.note().beat());
            selectedNote.note().pos().set(pos.x(), pos.y(), smoothingNoteType == NoteType.HOLD ? -1.0 : pos.z());
        }
    }

    SmoothingCircle buildSmoothingCircle(List<SelectedNote> anchors) {
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

    double circleSignedArea(List<SelectedNote> anchors, double centerX, double centerY) {
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

    Vec3Data sampleCirclePosition(List<SelectedNote> anchors, SmoothingCircle circle, double beat) {
        int segment = findSmoothingSegment(anchors, beat);
        NoteData left = anchors.get(segment).note();
        NoteData right = anchors.get(segment + 1).note();
        double segmentSpan = Math.max(ChartEditorScreen.NOTE_BEAT_EPSILON, right.beat() - left.beat());
        double segmentT = Math.max(0.0, Math.min(1.0, (beat - left.beat()) / segmentSpan));
        double fullSpan = Math.max(ChartEditorScreen.NOTE_BEAT_EPSILON, anchors.getLast().note().beat() - anchors.getFirst().note().beat());
        double progress = Math.max(0.0, Math.min(1.0, (beat - anchors.getFirst().note().beat()) / fullSpan));
        double angle = circle.startAngle() + circle.sweep() * progress;
        return new Vec3Data(
                circle.centerX() + Math.cos(angle) * circle.radius(),
                circle.centerY() + Math.sin(angle) * circle.radius(),
                EditorUtils.lerp(left.pos().z(), right.pos().z(), segmentT)
        );
    }

    void applySmoothingNoteType(NoteData note, double step) {
        note.setNoteType(smoothingNoteType);
        if (smoothingNoteType == NoteType.HOLD) {
            note.pos().set(note.pos().x(), note.pos().y(), -1.0);
            note.setHoldLengthBeats(Math.max(step, note.holdLengthBeats()));
        } else {
            note.setHoldLengthBeats(0.0);
        }
    }

    boolean isAnchorBeat(List<SelectedNote> anchors, double beat) {
        for (SelectedNote anchor : anchors) {
            if (Math.abs(anchor.note().beat() - beat) < ChartEditorScreen.NOTE_BEAT_EPSILON) {
                return true;
            }
        }
        return false;
    }

    NoteData findNoteAtBeat(TrackData track, double beat) {
        for (NoteData note : track.notes()) {
            if (Math.abs(note.beat() - beat) < ChartEditorScreen.NOTE_BEAT_EPSILON) {
                return note;
            }
        }
        return null;
    }

    double smoothingEase(double t) {
        return smoothingUseCatmull || smoothingUseCircle ? t : EasingFunctions.getEase(t, smoothingEasingType);
    }

    double smoothingRelaxStrength() {
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

    String smoothingCurveLabel() {
        if (smoothingUseCatmull) {
            return "Catmull";
        }
        return smoothingUseCircle ? "Circle" : EditorUtils.trackEaseDisplayLabel(smoothingEasingType);
    }

    void advanceSmoothingCurve() {
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

    double catmull(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2.0 * p1) + (-p0 + p2) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    String fillSmoothingLabel() {
        return smoothingFillEnabled ? "Fill: On" : "Fill: Off";
    }

    int smoothingPopupX() {
        return Math.max(10, screen.publicWidth() / 2 - ChartEditorScreen.SMOOTHING_POPUP_WIDTH / 2);
    }

    int smoothingPopupY() {
        return Math.max(10, screen.publicHeight() / 2 - ChartEditorScreen.SMOOTHING_POPUP_HEIGHT / 2);
    }

    void createSmoothingPopupWidgets() {
        int popupX = smoothingPopupX();
        int popupY = smoothingPopupY();
        smoothingKindButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Curve: " + smoothingCurveLabel()), b -> {
            advanceSmoothingCurve();
            smoothingKindButton.setMessage(Text.literal("Curve: " + smoothingCurveLabel()));
        }).dimensions(popupX + 18, popupY + 54, 324, 18).build());
        smoothingNoteTypeButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Note: " + EditorUtils.shortNoteLabel(smoothingNoteType)), b -> {
            smoothingNoteType = EditorUtils.nextNoteType(smoothingNoteType);
            smoothingNoteTypeButton.setMessage(Text.literal("Note: " + EditorUtils.shortNoteLabel(smoothingNoteType)));
        }).dimensions(popupX + 18, popupY + 94, 98, 18).build());
        smoothingFillButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal(fillSmoothingLabel()), b -> {
            smoothingFillEnabled = !smoothingFillEnabled;
            smoothingFillButton.setMessage(Text.literal(fillSmoothingLabel()));
        }).dimensions(popupX + 128, popupY + 94, 98, 18).build());
        smoothingDivisionField = (TextFieldWidget) screen.publicAddDrawableChild(new TextFieldWidget(screen.getTextRenderer(), popupX + 238, popupY + 94, 104, 18, Text.literal("Division")));
        smoothingDivisionField.setMaxLength(4);
        smoothingDivisionField.setText("16");
        smoothingApplyButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> applySmoothingFromPopup())
                .dimensions(popupX + 176, popupY + 174, 76, 20).build());
        smoothingCancelButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> closeSmoothingPopup())
                .dimensions(popupX + 264, popupY + 174, 78, 20).build());
        setSmoothingPopupWidgetsVisible(false);
    }

    void setSmoothingPopupWidgetsVisible(boolean visible) {
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

    void positionSmoothingPopupWidgets() {
        int popupX = smoothingPopupX();
        int popupY = smoothingPopupY();
        smoothingKindButton.setPosition(popupX + 18, popupY + 54);
        smoothingNoteTypeButton.setPosition(popupX + 18, popupY + 94);
        smoothingFillButton.setPosition(popupX + 128, popupY + 94);
        smoothingDivisionField.setPosition(popupX + 238, popupY + 94);
        smoothingApplyButton.setPosition(popupX + 176, popupY + 174);
        smoothingCancelButton.setPosition(popupX + 264, popupY + 174);
    }
}

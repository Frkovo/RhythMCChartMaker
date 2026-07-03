package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.Vec3Data;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.DragMode;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.MapSelection;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.NoteAxis;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.SelectedNote;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class EditorSceneMap {
    private final ChartEditorScreen screen;

    EditorSceneMap(ChartEditorScreen screen) {
        this.screen = screen;
    }

    static double mapScale(int width) {
        return Math.max(6.0, width / 22.0);
    }

    static double[] computeGridSteps(double visibleHalfWorld) {
        if (visibleHalfWorld <= 0.0 || !Double.isFinite(visibleHalfWorld)) {
            return new double[]{1.0, 0.5};
        }
        double rough = visibleHalfWorld / 4.0;
        double log = Math.log10(rough);
        double pow = Math.pow(10.0, Math.floor(log));
        double ratio = rough / pow;
        double major;
        if (ratio < Math.sqrt(2.0)) {
            major = pow;
        } else if (ratio < Math.sqrt(10.0)) {
            major = 2.0 * pow;
        } else {
            major = 5.0 * pow;
        }
        return new double[]{major, major / 5.0};
    }

    static double snapToGrid(double value, double gridStep) {
        if (gridStep <= 0.0 || !Double.isFinite(gridStep)) {
            return value;
        }
        return Math.round(value / gridStep) * gridStep;
    }

    boolean handleSceneMapAreaClick(double mouseX, double mouseY, int x, int y) {
        int mapSize = screen.leftPanelWidth() - 20;
        int mapX = x + 10;
        int mapY = y + 28;
        if (!EditorUtils.isInside(mouseX, mouseY, mapX, mapY, mapSize, mapSize)) {
            return false;
        }
        if (handleCurrentFrameMapOverlayClick(mouseX, mouseY, mapX, mapY, mapSize, mapSize)) {
            return true;
        }
        return handleCurrentFrameMapClick(mouseX, mouseY, mapX, mapY, mapSize, mapSize);
    }

    boolean handleCurrentFrameMapClick(double mouseX, double mouseY, int x, int y, int width, int height) {
        MapSelection best = null;
        for (MapSelection candidate : collectCurrentFrameMapSelections(x, y, width, height)) {
            double distance = EditorUtils.squaredDistance(mouseX, mouseY, candidate.x, candidate.y);
            if (best == null || distance < best.distance) {
                best = new MapSelection(candidate.kind, candidate.track, candidate.note, candidate.effect, candidate.x, candidate.y, distance);
            }
        }
        if (best == null || best.distance > 196.0) {
            return false;
        }
        if (best.kind == EditorSelection.Kind.NOTE) {
            screen.selectionManager.updateNoteSelectionFromClick(new SelectedNote(best.track, best.note), screen.isControlDown());
            screen.state.setSelection(EditorSelection.note(best.track, best.note));
            initializeCurrentFrameMapNoteDrag(mouseX, mouseY);
            screen.dragMode = DragMode.MAP_NOTE;
            screen.state.setStatus("Selected note from current-frame map");
        } else if (best.kind == EditorSelection.Kind.EFFECT) {
            screen.selectionManager.selectedNotes.clear();
            screen.selectionManager.selectedEventClips.clear();
            screen.state.setSelection(EditorSelection.effect(best.effect));
            initializeCurrentFrameMapEffectDrag(mouseX, mouseY, best.effect);
            screen.dragMode = DragMode.MAP_EFFECT;
            screen.state.setStatus("Selected effect from current-frame map");
        }
        return true;
    }

    boolean handleCurrentFrameMapOverlayClick(double mouseX, double mouseY, int x, int y, int width, int height) {
        SceneMapOverlayUi.Model overlayModel = buildSceneMapOverlayModel(x, y, width, height);
        if (overlayModel == null) {
            return false;
        }
        if (!EditorUtils.isInside(mouseX, mouseY, overlayModel.x(), overlayModel.y(), overlayModel.width(), overlayModel.height())) {
            return false;
        }
        int buttonIndex = SceneMapOverlayUi.hitTest(overlayModel, mouseX, mouseY);
        if (buttonIndex < 0) {
            return true;
        }
        if (screen.state.selection().note() != null && screen.state.selection().note().noteType() == NoteType.HOLD) {
            centerSelectedMapXY();
            return true;
        }
        if (buttonIndex == 0) {
            adjustSelectedMapAxis(NoteAxis.Z, -0.25);
        } else if (buttonIndex == 1) {
            adjustSelectedMapAxis(NoteAxis.Z, 0.25);
        } else {
            centerSelectedMapXY();
        }
        return true;
    }

    void handleCurrentFrameMapDrag(double mouseX, double mouseY, int x, int y, int width, int height) {
        double scale = mapScale(width) * screen.sceneMapZoom;
        double deltaX = (mouseX - screen.mapDragAnchorMouseX) / scale;
        double deltaY = -(mouseY - screen.mapDragAnchorMouseY) / scale;
        double visibleHalfWorld = (width / 2.0) / scale;
        double gridStep = computeGridSteps(visibleHalfWorld)[1];
        if (screen.dragMode == DragMode.MAP_NOTE && !screen.mapNoteDragOrigins.isEmpty()) {
            for (Map.Entry<SelectedNote, Vec3Data> entry : screen.mapNoteDragOrigins.entrySet()) {
                SelectedNote selectedNote = entry.getKey();
                Vec3Data origin = entry.getValue();
                double nx = snapToGrid(origin.x() + deltaX, gridStep);
                double ny = snapToGrid(origin.y() + deltaY, gridStep);
                selectedNote.note().pos().set(nx, ny, selectedNote.note().pos().z());
            }
            screen.state.markDirty();
            screen.propertyPanel.populateFieldsFromSelection();
            screen.state.setStatus("Map move X/Y " + EditorUtils.format(deltaX) + ", " + EditorUtils.format(deltaY));
            return;
        }
        if (screen.dragMode == DragMode.MAP_EFFECT && screen.state.selection().effect() != null) {
            EffectData effect = screen.state.selection().effect();
            Vec3Data origin = screen.mapEffectDragOrigins.get(effect);
            if (origin == null) {
                return;
            }
            JsonObject properties = effect.properties() == null ? new JsonObject() : effect.properties();
            properties.addProperty("x", snapToGrid(origin.x() + deltaX, gridStep));
            properties.addProperty("y", snapToGrid(origin.y() + deltaY, gridStep));
            properties.addProperty("z", origin.z());
            effect.setProperties(properties);
            screen.state.markDirty();
            screen.propertyPanel.populateFieldsFromSelection();
            screen.state.setStatus("Map move effect X/Y " + EditorUtils.format(deltaX) + ", " + EditorUtils.format(deltaY));
        }
    }

    void initializeCurrentFrameMapNoteDrag(double mouseX, double mouseY) {
        screen.mapNoteDragOrigins.clear();
        screen.mapEffectDragOrigins.clear();
        if (screen.selectionManager.selectedNotes.isEmpty() && screen.state.selection().track() != null && screen.state.selection().note() != null) {
            screen.selectionManager.selectedNotes.add(new SelectedNote(screen.state.selection().track(), screen.state.selection().note()));
        }
        for (SelectedNote selectedNote : screen.selectionManager.selectedNotes) {
            screen.mapNoteDragOrigins.put(selectedNote, selectedNote.note().pos().copy());
        }
        screen.mapDragAnchorMouseX = mouseX;
        screen.mapDragAnchorMouseY = mouseY;
    }

    void initializeCurrentFrameMapEffectDrag(double mouseX, double mouseY, EffectData effect) {
        screen.mapNoteDragOrigins.clear();
        screen.mapEffectDragOrigins.clear();
        JsonObject properties = effect.properties() == null ? new JsonObject() : effect.properties();
        screen.mapEffectDragOrigins.put(effect, new Vec3Data(
                EditorUtils.getDouble(properties, "x", 0.0),
                EditorUtils.getDouble(properties, "y", 0.0),
                EditorUtils.getDouble(properties, "z", 0.0)
        ));
        screen.mapDragAnchorMouseX = mouseX;
        screen.mapDragAnchorMouseY = mouseY;
    }

    List<MapSelection> collectCurrentFrameMapSelections(int x, int y, int width, int height) {
        List<MapSelection> selections = new ArrayList<>();
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        double beat = screen.state.playheadBeat();
        double scale = mapScale(width) * screen.sceneMapZoom;
        for (TrackData track : screen.state.visibleTracks()) {
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
        for (EffectData effect : screen.state.triggeredEffects()) {
            JsonObject properties = effect.properties();
            selections.add(new MapSelection(
                    EditorSelection.Kind.EFFECT,
                    null,
                    null,
                    effect,
                    centerX + (int) Math.round(EditorUtils.getDouble(properties, "x", 0.0) * scale),
                    centerY - (int) Math.round(EditorUtils.getDouble(properties, "y", 0.0) * scale),
                    0.0
            ));
        }
        return selections;
    }

    SceneMapOverlayUi.Model buildSceneMapOverlayModel(int x, int y, int width, int height) {
        if (screen.state.selection().note() == null && screen.state.selection().effect() == null) {
            return null;
        }
        int overlayX = x + 4;
        int overlayY = y + height - ChartEditorScreen.MAP_OVERLAY_HEIGHT;
        int overlayWidth = width - 8;
        int overlayHeight = ChartEditorScreen.MAP_OVERLAY_HEIGHT - 4;
        int startX = overlayX + overlayWidth - (ChartEditorScreen.MAP_OVERLAY_BUTTON_WIDTH + 6) * 3 - 8;
        List<SceneMapOverlayUi.Button> buttons = new ArrayList<>();
        buttons.add(new SceneMapOverlayUi.Button("Z-", startX, overlayY + 24, ChartEditorScreen.MAP_OVERLAY_BUTTON_WIDTH, 16));
        buttons.add(new SceneMapOverlayUi.Button("Z+", startX + ChartEditorScreen.MAP_OVERLAY_BUTTON_WIDTH + 6, overlayY + 24, ChartEditorScreen.MAP_OVERLAY_BUTTON_WIDTH, 16));
        buttons.add(new SceneMapOverlayUi.Button("Center", startX + (ChartEditorScreen.MAP_OVERLAY_BUTTON_WIDTH + 6) * 2, overlayY + 24, ChartEditorScreen.MAP_OVERLAY_BUTTON_WIDTH, 16));

        if (screen.state.selection().note() != null) {
            NoteData note = screen.state.selection().note();
            String title = EditorUtils.shortNoteLabel(note.noteType()) + "  X " + EditorUtils.format(note.pos().x()) + "  Y " + EditorUtils.format(note.pos().y()) + "  Z " + EditorUtils.format(note.pos().z());
            String detail = note.noteType() == NoteType.HOLD ? "HOLD keeps fixed Z depth; drag map for X/Y." : "Use Scene Map drag for X/Y, buttons for quick depth/center.";
            if (note.noteType() == NoteType.HOLD) {
                buttons = List.of(buttons.get(2));
            }
            return new SceneMapOverlayUi.Model(overlayX, overlayY, overlayWidth, overlayHeight, title, detail, buttons);
        }

        EffectData effect = screen.state.selection().effect();
        JsonObject properties = effect == null || effect.properties() == null ? new JsonObject() : effect.properties();
        String title = EditorUtils.shortEffectLabel(effect.effectType()) + "  X " + EditorUtils.format(EditorUtils.getDouble(properties, "x", 0.0)) + "  Y " + EditorUtils.format(EditorUtils.getDouble(properties, "y", 0.0)) + "  Z " + EditorUtils.format(EditorUtils.getDouble(properties, "z", 0.0));
        return new SceneMapOverlayUi.Model(overlayX, overlayY, overlayWidth, overlayHeight, title, "Use Scene Map drag for X/Y, buttons for quick depth/center.", buttons);
    }

    void adjustSelectedMapAxis(NoteAxis axis, double delta) {
        if (screen.state.selection().note() != null) {
            NoteData note = screen.state.selection().note();
            if (axis == NoteAxis.Z && note.noteType() == NoteType.HOLD) {
                screen.state.setStatus("HOLD uses fixed Z depth");
                return;
            }
            EditorUtils.setNoteAxisValue(note, axis, EditorUtils.noteAxisValue(note, axis) + delta);
            screen.state.markDirty();
            screen.propertyPanel.populateFieldsFromSelection();
            screen.propertyPanel.layoutPropertyFields();
            screen.state.setStatus("Note " + axis.name() + " " + EditorUtils.format(EditorUtils.noteAxisValue(note, axis)));
            return;
        }
        if (screen.state.selection().effect() != null) {
            EffectData effect = screen.state.selection().effect();
            JsonObject properties = effect.properties() == null ? new JsonObject() : effect.properties();
            String key = switch (axis) {
                case X -> "x";
                case Y -> "y";
                case Z -> "z";
            };
            double next = EditorUtils.getDouble(properties, key, 0.0) + delta;
            properties.addProperty(key, next);
            effect.setProperties(properties);
            screen.state.markDirty();
            screen.propertyPanel.populateFieldsFromSelection();
            screen.propertyPanel.layoutPropertyFields();
            screen.state.setStatus("Effect " + axis.name() + " " + EditorUtils.format(next));
        }
    }

    void centerSelectedMapXY() {
        if (screen.state.selection().note() != null) {
            NoteData note = screen.state.selection().note();
            note.pos().set(0.0, 0.0, note.noteType() == NoteType.HOLD ? -1.0 : note.pos().z());
            screen.state.markDirty();
            screen.propertyPanel.populateFieldsFromSelection();
            screen.propertyPanel.layoutPropertyFields();
            screen.state.setStatus("Centered note X/Y");
            return;
        }
        if (screen.state.selection().effect() != null) {
            EffectData effect = screen.state.selection().effect();
            JsonObject properties = effect.properties() == null ? new JsonObject() : effect.properties();
            properties.addProperty("x", 0.0);
            properties.addProperty("y", 0.0);
            effect.setProperties(properties);
            screen.state.markDirty();
            screen.propertyPanel.populateFieldsFromSelection();
            screen.propertyPanel.layoutPropertyFields();
            screen.state.setStatus("Centered effect X/Y");
        }
    }
}

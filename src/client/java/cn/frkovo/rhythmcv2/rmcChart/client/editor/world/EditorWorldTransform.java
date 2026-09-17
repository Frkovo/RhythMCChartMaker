package cn.frkovo.rhythmcv2.rmcChart.client.editor.world;

import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator.NoteState;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator.TrackState;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorState;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.EditorSelection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/**
 * Moves the selected note along the camera plane in response to screen-space pixel deltas.
 * Pixel deltas are scaled by the note's depth so the note tracks the cursor 1:1.
 */
public final class EditorWorldTransform {
    private static final double GRID_STEP = 0.25;
    private static final double MIN_DEPTH = 0.5;
    private static final double MIN_SCALE = 1.0e-6;

    private EditorWorldTransform() {
    }

    /**
     * Applies one drag increment to the selected note.
     *
     * @return true when a note was actually moved.
     */
    public static boolean dragSelectedNote(ChartEditorState state, MinecraftClient client, double deltaXPx, double deltaYPx) {
        if (state == null || client == null || client.options == null || client.getWindow() == null) {
            return false;
        }
        EditorSelection selection = state.selection();
        if (selection == null || selection.kind() != EditorSelection.Kind.NOTE
                || selection.track() == null || selection.note() == null) {
            return false;
        }
        if (!EditorWorldPlacement.hasAnchor()) {
            return false;
        }
        Camera camera = client.gameRenderer.getCamera();
        if (camera == null) {
            return false;
        }

        TrackState trackState = ChartEvaluator.evaluateTrack(selection.track(),
                state.level().meta().bpms(), state.playheadBeat());
        NoteState noteState = ChartEvaluator.evaluateNote(trackState,
                state.level().meta().bpms(), selection.note());
        Vec3d noteWorld = EditorWorldPlacement.chartToWorld(noteState.worldX(), noteState.worldY(), noteState.worldZ());
        Vec3d camPos = camera.getCameraPos();

        // Camera basis in world space, derived from yaw/pitch (MC: yaw 0 = +Z/south, pitch +90 = straight down).
        double yawRad = Math.toRadians(camera.getYaw());
        double pitchRad = Math.toRadians(camera.getPitch());
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);
        double sinPitch = Math.sin(pitchRad);
        double cosPitch = Math.cos(pitchRad);
        Vec3d forward = new Vec3d(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
        Vec3d right = new Vec3d(-cosYaw, 0.0, -sinYaw);
        Vec3d up = right.crossProduct(forward);

        double depth = Math.max(MIN_DEPTH, noteWorld.subtract(camPos).dotProduct(forward));
        double fovRad = Math.toRadians(client.options.getFov().getValue());
        int viewportHeight = Math.max(1, client.getWindow().getScaledHeight());
        double worldPerPixel = depth * 2.0 * Math.tan(fovRad / 2.0) / viewportHeight;

        Vec3d worldDelta = right.multiply(deltaXPx * worldPerPixel).add(up.multiply(-deltaYPx * worldPerPixel));
        if (worldDelta.lengthSquared() < 1.0e-12) {
            return true;
        }

        // World delta -> chart delta: translation cancels, z sign flips (chartToWorld negates z).
        Vec3d chartDelta = new Vec3d(worldDelta.x, worldDelta.y, -worldDelta.z);
        Vec3d local = EditorWorldPlacement.rotateInverse(chartDelta.x, chartDelta.y, chartDelta.z,
                trackState.xRot(), trackState.yRot(), trackState.zRot());
        double dx = local.x / safeScale(trackState.xScale());
        double dy = local.y / safeScale(trackState.yScale());
        double dz = local.z / safeScale(trackState.zScale());

        NoteData note = selection.note();
        boolean hold = note.noteType() == NoteType.HOLD;
        note.pos().set(
                snap(note.pos().x() + dx),
                snap(note.pos().y() + dy),
                hold ? -1.0 : snap(note.pos().z() + dz));
        state.markDirty();
        return true;
    }

    private static double safeScale(double scale) {
        return Math.abs(scale) < MIN_SCALE ? 1.0 : scale;
    }

    private static double snap(double value) {
        return Math.round(value / GRID_STEP) * GRID_STEP;
    }
}

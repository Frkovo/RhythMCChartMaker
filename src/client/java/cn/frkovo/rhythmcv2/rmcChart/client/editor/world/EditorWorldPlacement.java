package cn.frkovo.rhythmcv2.rmcChart.client.editor.world;

import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator.TrackState;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Client-side world placement math.
 *
 * <p>The preview server teleports the player to the arena game center during
 * initialization. We capture the note origin anchor from the player position at
 * PREVIEW_READY: anchor = feet + (0, 0.5, -1.5), matching the server's
 * {@code screenCenterLoc + (0, 0.5, 0)} note origin. This is a heuristic; a
 * dedicated anchor packet is a documented follow-up.
 *
 * <p>Chart→world mapping (mirrors server NoteObject): a chart-space vector
 * (cx, cy, cz) maps to world (anchorX + cx, anchorY + cy, anchorZ - cz).
 */
public final class EditorWorldPlacement {
    private static final double MAX_PLACE_DISTANCE = 64.0;
    private static final double GRID_STEP = 0.25;

    private static double anchorX;
    private static double anchorY;
    private static double anchorZ;
    private static boolean anchorValid;

    private EditorWorldPlacement() {
    }

    public static void captureAnchorFromPlayer(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }
        Vec3d feet = client.player.getEntityPos();
        anchorX = feet.x;
        anchorY = feet.y + 0.5;
        anchorZ = feet.z - 1.5;
        anchorValid = true;
    }

    public static void invalidateAnchor() {
        anchorValid = false;
    }

    public static boolean hasAnchor() {
        return anchorValid;
    }

    public static double anchorX() { return anchorX; }
    public static double anchorY() { return anchorY; }
    public static double anchorZ() { return anchorZ; }

    /** Chart-space vector to world position. */
    public static Vec3d chartToWorld(double cx, double cy, double cz) {
        return new Vec3d(anchorX + cx, anchorY + cy, anchorZ - cz);
    }

    /**
     * Intersects the player crosshair ray with the selected track's hit plane at
     * the given beat and converts the hit into chart note coordinates.
     *
     * @return chart-space {posX, posY} (grid-snapped), or null when no valid hit.
     */
    public static double[] crosshairToNotePos(MinecraftClient client, TrackData track,
                                              List<BpmPoint> bpms, double beat) {
        if (!anchorValid || client == null || client.player == null || track == null) {
            return null;
        }
        TrackState ts = ChartEvaluator.evaluateTrack(track, bpms, beat);
        if (Math.abs(ts.xScale()) < 1.0e-6 || Math.abs(ts.yScale()) < 1.0e-6) {
            return null;
        }

        // Track base (hit plane origin) in world.
        Vec3d planeOrigin = chartToWorld(ts.xTransform(), ts.yTransform(), ts.zTransform());
        // Hit plane normal: rotated chart +Z axis, mapped to world (Z negated).
        Vec3d normalChart = rotateForward(0.0, 0.0, 1.0, ts.xRot(), ts.yRot(), ts.zRot());
        Vec3d normal = new Vec3d(normalChart.x, normalChart.y, -normalChart.z);

        Vec3d eye = client.player.getEyePos();
        Vec3d dir = client.player.getRotationVec(1.0f).normalize();
        double denom = dir.dotProduct(normal);
        if (Math.abs(denom) < 1.0e-6) {
            return null;
        }
        double t = planeOrigin.subtract(eye).dotProduct(normal) / denom;
        if (t < 0.0 || t > MAX_PLACE_DISTANCE) {
            return null;
        }
        Vec3d hit = eye.add(dir.multiply(t));

        // World → chart (undo Z negation), then remove track transform.
        double vx = hit.x - anchorX - ts.xTransform();
        double vy = hit.y - anchorY - ts.yTransform();
        double vz = (anchorZ - hit.z) - ts.zTransform();
        Vec3d local = rotateInverse(vx, vy, vz, ts.xRot(), ts.yRot(), ts.zRot());

        double posX = snap(local.x / ts.xScale());
        double posY = snap(local.y / ts.yScale());
        return new double[]{posX, posY};
    }

    /** Forward rotation identical to ChartEvaluator: Rz, then Ry, then Rx. */
    private static Vec3d rotateForward(double x, double y, double z, double xRot, double yRot, double zRot) {
        double zRad = Math.toRadians(zRot);
        double yRad = Math.toRadians(yRot);
        double xRad = Math.toRadians(xRot);

        double x1 = x * Math.cos(zRad) - y * Math.sin(zRad);
        double y1 = x * Math.sin(zRad) + y * Math.cos(zRad);
        double z1 = z;

        double x2 = x1 * Math.cos(yRad) + z1 * Math.sin(yRad);
        double y2 = y1;
        double z2 = -x1 * Math.sin(yRad) + z1 * Math.cos(yRad);

        double localX = x2;
        double localY = y2 * Math.cos(xRad) - z2 * Math.sin(xRad);
        double localZ = y2 * Math.sin(xRad) + z2 * Math.cos(xRad);
        return new Vec3d(localX, localY, localZ);
    }

    /** Inverse of {@link #rotateForward}: Rx(-xRot), then Ry(-yRot), then Rz(-zRot). */
    public static Vec3d rotateInverse(double x, double y, double z, double xRot, double yRot, double zRot) {
        // Undo Rx.
        double xRad = Math.toRadians(xRot);
        double y1 = y * Math.cos(xRad) + z * Math.sin(xRad);
        double z1 = -y * Math.sin(xRad) + z * Math.cos(xRad);
        double x1 = x;
        // Undo Ry.
        double yRad = Math.toRadians(yRot);
        double x2 = x1 * Math.cos(yRad) - z1 * Math.sin(yRad);
        double z2 = x1 * Math.sin(yRad) + z1 * Math.cos(yRad);
        double y2 = y1;
        // Undo Rz.
        double zRad = Math.toRadians(zRot);
        double x3 = x2 * Math.cos(zRad) + y2 * Math.sin(zRad);
        double y3 = -x2 * Math.sin(zRad) + y2 * Math.cos(zRad);
        return new Vec3d(x3, y3, z2);
    }

    private static double snap(double value) {
        return Math.round(value / GRID_STEP) * GRID_STEP;
    }
}

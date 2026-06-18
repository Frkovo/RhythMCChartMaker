package cn.frkovo.rhythmcv2.rmcChart.chart.core;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;

public final class ChartEvaluator {
    private ChartEvaluator() {
    }

    public static TrackState evaluateTrack(TrackData track, double beat) {
        return new TrackState(
                track,
                beat,
                ChartMath.getDistance(track.speedEvents(), beat),
                ChartMath.getTransformation(track.xTransformEvents(), beat, 0.0),
                ChartMath.getTransformation(track.yTransformEvents(), beat, 0.0),
                ChartMath.getTransformation(track.zTransformEvents(), beat, 0.0),
                ChartMath.getTransformation(track.xRotateEvents(), beat, 0.0),
                ChartMath.getTransformation(track.yRotateEvents(), beat, 0.0),
                ChartMath.getTransformation(track.zRotateEvents(), beat, 0.0),
                ChartMath.getTransformation(track.xScaleEvents(), beat, 1.0),
                ChartMath.getTransformation(track.yScaleEvents(), beat, 1.0),
                ChartMath.getTransformation(track.zScaleEvents(), beat, 1.0)
        );
    }

    public static NoteState evaluateNote(TrackState trackState, NoteData note) {
        double noteDistance = ChartMath.getDistance(trackState.track().speedEvents(), note.beat());
        double distanceToHitPlane = noteDistance - trackState.distance() + note.pos().z() * trackState.zScale();

        double x = note.pos().x() * trackState.xScale();
        double y = note.pos().y() * trackState.yScale();
        double z = distanceToHitPlane;

        double zRad = Math.toRadians(trackState.zRot());
        double yRad = Math.toRadians(trackState.yRot());
        double xRad = Math.toRadians(trackState.xRot());

        double x1 = x * Math.cos(zRad) - y * Math.sin(zRad);
        double y1 = x * Math.sin(zRad) + y * Math.cos(zRad);
        double z1 = z;

        double x2 = x1 * Math.cos(yRad) + z1 * Math.sin(yRad);
        double y2 = y1;
        double z2 = -x1 * Math.sin(yRad) + z1 * Math.cos(yRad);

        double localX = x2;
        double localY = y2 * Math.cos(xRad) - z2 * Math.sin(xRad);
        double localZ = y2 * Math.sin(xRad) + z2 * Math.cos(xRad);

        return new NoteState(
                note,
                trackState,
                distanceToHitPlane,
                localX,
                localY,
                localZ,
                localX + trackState.xTransform(),
                localY + trackState.yTransform(),
                localZ + trackState.zTransform(),
                note.rotation().x() + trackState.xRot(),
                note.rotation().y() + trackState.yRot(),
                note.rotation().z() + trackState.zRot(),
                note.scale().x() * trackState.xScale(),
                note.scale().y() * trackState.yScale(),
                note.scale().z() * trackState.zScale()
        );
    }

    public record TrackState(TrackData track, double beat, double distance,
                             double xTransform, double yTransform, double zTransform,
                             double xRot, double yRot, double zRot,
                             double xScale, double yScale, double zScale) {
    }

    public record NoteState(NoteData note, TrackState trackState, double distanceToHitPlane,
                            double localX, double localY, double localZ,
                            double worldX, double worldY, double worldZ,
                            double rotX, double rotY, double rotZ,
                            double scaleX, double scaleY, double scaleZ) {
        public boolean isWithinTypeRange() {
            return distanceToHitPlane >= note.noteType().zNear() && distanceToHitPlane <= note.noteType().zFar();
        }
    }
}

package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;

public final class NoteDragSnapshot {
    public final TrackData track;
    public final double beat;
    public final double projectionValue;
    public final double radius;

    public NoteDragSnapshot(TrackData track, double beat, double projectionValue, double radius) {
        this.track = track;
        this.beat = beat;
        this.projectionValue = projectionValue;
        this.radius = radius;
    }

    public TrackData track() { return track; }
    public double beat() { return beat; }
    public double projectionValue() { return projectionValue; }
    public double radius() { return radius; }
}

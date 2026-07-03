package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;

public final class SelectedEventClip {
    public final TrackData track;
    public final EventLaneType eventType;
    public final int eventIndex;

    public SelectedEventClip(TrackData track, EventLaneType eventType, int eventIndex) {
        this.track = track;
        this.eventType = eventType;
        this.eventIndex = eventIndex;
    }

    public TrackData track() { return track; }
    public EventLaneType eventType() { return eventType; }
    public int eventIndex() { return eventIndex; }
}

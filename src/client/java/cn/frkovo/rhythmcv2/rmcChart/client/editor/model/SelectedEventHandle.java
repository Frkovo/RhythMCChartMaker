package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;

public final class SelectedEventHandle {
    public final TrackData track;
    public final EventLaneType eventType;
    public final int eventIndex;
    public final EventAnchor anchor;

    public SelectedEventHandle(TrackData track, EventLaneType eventType, int eventIndex, EventAnchor anchor) {
        this.track = track;
        this.eventType = eventType;
        this.eventIndex = eventIndex;
        this.anchor = anchor;
    }

    public TrackData track() { return track; }
    public EventLaneType eventType() { return eventType; }
    public int eventIndex() { return eventIndex; }
    public EventAnchor anchor() { return anchor; }
}

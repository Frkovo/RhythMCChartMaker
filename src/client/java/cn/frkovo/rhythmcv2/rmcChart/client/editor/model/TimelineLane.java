package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;

public final class TimelineLane {
    public final LaneType type;
    public final TrackData track;
    public final EffectData effect;
    public final EventLaneType eventType;
    public final NoteAxis noteAxis;
    public final String eventGroup;

    public TimelineLane(LaneType type, TrackData track, EffectData effect, EventLaneType eventType, NoteAxis noteAxis, String eventGroup) {
        this.type = type;
        this.track = track;
        this.effect = effect;
        this.eventType = eventType;
        this.noteAxis = noteAxis;
        this.eventGroup = eventGroup;
    }

    public LaneType type() { return type; }
    public TrackData track() { return track; }
    public EffectData effect() { return effect; }
    public EventLaneType eventType() { return eventType; }
    public NoteAxis noteAxis() { return noteAxis; }
    public String eventGroup() { return eventGroup; }
}

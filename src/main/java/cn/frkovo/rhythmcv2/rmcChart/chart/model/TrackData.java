package cn.frkovo.rhythmcv2.rmcChart.chart.model;

import java.util.ArrayList;
import java.util.List;

public class TrackData {
    private int id;
    private int beatDivision = 16;
    private final List<NumEventData> speedEvents = new ArrayList<>();
    private final List<NumEventData> xTransformEvents = new ArrayList<>();
    private final List<NumEventData> yTransformEvents = new ArrayList<>();
    private final List<NumEventData> zTransformEvents = new ArrayList<>();
    private final List<NumEventData> xRotateEvents = new ArrayList<>();
    private final List<NumEventData> yRotateEvents = new ArrayList<>();
    private final List<NumEventData> zRotateEvents = new ArrayList<>();
    private final List<NumEventData> xScaleEvents = new ArrayList<>();
    private final List<NumEventData> yScaleEvents = new ArrayList<>();
    private final List<NumEventData> zScaleEvents = new ArrayList<>();
    private final List<NoteData> notes = new ArrayList<>();

    public TrackData(int id) {
        this.id = id;
    }

    public static TrackData createDefault(int id) {
        TrackData track = new TrackData(id);
        track.speedEvents.add(new NumEventData(0.0, 4096.0, 8.0, 8.0, EasingType.LINEAR));
        track.xScaleEvents.add(new NumEventData(0.0, 4096.0, 1.0, 1.0, EasingType.LINEAR));
        track.yScaleEvents.add(new NumEventData(0.0, 4096.0, 1.0, 1.0, EasingType.LINEAR));
        track.zScaleEvents.add(new NumEventData(0.0, 4096.0, 1.0, 1.0, EasingType.LINEAR));
        return track;
    }

    public int id() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int beatDivision() {
        return beatDivision;
    }

    public void setBeatDivision(int beatDivision) {
        if (beatDivision <= 0) {
            this.beatDivision = 16;
            return;
        }
        this.beatDivision = Math.max(1, Math.min(256, beatDivision));
    }

    public List<NumEventData> speedEvents() {
        return speedEvents;
    }

    public List<NumEventData> xTransformEvents() {
        return xTransformEvents;
    }

    public List<NumEventData> yTransformEvents() {
        return yTransformEvents;
    }

    public List<NumEventData> zTransformEvents() {
        return zTransformEvents;
    }

    public List<NumEventData> xRotateEvents() {
        return xRotateEvents;
    }

    public List<NumEventData> yRotateEvents() {
        return yRotateEvents;
    }

    public List<NumEventData> zRotateEvents() {
        return zRotateEvents;
    }

    public List<NumEventData> xScaleEvents() {
        return xScaleEvents;
    }

    public List<NumEventData> yScaleEvents() {
        return yScaleEvents;
    }

    public List<NumEventData> zScaleEvents() {
        return zScaleEvents;
    }

    public List<NoteData> notes() {
        return notes;
    }
}

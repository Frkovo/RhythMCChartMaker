package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.EditorSelection;

public final class MapSelection {
    public final EditorSelection.Kind kind;
    public final TrackData track;
    public final NoteData note;
    public final EffectData effect;
    public final int x;
    public final int y;
    public final double distance;

    public MapSelection(EditorSelection.Kind kind, TrackData track, NoteData note, EffectData effect, int x, int y, double distance) {
        this.kind = kind;
        this.track = track;
        this.note = note;
        this.effect = effect;
        this.x = x;
        this.y = y;
        this.distance = distance;
    }

    public EditorSelection.Kind kind() { return kind; }
    public TrackData track() { return track; }
    public NoteData note() { return note; }
    public EffectData effect() { return effect; }
    public int x() { return x; }
    public int y() { return y; }
    public double distance() { return distance; }
}

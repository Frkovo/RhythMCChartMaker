package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;

public final class SelectedNote {
    public final TrackData track;
    public final NoteData note;

    public SelectedNote(TrackData track, NoteData note) {
        this.track = track;
        this.note = note;
    }

    public TrackData track() { return track; }
    public NoteData note() { return note; }
}

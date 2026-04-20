package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;

public record EditorSelection(Kind kind, TrackData track, NoteData note, EffectData effect, BpmPoint bpm) {
    public enum Kind {
        SONG,
        META,
        TRACK,
        NOTE,
        EFFECT,
        BPM
    }

    public static EditorSelection song() {
        return new EditorSelection(Kind.SONG, null, null, null, null);
    }

    public static EditorSelection meta() {
        return new EditorSelection(Kind.META, null, null, null, null);
    }

    public static EditorSelection track(TrackData track) {
        return new EditorSelection(Kind.TRACK, track, null, null, null);
    }

    public static EditorSelection note(TrackData track, NoteData note) {
        return new EditorSelection(Kind.NOTE, track, note, null, null);
    }

    public static EditorSelection effect(EffectData effect) {
        return new EditorSelection(Kind.EFFECT, null, null, effect, null);
    }

    public static EditorSelection bpm(BpmPoint bpm) {
        return new EditorSelection(Kind.BPM, null, null, null, bpm);
    }
}

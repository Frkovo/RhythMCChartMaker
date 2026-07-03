package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import java.util.List;

public final class NoteClipboard {
    public final List<NoteClipboardEntry> entries;
    public final int baseTrackIndex;
    public final double baseBeat;

    public NoteClipboard(List<NoteClipboardEntry> entries, int baseTrackIndex, double baseBeat) {
        this.entries = entries;
        this.baseTrackIndex = baseTrackIndex;
        this.baseBeat = baseBeat;
    }

    public List<NoteClipboardEntry> entries() { return entries; }
    public int baseTrackIndex() { return baseTrackIndex; }
    public double baseBeat() { return baseBeat; }
}

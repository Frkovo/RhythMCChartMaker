package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.Vec3Data;

public final class NoteClipboardEntry {
    public final int trackOffset;
    public final double beatOffset;
    public final NoteType noteType;
    public final Vec3Data pos;
    public final Vec3Data scale;
    public final Vec3Data rotation;
    public final int holdGroup;
    public final double holdLengthBeats;

    public NoteClipboardEntry(int trackOffset, double beatOffset, NoteType noteType, Vec3Data pos, Vec3Data scale, Vec3Data rotation, int holdGroup, double holdLengthBeats) {
        this.trackOffset = trackOffset;
        this.beatOffset = beatOffset;
        this.noteType = noteType;
        this.pos = pos;
        this.scale = scale;
        this.rotation = rotation;
        this.holdGroup = holdGroup;
        this.holdLengthBeats = holdLengthBeats;
    }

    public int trackOffset() { return trackOffset; }
    public double beatOffset() { return beatOffset; }
    public NoteType noteType() { return noteType; }
    public Vec3Data pos() { return pos; }
    public Vec3Data scale() { return scale; }
    public Vec3Data rotation() { return rotation; }
    public int holdGroup() { return holdGroup; }
    public double holdLengthBeats() { return holdLengthBeats; }
}

package cn.frkovo.rhythmcv2.rmcChart.chart.model;

public class NoteData {
    private NoteType noteType;
    private double beat;
    private Vec3Data pos;
    private Vec3Data scale;
    private Vec3Data rotation;
    private int holdGroup;
    private double holdLengthBeats;

    public NoteData(NoteType noteType, double beat, Vec3Data pos, Vec3Data scale, Vec3Data rotation, int holdGroup, double holdLengthBeats) {
        this.noteType = noteType;
        this.beat = beat;
        this.pos = pos;
        this.scale = scale;
        this.rotation = rotation;
        this.holdGroup = holdGroup;
        this.holdLengthBeats = Math.max(0.0, holdLengthBeats);
    }

    public static NoteData createDefault(double beat) {
        return new NoteData(NoteType.TAP, beat, Vec3Data.zero(), Vec3Data.one(), Vec3Data.zero(), -1, 0.0);
    }

    public NoteType noteType() {
        return noteType;
    }

    public void setNoteType(NoteType noteType) {
        this.noteType = noteType;
        if (noteType == NoteType.HOLD) {
            this.pos.set(this.pos.x(), this.pos.y(), -1.0);
        }
    }

    public double beat() {
        return beat;
    }

    public void setBeat(double beat) {
        this.beat = beat;
    }

    public Vec3Data pos() {
        return pos;
    }

    public Vec3Data scale() {
        return scale;
    }

    public Vec3Data rotation() {
        return rotation;
    }

    public int holdGroup() {
        return holdGroup;
    }

    public void setHoldGroup(int holdGroup) {
        this.holdGroup = holdGroup;
    }

    public double holdLengthBeats() {
        return holdLengthBeats;
    }

    public void setHoldLengthBeats(double holdLengthBeats) {
        this.holdLengthBeats = Math.max(0.0, holdLengthBeats);
    }
}

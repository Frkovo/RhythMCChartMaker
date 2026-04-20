package cn.frkovo.rhythmcv2.rmcChart.chart.model;

public enum NoteType {
    TAP(0, 0, 25),
    LOOK(1, 0, 25),
    HOLD(2, 1, 25),
    DODGE(3, -2, 25);

    private final int id;
    private final int zNear;
    private final int zFar;

    NoteType(int id, int zNear, int zFar) {
        this.id = id;
        this.zNear = zNear;
        this.zFar = zFar;
    }

    public int id() {
        return id;
    }

    public int zNear() {
        return zNear;
    }

    public int zFar() {
        return zFar;
    }

    public static NoteType fromId(int id) {
        for (NoteType value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return LOOK;
    }
}

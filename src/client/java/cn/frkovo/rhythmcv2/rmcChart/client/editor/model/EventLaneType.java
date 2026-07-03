package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public enum EventLaneType {
    SPEED("Speed", 0xFFE8B15B, 0x55452A11),
    MOVE_X("Move X", 0xFF87D8FF, 0x55224477),
    MOVE_Y("Move Y", 0xFF93F2C1, 0x55224477),
    MOVE_Z("Move Z", 0xFFFFD37A, 0x55224477),
    ROT_X("Rot X", 0xFFFF9E9E, 0x55773333),
    ROT_Y("Rot Y", 0xFFD4A5FF, 0x55773333),
    ROT_Z("Rot Z", 0xFFFFB347, 0x55773333),
    SCALE_X("Scale X", 0xFFA0E7E5, 0x55336666),
    SCALE_Y("Scale Y", 0xFFFFC8DD, 0x55336666),
    SCALE_Z("Scale Z", 0xFFB0E57C, 0x55336666);

    public final String label;
    public final int color;
    public final int fillColor;

    EventLaneType(String label, int color, int fillColor) {
        this.label = label;
        this.color = color;
        this.fillColor = fillColor;
    }
}

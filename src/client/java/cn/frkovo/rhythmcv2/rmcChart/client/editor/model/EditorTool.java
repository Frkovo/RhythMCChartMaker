package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;

public enum EditorTool {
    SELECT(0, "Select", "Sl", null),
    TAP(1, "Tap", "Tp", NoteType.TAP),
    LOOK(2, "Look", "Lk", NoteType.LOOK),
    HOLD(3, "Hold", "Hd", NoteType.HOLD),
    DODGE(4, "Dodge", "Dg", NoteType.DODGE),
    EVENT(5, "Event", "Fx", null),
    TRANSFORM(6, "Transform", "Tr", null),
    TEST(7, "Test", "Ts", null),
    TIMELINE(8, "Timeline", "Tl", null);

    private final int hotbarSlot;
    private final String label;
    private final String shortLabel;
    private final NoteType noteType;

    EditorTool(int hotbarSlot, String label, String shortLabel, NoteType noteType) {
        this.hotbarSlot = hotbarSlot;
        this.label = label;
        this.shortLabel = shortLabel;
        this.noteType = noteType;
    }

    public int hotbarSlot() {
        return hotbarSlot;
    }

    public String label() {
        return label;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public NoteType noteType() {
        return noteType;
    }

    public boolean isNotePlacement() {
        return noteType != null;
    }

    public static EditorTool fromHotbarSlot(int slot) {
        for (EditorTool tool : values()) {
            if (tool.hotbarSlot == slot) {
                return tool;
            }
        }
        return SELECT;
    }
}

package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public enum NoteAxis {
    X("Note X", 0xFF87D8FF),
    Y("Note Y", 0xFF93F2C1),
    Z("Note Z", 0xFFFFD37A);

    public final String label;
    public final int color;

    NoteAxis(String label, int color) {
        this.label = label;
        this.color = color;
    }
}

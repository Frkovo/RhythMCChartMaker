package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public enum ToolbarMenu {
    FILE("File"),
    EDIT("Edit"),
    OPTIONS("Options"),
    PREVIEW("Preview");

    public final String label;

    ToolbarMenu(String label) {
        this.label = label;
    }
}

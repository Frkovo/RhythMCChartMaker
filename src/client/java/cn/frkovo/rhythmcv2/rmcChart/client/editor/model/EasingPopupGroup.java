package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public final class EasingPopupGroup {
    public final String title;
    public final int y;

    public EasingPopupGroup(String title, int y) {
        this.title = title;
        this.y = y;
    }

    public String title() { return title; }
    public int y() { return y; }
}

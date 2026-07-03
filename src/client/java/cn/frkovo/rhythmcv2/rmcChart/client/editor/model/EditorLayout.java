package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public final class EditorLayout {
    public final int leftX;
    public final int topY;
    public final int leftWidth;
    public final int rightX;
    public final int rightWidth;
    public final int centerX;
    public final int centerWidth;
    public final int panelHeight;
    public final int previewHeight;
    public final int timelineY;
    public final int timelineHeight;
    public final int trackBarY;
    public final int scrollbarY;

    public EditorLayout(int leftX, int topY, int leftWidth, int rightX, int rightWidth, int centerX, int centerWidth, int panelHeight, int previewHeight, int timelineY, int timelineHeight, int trackBarY, int scrollbarY) {
        this.leftX = leftX;
        this.topY = topY;
        this.leftWidth = leftWidth;
        this.rightX = rightX;
        this.rightWidth = rightWidth;
        this.centerX = centerX;
        this.centerWidth = centerWidth;
        this.panelHeight = panelHeight;
        this.previewHeight = previewHeight;
        this.timelineY = timelineY;
        this.timelineHeight = timelineHeight;
        this.trackBarY = trackBarY;
        this.scrollbarY = scrollbarY;
    }

    public int leftX() { return leftX; }
    public int topY() { return topY; }
    public int leftWidth() { return leftWidth; }
    public int rightX() { return rightX; }
    public int rightWidth() { return rightWidth; }
    public int centerX() { return centerX; }
    public int centerWidth() { return centerWidth; }
    public int panelHeight() { return panelHeight; }
    public int previewHeight() { return previewHeight; }
    public int timelineY() { return timelineY; }
    public int timelineHeight() { return timelineHeight; }
    public int trackBarY() { return trackBarY; }
    public int scrollbarY() { return scrollbarY; }
}

package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public final class SelectionBox {
    public final int startX;
    public final int startY;
    public final int endX;
    public final int endY;

    public SelectionBox(int startX, int startY, int endX, int endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public int startX() { return startX; }
    public int startY() { return startY; }
    public int endX() { return endX; }
    public int endY() { return endY; }
}

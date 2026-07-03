package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public final class PropertyLayout {
    public int panelX = -1;
    public int transformCardY = -1;
    public int panelInnerWidth = -1;
    public int transformCardBottom = -1;

    public void setTransformCard(int panelX, int y, int innerWidth) {
        this.panelX = panelX;
        this.transformCardY = y;
        this.panelInnerWidth = innerWidth;
    }

    public void clearTransformCard() {
        this.panelX = -1;
        this.transformCardY = -1;
        this.panelInnerWidth = -1;
    }

    public int lastPanelX() { return panelX; }
    public int lastTransformCardY() { return transformCardY; }
    public int lastPanelInnerWidth() { return panelInnerWidth; }
    public void setLastTransformCardBottom(int bottom) { this.transformCardBottom = bottom; }
}

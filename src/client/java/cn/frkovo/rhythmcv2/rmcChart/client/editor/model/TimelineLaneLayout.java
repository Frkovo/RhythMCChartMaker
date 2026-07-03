package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public final class TimelineLaneLayout {
    public final TimelineLane lane;
    public final int top;
    public final int height;

    public TimelineLaneLayout(TimelineLane lane, int top, int height) {
        this.lane = lane;
        this.top = top;
        this.height = height;
    }

    public TimelineLane lane() { return lane; }
    public int top() { return top; }
    public int height() { return height; }
}

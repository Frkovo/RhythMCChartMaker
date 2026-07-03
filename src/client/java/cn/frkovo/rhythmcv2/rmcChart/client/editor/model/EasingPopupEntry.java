package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType;

public final class EasingPopupEntry {
    public final EasingType easingType;
    public final String label;
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public EasingPopupEntry(EasingType easingType, String label, int x, int y, int width, int height) {
        this.easingType = easingType;
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public EasingType easingType() { return easingType; }
    public String label() { return label; }
    public int x() { return x; }
    public int y() { return y; }
    public int width() { return width; }
    public int height() { return height; }
}

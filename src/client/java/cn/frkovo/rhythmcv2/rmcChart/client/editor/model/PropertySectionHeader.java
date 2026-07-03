package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public final class PropertySectionHeader {
    public final String key;
    public final String title;
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public PropertySectionHeader(String key, String title, int x, int y, int width, int height) {
        this.key = key;
        this.title = title;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String key() { return key; }
    public String title() { return title; }
    public int x() { return x; }
    public int y() { return y; }
    public int width() { return width; }
    public int height() { return height; }
}

package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import java.util.List;

public final class EasingPopupLayout {
    public final int width;
    public final int height;
    public final List<EasingPopupGroup> groups;
    public final List<EasingPopupEntry> entries;

    public EasingPopupLayout(int width, int height, List<EasingPopupGroup> groups, List<EasingPopupEntry> entries) {
        this.width = width;
        this.height = height;
        this.groups = groups;
        this.entries = entries;
    }

    public int width() { return width; }
    public int height() { return height; }
    public List<EasingPopupGroup> groups() { return groups; }
    public List<EasingPopupEntry> entries() { return entries; }
}

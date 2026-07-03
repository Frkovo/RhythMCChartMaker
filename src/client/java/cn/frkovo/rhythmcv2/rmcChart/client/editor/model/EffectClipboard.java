package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import java.util.List;

public final class EffectClipboard {
    public final List<EffectClipboardEntry> entries;
    public final double baseBeat;

    public EffectClipboard(List<EffectClipboardEntry> entries, double baseBeat) {
        this.entries = entries;
        this.baseBeat = baseBeat;
    }

    public List<EffectClipboardEntry> entries() { return entries; }
    public double baseBeat() { return baseBeat; }
}

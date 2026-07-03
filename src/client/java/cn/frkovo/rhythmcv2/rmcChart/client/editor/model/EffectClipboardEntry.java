package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectType;
import com.google.gson.JsonObject;

public final class EffectClipboardEntry {
    public final double beatOffset;
    public final EffectType effectType;
    public final JsonObject properties;

    public EffectClipboardEntry(double beatOffset, EffectType effectType, JsonObject properties) {
        this.beatOffset = beatOffset;
        this.effectType = effectType;
        this.properties = properties;
    }

    public double beatOffset() { return beatOffset; }
    public EffectType effectType() { return effectType; }
    public JsonObject properties() { return properties; }
}

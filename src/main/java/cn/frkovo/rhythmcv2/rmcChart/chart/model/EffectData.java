package cn.frkovo.rhythmcv2.rmcChart.chart.model;

import com.google.gson.JsonObject;

public class EffectData {
    private EffectType effectType;
    private double beat;
    private JsonObject properties;

    public EffectData(EffectType effectType, double beat, JsonObject properties) {
        this.effectType = effectType;
        this.beat = beat;
        this.properties = properties;
    }

    public static EffectData createDefault(double beat) {
        JsonObject properties = new JsonObject();
        properties.addProperty("id", "display_" + Math.max(0, (int) Math.round(beat)));
        properties.addProperty("text", "New Text Display");
        properties.addProperty("x", 0.0);
        properties.addProperty("y", 0.0);
        properties.addProperty("z", 0.0);
        return new EffectData(EffectType.TEXT_DISPLAY, beat, properties);
    }

    public EffectType effectType() {
        return effectType;
    }

    public void setEffectType(EffectType effectType) {
        this.effectType = effectType;
    }

    public double beat() {
        return beat;
    }

    public void setBeat(double beat) {
        this.beat = beat;
    }

    public JsonObject properties() {
        return properties;
    }

    public void setProperties(JsonObject properties) {
        this.properties = properties;
    }
}

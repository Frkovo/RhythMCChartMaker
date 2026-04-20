package cn.frkovo.rhythmcv2.rmcChart.chart.model;

public enum EffectType {
    HOLOGRAM,
    REMOVE_HOLOGRAM,
    TITLE,
    FIREWORK,
    TIME,
    EFFECT,
    CLEAR_EFFECT,
    WEATHER,
    ARENA,
    TEXT_DISPLAY,
    TEXT_DISPLAY_EFFECT,
    TEXT_DISPLAY_SYNC_TRACK,
    TEXT_DISPLAY_DESYNC_TRACK,
    HIDE_NOTES,
    GLOW_COLOR,
    MESSAGE,
    TEXT_DISPLAY_REMOVE;

    public static EffectType fromName(String name) {
        if (name == null || name.isBlank()) {
            return TEXT_DISPLAY;
        }
        try {
            return EffectType.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return TEXT_DISPLAY;
        }
    }
}

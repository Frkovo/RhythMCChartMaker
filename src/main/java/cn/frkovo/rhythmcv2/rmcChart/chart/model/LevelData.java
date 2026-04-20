package cn.frkovo.rhythmcv2.rmcChart.chart.model;

import java.util.ArrayList;
import java.util.List;

public class LevelData {
    private MetaData meta;
    private final List<TrackData> tracks = new ArrayList<>();
    private final List<EffectData> effects = new ArrayList<>();

    public LevelData(MetaData meta) {
        this.meta = meta;
    }

    public static LevelData createDefault(ChartDifficulty difficulty) {
        LevelData level = new LevelData(MetaData.createDefault(difficulty.defaultLevelId()));
        level.tracks.add(TrackData.createDefault(0));
        return level;
    }

    public MetaData meta() {
        return meta;
    }

    public void setMeta(MetaData meta) {
        this.meta = meta;
    }

    public List<TrackData> tracks() {
        return tracks;
    }

    public List<EffectData> effects() {
        return effects;
    }
}

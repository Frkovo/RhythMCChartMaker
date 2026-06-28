package cn.frkovo.rhythmcv2.rmcChart.chart.model;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public class ChartProject {
    private Path projectPath;
    private final SongManifestData manifest;
    private final EnumMap<ChartDifficulty, LevelData> levels;

    public ChartProject(Path projectPath, SongManifestData manifest, EnumMap<ChartDifficulty, LevelData> levels) {
        this.projectPath = projectPath;
        this.manifest = manifest;
        this.levels = levels;
    }

    public static ChartProject createEmpty(Path path) {
        EnumMap<ChartDifficulty, LevelData> levels = new EnumMap<>(ChartDifficulty.class);
        for (ChartDifficulty difficulty : ChartDifficulty.values()) {
            levels.put(difficulty, LevelData.createDefault(difficulty));
        }
        return new ChartProject(path, SongManifestData.createDefault(), levels);
    }

    public Path projectPath() {
        return projectPath;
    }

    public void setProjectPath(Path projectPath) {
        this.projectPath = projectPath;
    }

    public SongManifestData manifest() {
        return manifest;
    }

    public LevelData level(ChartDifficulty difficulty) {
        return levels.get(difficulty);
    }

    public void setLevel(ChartDifficulty difficulty, LevelData level) {
        levels.put(difficulty, level);
    }

    public Map<ChartDifficulty, LevelData> levels() {
        return levels;
    }
}

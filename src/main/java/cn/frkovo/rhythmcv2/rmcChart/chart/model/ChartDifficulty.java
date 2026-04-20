package cn.frkovo.rhythmcv2.rmcChart.chart.model;

public enum ChartDifficulty {
    WORLD("world.rmcc", "World", 1),
    NETHER("nether.rmcc", "Nether", 2),
    END("end.rmcc", "End", 3),
    VOID("void.rmcc", "Void", 4);

    private final String fileName;
    private final String displayName;
    private final int defaultLevelId;

    ChartDifficulty(String fileName, String displayName, int defaultLevelId) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.defaultLevelId = defaultLevelId;
    }

    public String fileName() {
        return fileName;
    }

    public String displayName() {
        return displayName;
    }

    public int defaultLevelId() {
        return defaultLevelId;
    }
}

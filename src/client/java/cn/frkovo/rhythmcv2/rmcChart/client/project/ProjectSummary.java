package cn.frkovo.rhythmcv2.rmcChart.client.project;

import java.nio.file.Path;

public record ProjectSummary(
        Path path,
        String folderName,
        String name,
        String composer,
        int lengthMillis,
        int difficultyCount,
        long lastModifiedMillis
) {
}

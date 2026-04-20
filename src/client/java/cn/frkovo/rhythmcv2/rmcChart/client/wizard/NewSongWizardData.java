package cn.frkovo.rhythmcv2.rmcChart.client.wizard;

import java.nio.file.Path;

public record NewSongWizardData(
        String title,
        String composer,
        String folderName,
        String description,
        Path songPath
) {
}

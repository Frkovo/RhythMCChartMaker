package cn.frkovo.rhythmcv2.rmcChart.client.project;

import cn.frkovo.rhythmcv2.rmcChart.chart.io.ChartProjectIo;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartProject;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.SongManifestData;
import cn.frkovo.rhythmcv2.rmcChart.client.wizard.ImportedSongMetadata;
import cn.frkovo.rhythmcv2.rmcChart.client.wizard.NewSongWizardData;
import cn.frkovo.rhythmcv2.rmcChart.client.wizard.SongImportHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ProjectCreationService {
    private ProjectCreationService() {
    }

    public static Path createProject(NewSongWizardData data) throws IOException {
        if (data == null) {
            throw new IOException("Missing new project data");
        }

        Path projectPath = ProjectStorage.ensureProjectsRoot().resolve(data.folderName());
        if (Files.exists(projectPath)) {
            throw new IOException("Project folder already exists: " + data.folderName());
        }

        ImportedSongMetadata metadata = SongImportHelper.readMetadata(data.songPath());
        ChartProject project = ChartProject.createEmpty(projectPath);
        SongManifestData manifest = project.manifest();
        manifest.setName(firstNonBlank(data.title(), metadata.title(), "Unknown Title"));
        manifest.setComposer(firstNonBlank(data.composer(), metadata.composer(), "Unknown Composer"));
        manifest.setDescription(firstNonBlank(data.description(), metadata.description(), ""));
        manifest.setAlias(data.folderName());
        manifest.setLength(Math.max(0, metadata.lengthMillis()));

        Files.createDirectories(projectPath);
        if (data.songPath() != null && Files.isRegularFile(data.songPath())) {
            SongImportHelper.copySongToProject(data.songPath(), projectPath);
        }
        ChartProjectIo.save(project);
        ProjectStorage.recordRecentProject(projectPath);
        return projectPath;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}

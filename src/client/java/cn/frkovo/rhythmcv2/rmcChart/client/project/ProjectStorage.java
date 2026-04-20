package cn.frkovo.rhythmcv2.rmcChart.client.project;

import cn.frkovo.rhythmcv2.rmcChart.chart.io.ChartProjectIo;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartProject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ProjectStorage {
    private static final String RECENT_FILE = ".recent-projects.txt";
    private static final int MAX_RECENT = 10;

    private ProjectStorage() {
    }

    public static Path projectsRoot() {
        return FabricLoader.getInstance().getGameDir().resolve("projects");
    }

    public static Path ensureProjectsRoot() throws IOException {
        Path root = projectsRoot();
        Files.createDirectories(root);
        return root;
    }

    public static Path projectPath(String folderName) {
        return projectsRoot().resolve(folderName);
    }

    public static List<Path> listProjects() {
        List<Path> result = new ArrayList<>();
        Path root = projectsRoot();
        if (!Files.isDirectory(root)) {
            return result;
        }
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> Files.exists(path.resolve("manifest.yml")))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .forEach(result::add);
        } catch (IOException ignored) {
        }
        return result;
    }

    public static List<ProjectSummary> listProjectSummaries() {
        List<ProjectSummary> summaries = new ArrayList<>();
        for (Path project : listProjects()) {
            summaries.add(readSummary(project));
        }
        return summaries;
    }

    public static List<ProjectSummary> listRecentProjectSummaries() {
        List<ProjectSummary> summaries = new ArrayList<>();
        for (Path project : listRecentProjects()) {
            summaries.add(readSummary(project));
        }
        return summaries;
    }

    public static ProjectSummary mostRecentProjectSummary() {
        List<ProjectSummary> summaries = listRecentProjectSummaries();
        return summaries.isEmpty() ? null : summaries.getFirst();
    }

    public static void recordRecentProject(Path projectPath) {
        if (projectPath == null) {
            return;
        }
        try {
            Files.createDirectories(projectsRoot());
            Set<String> entries = new LinkedHashSet<>();
            entries.add(projectPath.toAbsolutePath().normalize().toString());
            for (Path path : listRecentProjects()) {
                entries.add(path.toAbsolutePath().normalize().toString());
            }
            List<String> recent = new ArrayList<>(entries);
            if (recent.size() > MAX_RECENT) {
                recent = recent.subList(0, MAX_RECENT);
            }
            try (Writer writer = Files.newBufferedWriter(projectsRoot().resolve(RECENT_FILE))) {
                for (String line : recent) {
                    writer.write(line);
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static List<Path> listRecentProjects() {
        List<Path> result = new ArrayList<>();
        Path file = projectsRoot().resolve(RECENT_FILE);
        if (!Files.isRegularFile(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                Path path = Path.of(line.trim());
                if (Files.isDirectory(path) && Files.exists(path.resolve("manifest.yml"))) {
                    result.add(path);
                }
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    private static ProjectSummary readSummary(Path projectPath) {
        String folderName = projectPath.getFileName().toString();
        String name = folderName;
        String composer = "Unknown Composer";
        int lengthMillis = 0;
        int difficultyCount = 0;
        long lastModifiedMillis = 0L;

        try {
            ChartProject project = ChartProjectIo.load(projectPath);
            name = project.manifest().name();
            composer = project.manifest().composer();
            lengthMillis = project.manifest().length();
            for (ChartDifficulty difficulty : ChartDifficulty.values()) {
                if (Files.exists(projectPath.resolve(difficulty.fileName()))) {
                    difficultyCount++;
                }
            }
            lastModifiedMillis = scanLastModified(projectPath);
        } catch (IOException ignored) {
        }

        return new ProjectSummary(projectPath, folderName, name, composer, lengthMillis, difficultyCount, lastModifiedMillis);
    }

    private static long scanLastModified(Path projectPath) {
        try (var stream = Files.walk(projectPath, 2)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis();
                        } catch (IOException ignored) {
                            return 0L;
                        }
                    })
                    .max()
                    .orElse(0L);
        } catch (IOException ignored) {
            return 0L;
        }
    }
}

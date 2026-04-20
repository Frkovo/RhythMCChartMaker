package cn.frkovo.rhythmcv2.rmcChart.client.editor.audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SongAudioResolver {
    private static final List<String> AUDIO_EXTENSIONS = List.of(".ogg", ".wav", ".mp3", ".flac", ".aiff", ".au");
    private static final List<String> PREFERRED_BASE_NAMES = List.of("song", "music", "audio", "bgm", "track", "preview");

    private SongAudioResolver() {
    }

    public static Optional<Path> resolve(Path projectPath) {
        if (projectPath == null || !Files.isDirectory(projectPath)) {
            return Optional.empty();
        }

        for (String baseName : PREFERRED_BASE_NAMES) {
            for (String extension : AUDIO_EXTENSIONS) {
                Path direct = projectPath.resolve(baseName + extension);
                if (Files.isRegularFile(direct)) {
                    return Optional.of(direct);
                }
            }
        }

        try {
            List<Path> audioFiles = new ArrayList<>();
            try (var stream = Files.walk(projectPath, 3)) {
                stream.filter(Files::isRegularFile)
                        .filter(SongAudioResolver::isAudioFile)
                        .sorted(Comparator.comparing(path -> projectPath.relativize(path).toString(), String.CASE_INSENSITIVE_ORDER))
                        .forEach(audioFiles::add);
            }

            if (audioFiles.isEmpty()) {
                return Optional.empty();
            }
            if (audioFiles.size() == 1) {
                return Optional.of(audioFiles.getFirst());
            }

            return audioFiles.stream()
                    .sorted(Comparator.comparingInt(SongAudioResolver::priorityOf))
                    .findFirst();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private static boolean isAudioFile(Path path) {
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : AUDIO_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static int priorityOf(Path path) {
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (int index = 0; index < PREFERRED_BASE_NAMES.size(); index++) {
            if (lower.startsWith(PREFERRED_BASE_NAMES.get(index))) {
                return index;
            }
        }
        return PREFERRED_BASE_NAMES.size() + 1;
    }
}

package cn.frkovo.rhythmcv2.rmcChart.client.wizard;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SongImportHelper {
    private SongImportHelper() {
    }

    public static ImportedSongMetadata readMetadata(Path songPath) {
        if (songPath == null || !Files.isRegularFile(songPath)) {
            return ImportedSongMetadata.empty();
        }

        try {
            AudioFile audioFile = AudioFileIO.read(songPath.toFile());
            Tag tag = audioFile.getTag();
            AudioHeader header = audioFile.getAudioHeader();

            String title = firstNonBlank(
                    getTagValue(tag, FieldKey.TITLE),
                    stripExtension(songPath.getFileName().toString())
            );
            String composer = firstNonBlank(
                    getTagValue(tag, FieldKey.COMPOSER),
                    getTagValue(tag, FieldKey.ARTIST),
                    getTagValue(tag, FieldKey.ALBUM_ARTIST),
                    "Unknown Composer"
            );
            String description = firstNonBlank(
                    getTagValue(tag, FieldKey.COMMENT),
                    getTagValue(tag, FieldKey.LYRICS),
                    ""
            );
            int lengthMillis = header == null ? 0 : Math.max(0, header.getTrackLength() * 1000);

            return new ImportedSongMetadata(title, composer, description, lengthMillis);
        } catch (Exception ignored) {
            return new ImportedSongMetadata(stripExtension(songPath.getFileName().toString()), "", "", 0);
        }
    }

    public static Path copySongToProject(Path sourceSongPath, Path projectPath) throws IOException {
        if (sourceSongPath == null || !Files.isRegularFile(sourceSongPath) || projectPath == null) {
            return null;
        }

        Files.createDirectories(projectPath);
        String fileName = sourceSongPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex) : "";
        Path target = projectPath.resolve("song" + extension);
        Files.copy(sourceSongPath, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        return target;
    }

    private static String getTagValue(Tag tag, FieldKey fieldKey) {
        if (tag == null) {
            return "";
        }
        try {
            String value = tag.getFirst(fieldKey);
            return value == null ? "" : value.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
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

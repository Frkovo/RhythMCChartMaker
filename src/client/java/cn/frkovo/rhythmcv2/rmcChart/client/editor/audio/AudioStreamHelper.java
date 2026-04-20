package cn.frkovo.rhythmcv2.rmcChart.client.editor.audio;

import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import javazoom.spi.vorbis.sampled.convert.VorbisFormatConversionProvider;
import javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import javax.sound.sampled.spi.FormatConversionProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

final class AudioStreamHelper {
    private static final List<AudioFileReader> FALLBACK_READERS = List.of(
            new MpegAudioFileReader(),
            new VorbisAudioFileReader()
    );
    private static final List<FormatConversionProvider> FALLBACK_CONVERTERS = List.of(
            new MpegFormatConversionProvider(),
            new VorbisFormatConversionProvider()
    );

    private AudioStreamHelper() {
    }

    static AudioInputStream openDecodedStream(Path path) throws IOException, UnsupportedAudioFileException {
        AudioInputStream source = openSourceStream(path);
        try {
            AudioFormat sourceFormat = source.getFormat();
            AudioFormat targetFormat = targetFormat(sourceFormat);
            if (isTargetFormat(sourceFormat, targetFormat)) {
                return source;
            }
            return convert(source, sourceFormat, targetFormat);
        } catch (RuntimeException | UnsupportedAudioFileException exception) {
            try {
                source.close();
            } catch (IOException ignored) {
            }
            throw exception;
        }
    }

    private static AudioInputStream openSourceStream(Path path) throws IOException, UnsupportedAudioFileException {
        try {
            return AudioSystem.getAudioInputStream(path.toFile());
        } catch (UnsupportedAudioFileException exception) {
            for (AudioFileReader reader : FALLBACK_READERS) {
                try {
                    return reader.getAudioInputStream(path.toFile());
                } catch (UnsupportedAudioFileException ignored) {
                }
            }
            throw new UnsupportedAudioFileException(extensionOf(path) + " is not supported by the bundled audio decoders");
        }
    }

    private static AudioInputStream convert(AudioInputStream source, AudioFormat sourceFormat, AudioFormat targetFormat) throws UnsupportedAudioFileException {
        if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            return AudioSystem.getAudioInputStream(targetFormat, source);
        }
        for (FormatConversionProvider provider : FALLBACK_CONVERTERS) {
            if (provider.isConversionSupported(targetFormat, sourceFormat)) {
                return provider.getAudioInputStream(targetFormat, source);
            }
        }
        throw new UnsupportedAudioFileException("No PCM decoder for " + sourceFormat.getEncoding());
    }

    private static AudioFormat targetFormat(AudioFormat sourceFormat) {
        int channels = Math.max(1, sourceFormat.getChannels());
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                channels,
                channels * 2,
                sourceFormat.getSampleRate(),
                false
        );
    }

    private static boolean isTargetFormat(AudioFormat sourceFormat, AudioFormat targetFormat) {
        return AudioFormat.Encoding.PCM_SIGNED.equals(sourceFormat.getEncoding())
                && sourceFormat.getSampleSizeInBits() == targetFormat.getSampleSizeInBits()
                && sourceFormat.getChannels() == targetFormat.getChannels()
                && sourceFormat.getFrameSize() == targetFormat.getFrameSize()
                && Float.compare(sourceFormat.getSampleRate(), targetFormat.getSampleRate()) == 0
                && !sourceFormat.isBigEndian();
    }

    private static String extensionOf(Path path) {
        String name = path == null ? "audio file" : path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "Audio file";
        }
        return name.substring(dot + 1).toUpperCase(Locale.ROOT);
    }
}

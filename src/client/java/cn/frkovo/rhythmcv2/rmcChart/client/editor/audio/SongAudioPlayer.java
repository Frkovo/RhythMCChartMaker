package cn.frkovo.rhythmcv2.rmcChart.client.editor.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Path;

public class SongAudioPlayer {
    private Clip clip;
    private Path loadedPath;

    public void load(Path path) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        close();

        try (AudioInputStream decoded = AudioStreamHelper.openDecodedStream(path)) {
            AudioFormat decodedFormat = decoded.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
            Clip newClip = (Clip) AudioSystem.getLine(info);
            newClip.open(decoded);
            this.clip = newClip;
            this.loadedPath = path;
        }
    }

    public void play() {
        if (!isLoaded()) {
            return;
        }
        clip.start();
    }

    public void pause() {
        if (!isLoaded()) {
            return;
        }
        clip.stop();
    }

    public void seekMillis(long millis) {
        if (!isLoaded()) {
            return;
        }
        long clamped = Math.clamp(millis, 0L, lengthMillis());
        clip.setMicrosecondPosition(clamped * 1000L);
    }

    public long positionMillis() {
        if (!isLoaded()) {
            return 0L;
        }
        return clip.getMicrosecondPosition() / 1000L;
    }

    public long lengthMillis() {
        if (!isLoaded()) {
            return 0L;
        }
        return clip.getMicrosecondLength() / 1000L;
    }

    public boolean isLoaded() {
        return clip != null;
    }

    public boolean isPlaying() {
        return isLoaded() && clip.isRunning();
    }

    public Path loadedPath() {
        return loadedPath;
    }

    public void close() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
        loadedPath = null;
    }
}

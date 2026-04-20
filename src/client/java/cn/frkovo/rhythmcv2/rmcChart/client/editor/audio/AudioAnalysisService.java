package cn.frkovo.rhythmcv2.rmcChart.client.editor.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AudioAnalysisService {
    private static final int WAVEFORM_BINS = 512;
    private static final int WINDOW_FRAMES = 1024;

    private AudioAnalysisService() {
    }

    public static AudioAnalysis analyze(Path path) {
        if (path == null) {
            return AudioAnalysis.empty();
        }

        try (AudioInputStream decoded = AudioStreamHelper.openDecodedStream(path)) {
            AudioFormat decodedFormat = decoded.getFormat();
            long totalFrames = decoded.getFrameLength();
            if (totalFrames <= 0L) {
                totalFrames = Math.max(1L, Math.round(decodedFormat.getSampleRate() * 180.0));
            }

            int channels = Math.max(1, decodedFormat.getChannels());
            int frameSize = Math.max(2, decodedFormat.getFrameSize());
            float[] waveform = new float[WAVEFORM_BINS];
            long[] waveformCounts = new long[WAVEFORM_BINS];
            List<Double> energyWindows = new ArrayList<>();

            byte[] buffer = new byte[Math.max(frameSize * WINDOW_FRAMES, 4096)];
            long framesReadTotal = 0L;
            int windowSampleCount = 0;
            double windowEnergy = 0.0;

            int read;
            while ((read = decoded.read(buffer)) != -1) {
                int usableBytes = read - (read % frameSize);
                for (int offset = 0; offset < usableBytes; offset += frameSize) {
                    double sample = 0.0;
                    for (int channel = 0; channel < channels; channel++) {
                        int sampleOffset = offset + channel * 2;
                        int lo = buffer[sampleOffset] & 0xFF;
                        int hi = buffer[sampleOffset + 1];
                        short pcm = (short) ((hi << 8) | lo);
                        sample += pcm / 32768.0;
                    }
                    sample /= channels;

                    float amplitude = (float) Math.abs(sample);
                    int bin = (int) Math.min(WAVEFORM_BINS - 1, (framesReadTotal * WAVEFORM_BINS) / Math.max(1L, totalFrames));
                    waveform[bin] += amplitude;
                    waveformCounts[bin]++;

                    windowEnergy += sample * sample;
                    windowSampleCount++;
                    if (windowSampleCount >= WINDOW_FRAMES) {
                        energyWindows.add(windowEnergy / windowSampleCount);
                        windowEnergy = 0.0;
                        windowSampleCount = 0;
                    }

                    framesReadTotal++;
                }
            }

            if (windowSampleCount > 0) {
                energyWindows.add(windowEnergy / windowSampleCount);
            }

            float peak = 0.0001f;
            for (int index = 0; index < waveform.length; index++) {
                if (waveformCounts[index] > 0) {
                    waveform[index] /= waveformCounts[index];
                }
                peak = Math.max(peak, waveform[index]);
            }
            for (int index = 0; index < waveform.length; index++) {
                waveform[index] = Math.min(1.0f, waveform[index] / peak);
            }

            long lengthMillis = Math.max(0L, Math.round(framesReadTotal * 1000.0 / decodedFormat.getSampleRate()));
            double estimatedBpm = estimateBpm(energyWindows, decodedFormat.getSampleRate());
            return new AudioAnalysis(waveform, lengthMillis, estimatedBpm);
        } catch (Exception ignored) {
            return AudioAnalysis.empty();
        }
    }

    private static double estimateBpm(List<Double> energyWindows, float sampleRate) {
        if (energyWindows.size() < 32) {
            return 0.0;
        }

        double[] flux = new double[energyWindows.size()];
        for (int index = 1; index < energyWindows.size(); index++) {
            flux[index] = Math.max(0.0, energyWindows.get(index) - energyWindows.get(index - 1));
        }

        double hopSeconds = WINDOW_FRAMES / sampleRate;
        int minLag = Math.max(1, (int) Math.round(60.0 / 200.0 / hopSeconds));
        int maxLag = Math.max(minLag + 1, (int) Math.round(60.0 / 70.0 / hopSeconds));

        double bestScore = 0.0;
        int bestLag = 0;
        for (int lag = minLag; lag <= maxLag; lag++) {
            double score = 0.0;
            for (int index = lag; index < flux.length; index++) {
                score += flux[index] * flux[index - lag];
            }
            if (score > bestScore) {
                bestScore = score;
                bestLag = lag;
            }
        }

        if (bestLag == 0 || bestScore <= 0.0) {
            return 0.0;
        }

        double bpm = 60.0 / (bestLag * hopSeconds);
        while (bpm < 80.0) {
            bpm *= 2.0;
        }
        while (bpm > 190.0) {
            bpm /= 2.0;
        }
        return Math.round(bpm * 10.0) / 10.0;
    }
}

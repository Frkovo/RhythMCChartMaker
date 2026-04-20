package cn.frkovo.rhythmcv2.rmcChart.client.editor.audio;

public record AudioAnalysis(float[] waveform, long lengthMillis, double estimatedBpm) {
    public static AudioAnalysis empty() {
        return new AudioAnalysis(new float[0], 0L, 0.0);
    }

    public boolean hasWaveform() {
        return waveform.length > 0 && lengthMillis > 0L;
    }

    public boolean hasEstimatedBpm() {
        return estimatedBpm > 0.0;
    }
}

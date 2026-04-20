package cn.frkovo.rhythmcv2.rmcChart.chart.model;

public class BpmPoint {
    private double beat;
    private double bpm;

    public BpmPoint(double beat, double bpm) {
        this.beat = beat;
        this.bpm = bpm;
    }

    public double beat() {
        return beat;
    }

    public void setBeat(double beat) {
        this.beat = beat;
    }

    public double bpm() {
        return bpm;
    }

    public void setBpm(double bpm) {
        this.bpm = bpm;
    }
}

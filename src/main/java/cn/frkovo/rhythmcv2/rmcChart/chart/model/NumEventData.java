package cn.frkovo.rhythmcv2.rmcChart.chart.model;

public class NumEventData {
    private double startBeat;
    private double endBeat;
    private double startValue;
    private double endValue;
    private EasingType easingType;

    public NumEventData(double startBeat, double endBeat, double startValue, double endValue, EasingType easingType) {
        this.startBeat = startBeat;
        this.endBeat = endBeat;
        this.startValue = startValue;
        this.endValue = endValue;
        this.easingType = easingType;
    }

    public double startBeat() {
        return startBeat;
    }

    public void setStartBeat(double startBeat) {
        this.startBeat = startBeat;
    }

    public double endBeat() {
        return endBeat;
    }

    public void setEndBeat(double endBeat) {
        this.endBeat = endBeat;
    }

    public double startValue() {
        return startValue;
    }

    public void setStartValue(double startValue) {
        this.startValue = startValue;
    }

    public double endValue() {
        return endValue;
    }

    public void setEndValue(double endValue) {
        this.endValue = endValue;
    }

    public EasingType easingType() {
        return easingType;
    }

    public void setEasingType(EasingType easingType) {
        this.easingType = easingType;
    }
}

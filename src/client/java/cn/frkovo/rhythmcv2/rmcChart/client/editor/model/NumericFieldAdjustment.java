package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public final class NumericFieldAdjustment {
    public final double baseStep;
    public final boolean integer;

    public NumericFieldAdjustment(double baseStep, boolean integer) {
        this.baseStep = baseStep;
        this.integer = integer;
    }

    public double baseStep() { return baseStep; }
    public boolean integer() { return integer; }
}

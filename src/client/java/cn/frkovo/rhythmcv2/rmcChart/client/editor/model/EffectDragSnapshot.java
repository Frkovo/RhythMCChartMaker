package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public final class EffectDragSnapshot {
    public final double startBeat;
    public final double endBeat;

    public EffectDragSnapshot(double startBeat, double endBeat) {
        this.startBeat = startBeat;
        this.endBeat = endBeat;
    }

    public double startBeat() { return startBeat; }
    public double endBeat() { return endBeat; }
}

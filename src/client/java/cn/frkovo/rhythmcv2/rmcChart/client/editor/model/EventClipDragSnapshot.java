package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;

public final class EventClipDragSnapshot {
    public final NumEventData event;
    public final double startBeat;
    public final double endBeat;

    public EventClipDragSnapshot(NumEventData event, double startBeat, double endBeat) {
        this.event = event;
        this.startBeat = startBeat;
        this.endBeat = endBeat;
    }

    public NumEventData event() { return event; }
    public double startBeat() { return startBeat; }
    public double endBeat() { return endBeat; }
}

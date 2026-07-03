package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartProject;

public final class EditorSnapshot {
    public final ChartProject project;
    public final ChartDifficulty activeDifficulty;
    public final double visibleStartBeat;
    public final double beatsPerScreen;
    public final double playheadBeat;
    public final SelectionSnapshot selection;

    public EditorSnapshot(ChartProject project, ChartDifficulty activeDifficulty, double visibleStartBeat, double beatsPerScreen, double playheadBeat, SelectionSnapshot selection) {
        this.project = project;
        this.activeDifficulty = activeDifficulty;
        this.visibleStartBeat = visibleStartBeat;
        this.beatsPerScreen = beatsPerScreen;
        this.playheadBeat = playheadBeat;
        this.selection = selection;
    }

    public ChartProject project() { return project; }
    public ChartDifficulty activeDifficulty() { return activeDifficulty; }
    public double visibleStartBeat() { return visibleStartBeat; }
    public double beatsPerScreen() { return beatsPerScreen; }
    public double playheadBeat() { return playheadBeat; }
    public SelectionSnapshot selection() { return selection; }
}

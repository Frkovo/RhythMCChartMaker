package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.client.editor.EditorSelection;

public final class SelectionSnapshot {
    public final EditorSelection.Kind kind;
    public final Integer trackId;
    public final Integer noteIndex;
    public final Integer effectIndex;
    public final Integer bpmIndex;

    public SelectionSnapshot(EditorSelection.Kind kind, Integer trackId, Integer noteIndex, Integer effectIndex, Integer bpmIndex) {
        this.kind = kind;
        this.trackId = trackId;
        this.noteIndex = noteIndex;
        this.effectIndex = effectIndex;
        this.bpmIndex = bpmIndex;
    }

    public EditorSelection.Kind kind() { return kind; }
    public Integer trackId() { return trackId; }
    public Integer noteIndex() { return noteIndex; }
    public Integer effectIndex() { return effectIndex; }
    public Integer bpmIndex() { return bpmIndex; }
}

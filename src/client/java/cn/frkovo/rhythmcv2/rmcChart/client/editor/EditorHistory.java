package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import java.util.ArrayDeque;
import java.util.Deque;

class EditorHistory {
    private final ChartEditorScreen screen;
    final Deque<ChartEditorState.EditorSnapshot> undoSnapshots = new ArrayDeque<>();
    final Deque<ChartEditorState.EditorSnapshot> redoSnapshots = new ArrayDeque<>();
    int lastSnapshotRevision = -1;
    boolean applyingHistorySnapshot;

    EditorHistory(ChartEditorScreen screen) { this.screen = screen; }

    void resetHistorySnapshots() {
        undoSnapshots.clear(); redoSnapshots.clear();
        undoSnapshots.addLast(screen.state.snapshot());
        lastSnapshotRevision = screen.state.revision();
    }

    void captureHistorySnapshotIfNeeded() {
        if (applyingHistorySnapshot || lastSnapshotRevision == screen.state.revision()) return;
        undoSnapshots.addLast(screen.state.snapshot());
        while (undoSnapshots.size() > 128) undoSnapshots.removeFirst();
        redoSnapshots.clear();
        lastSnapshotRevision = screen.state.revision();
    }

    void undoEditorChange() {
        captureHistorySnapshotIfNeeded();
        if (undoSnapshots.size() <= 1) { screen.state.setStatus("Nothing to undo"); return; }
        ChartEditorState.EditorSnapshot current = undoSnapshots.removeLast();
        redoSnapshots.addLast(current);
        applyHistorySnapshot(undoSnapshots.getLast(), "Undo");
    }

    void redoEditorChange() {
        if (redoSnapshots.isEmpty()) { screen.state.setStatus("Nothing to redo"); return; }
        ChartEditorState.EditorSnapshot next = redoSnapshots.removeLast();
        undoSnapshots.addLast(next);
        applyHistorySnapshot(next, "Redo");
    }

    private void applyHistorySnapshot(ChartEditorState.EditorSnapshot snapshot, String label) {
        applyingHistorySnapshot = true;
        screen.state.restoreSnapshot(snapshot);
        applyingHistorySnapshot = false;
        lastSnapshotRevision = screen.state.revision();
        screen.selectionManager.clearTimelineSelections();
        if (screen.state.selection().note() != null && screen.state.selection().track() != null)
            screen.selectionManager.selectedNotes.add(new SelectedNote(screen.state.selection().track(), screen.state.selection().note()));
        if (screen.state.selection().effect() != null)
            screen.selectionManager.selectedEffects.add(new SelectedEffect(screen.state.selection().effect()));
        screen.propertyPanel.populateFieldsFromSelection();
        screen.propertyPanel.layoutPropertyFields();
        screen.state.setStatus(label);
    }
}

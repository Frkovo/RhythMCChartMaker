# Impact Analysis

## Files Changed

- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/editor/ChartEditorScreen.java`
  - Restructured timeline lane builder to place the FX Track at the bottom.
  - Added FX Track header and per-effect clip lane rendering.
  - Implemented timed-effect clip rendering with labels, end handles, and drag-to-move.
  - Added effect multi-selection, box-select, copy/cut/paste clipboard.
  - Added editor-only group creation popup (Ctrl+G / Edit > Group).
  - Group visualization for effect clips and notes via colored highlights.
  - Increased timeline ruler/grid subdivision granularity and labeled whole-beat numbers.
  - Right-click adding notes/effects now inserts at the playhead (red line).
  - Updated Edit toolbar buttons to dispatch Copy/Cut/Paste between notes and effects.

- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/editor/ChartEditorState.java`
  - Added FX Track expand/collapse state.
  - Added `EditorDraft` instance for editor-only note/effect groups.
  - Clears editor draft on project load/new/restore.

- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/editor/EditorDraft.java` (new)
  - In-memory storage for effect groups and note groups.
  - Explicitly not serialized to compiled chart JSON.

## Contract Boundaries

- Does **not** modify `rhythmc:chart_preview` opcodes, payload schema, or lifecycle.
- Does **not** change chart JSON field names/types for tracks, notes, effects, or events.
- Editor-only groups are not written to the level JSON consumed by the Preview plugin.

## Residual Risk / Known Limitations

- Note group membership is keyed by `(trackId, beat, noteType)`. After moving or pasting notes the membership does not automatically follow because note identity is not stable across edits. Effect groups use object identity and therefore follow moves but not copy/paste within the same session.
- Groups are currently held only in memory. They survive undo/redo and project switches within the same `ChartEditorState` session, but are not persisted to disk. Persisting groups in a project-side editor draft file is a follow-up task.
- The chart compiler / preview serialization path was not changed; however, visual validation in-game is still required to confirm lane ordering, clip rendering, and popup layering.

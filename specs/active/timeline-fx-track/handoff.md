# Handoff Notes

## Completed

- FX Track is now rendered at the bottom of the timeline, always present, with expand/collapse toggle.
- When expanded, each effect has its own clip row. Timed effects show a body rectangle from start beat to end beat; instantaneous effects show a 1-beat placeholder.
- Effect clips display a short label (type + text/value) and can be dragged to move. Multi-select works with Ctrl+click and box select.
- Timeline grid is finer (down to 1/16 beat) and the ruler labels every whole beat number.
- Right-click on a note lane or FX Track header creates a note/effect at the current playhead.
- Editor-only groups can be created for selected effects or notes via Edit > Group or Ctrl+G. Groups are visualized with colored highlights.
- Copy/Cut/Paste now works for effects when effect clips are selected; notes continue to use the existing clipboard.

## Validation

- `./gradlew compileClientJava` passes.
- No plugin channel or chart JSON contract changes.

## Risks

- In-game visual review is still needed; `ChartEditorScreen` is large and this change touches rendering order and lane math.
- Note group membership is keyed by beat/type and will not automatically follow moved/pasted notes.
- Groups are not persisted to disk yet.

## Next Steps

1. Run the mod in-game and verify FX Track layout, clip rendering, group highlights, and the group-name popup.
2. Decide whether to persist `EditorDraft` in a new project-side file (e.g., `.editor-draft.yml`).
3. Consider stable note IDs if group persistence is required.

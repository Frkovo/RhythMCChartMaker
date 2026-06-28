# Task Breakdown

- [x] Move Effects lane from top to a dedicated bottom FX Track.
- [x] Make FX Track always present and expandable/collapsible.
- [x] Render each effect in its own FX Track clip row when expanded.
- [x] Render timed effects (with `duration`) as horizontal clips with start/end and labels.
- [x] Implement drag-to-move for effect clips with snap/alignment.
- [x] Display effect type + identifying text/value on each clip.
- [x] Increase timeline ruler/grid granularity (1/16 up to whole beat).
- [x] Label whole-beat numbers on the timeline ruler.
- [x] Insert new notes and effects at the playhead red line.
- [x] Add editor-only effect grouping with name popup.
- [x] Add editor-only note grouping with name popup.
- [x] Add Copy/Cut/Paste for effects.
- [x] Wire Edit toolbar buttons and keyboard shortcuts (Ctrl+C/V/G).
- [x] Compile check.
- [x] Update docs/CHANGELOG.

## Pending / Follow-up

- [ ] Persist `EditorDraft` groups to the project file without including them in compiled chart JSON.
- [ ] Improve stable identity for note groups so membership follows move/paste operations.
- [ ] In-game visual validation of FX Track layout, clip labels, and group highlights.

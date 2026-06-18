# RhythMC Chart Maker Editor Shortcuts

## Note Creation

- `1`: Create TAP at playhead on the selected track.
- `2`: Create LOOK at playhead on the selected track.
- `3`: Create HOLD at playhead on the selected track. HOLD notes are forced to `Z = -1.0`.
- `4`: Create DODGE at playhead on the selected track.

## Selection

- `Ctrl + Click`: Toggle a note or event clip in the current multi-selection.
- Drag on the timeline: Box-select notes and event clips.
- `Ctrl + A`: Select all notes.
- `Esc`: Clear timeline selection.

## Clipboard And History

- `Ctrl + C`: Copy selected notes.
- `Ctrl + X`: Cut selected notes.
- `Ctrl + V`: Paste notes at the playhead.
- `Ctrl + Z`: Undo.
- `Ctrl + Y` or `Ctrl + Shift + Z`: Redo.

## Movement

- `Alt + Left/Right`: Move selected notes by the current shortcut step.
- `Alt + Shift + Left/Right`: Move selected notes by 1 beat.
- `Alt + Up/Down`: Move selected notes between tracks.

## Smoothing

- `Ctrl + 3`: Open the Smoothing popup.
- Smoothing uses the currently selected notes as beat-ordered control points.
- `Curve` cycles `Catmull` plus every chart `EasingType` backed by `EasingFunctions`, including Linear, Sine, Quad, Cubic, Quart, Quint, Expo, Circ, Back, Elastic, Bounce, and Square variants.
- `Note` chooses the note type for smoothed and generated notes.
- `Fill: On` creates additional notes between the first and last selected notes using `1/x` division.
- `Fill: Off` only smooths and retags the selected notes.
- HOLD smoothing keeps generated and selected HOLD notes at `Z = -1.0`.

## Timeline

- `Space`: Play or pause.
- `Left/Right`: Move playhead by 1/4 beat.
- `Ctrl + Mouse Wheel`: Zoom timeline.
- Mouse wheel on timeline: Scroll timeline.
- Click ruler or Audio strip: Seek playhead.

## Playback Range And Display

- `Options > In`: Set playback range start at playhead.
- `Options > Out`: Set playback range end at playhead.
- `Options > NoOut`: Clear playback range end.
- `Options > Range`: Play from In to Out.
- `Options > 1Track`: Toggle only the selected track in preview/timeline.

# RhythMC Chart Maker Editor Shortcuts

## Note Creation

- `1`: Create TAP at playhead on the selected track.
- `2`: Create LOOK at playhead on the selected track.
- `3`: Create HOLD at playhead on the selected track. HOLD notes are forced to `Z = -1.0`.
- `4`: Create DODGE at playhead on the selected track.

## Selection

- `Ctrl + Click`: Toggle a note, effect clip, or event clip in the current multi-selection.
- Drag on the timeline: Box-select notes, effect clips, and event clips.
- `Ctrl + A`: Select all notes.
- `Esc`: Clear timeline selection.

## Clipboard And History

- `Ctrl + C`: Copy selected notes or effect clips (whichever is selected).
- `Ctrl + X`: Cut selected notes or effect clips.
- `Ctrl + V`: Paste notes or effect clips at the playhead.
- `Ctrl + G`: Group the currently selected notes or effect clips.
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

## FX Track

- FX Track sits at the bottom of the timeline and can be expanded/collapsed like a Track header.
- Each effect has its own row when expanded. Timed effects show a draggable clip with a label.
- Right-click the FX Track header to add a new effect at the playhead.
- Effect groups are editor-only and highlighted with a colored bar.

## Timeline

- `F`: Toggle the ChartMaker GUI.
- `R`: Start playback from the current playhead.
- `T`: Stop playback.
- `Shift + Left/Right`: Seek backward/forward by 10 seconds.
- `Space`: Play or pause local audio in the editor.
- `Left/Right`: Move playhead by 1/4 beat.
- `Ctrl + Mouse Wheel`: Zoom timeline.
- Mouse wheel on timeline: Scroll timeline.
- Click ruler or Audio strip: Seek playhead.
- Right-click on a Note lane or FX Track header: Add a note/effect at the playhead red line.

## Raw JSON Editor

- `File > JSON`: Open the raw level JSON editor.
- `Ctrl + C`: Copy the current level JSON to clipboard.
- `Ctrl + V` / `Ctrl + Enter`: Parse clipboard JSON and apply it to the current project.
- `Ctrl + R`: Regenerate the JSON display from the current project state.
- `Esc`: Return to the chart editor.

## In-Game HUD

- The bottom-right overlay always shows the editor shortcuts.
- The top overlay always shows `BPM`, `beat`, `tick`, and current/total duration near the bossbar area.

## Playback Range And Preview

- `Options > In`: Set playback range start at playhead.
- `Options > Out`: Set playback range end at playhead.
- `Options > NoOut`: Clear playback range end.
- `Options > Range`: Play from In to Out locally.
- `Edit > Play` / `Edit > Stop`: Play or pause audio locally.
- `Options > 1Track`: Toggle only the selected track in editor/timeline views.
- `Preview > Play` / `Preview > Restart`, or the center `Game Preview` Play button: Start the real in-world preview through the RhythMC-Preview server and close the editor GUI. Reopening the GUI with `F` does not rebuild the arena.
- `Esc` while the in-world preview is active: Pause playback, stop the server preview, and return to the chart editor GUI.
- In-world preview plays autoplay hit sounds matching `RhythMC-Reborn`: TAP/LOOK use the default bass drum note; HOLD groups play pressure-plate on/off clicks for start/end blocks.

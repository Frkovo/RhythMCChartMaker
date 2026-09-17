# RhythMC Chart Maker Editor Shortcuts

## Vanilla Hotbar Tools

The selected Minecraft hotbar slot is the editor tool, both with the shell open
and while controlling the world directly.

| Key / slot | Tool | Current behavior |
|---:|---|---|
| `1` | Select | Select objects in the world, Scene Map, or timeline. |
| `2` | Tap | Select TAP placement mode. Left-click a Notes lane to place at that beat. Re-press `2` in world view to place at the crosshair hit on the selected track plane. |
| `3` | Look | Select LOOK placement mode. Left-click a Notes lane to place. Re-press `3` in world view to place at the crosshair hit. |
| `4` | Hold | Select HOLD placement mode. Left-click a Notes lane to place. Re-press `4` in world view to place at the crosshair hit. |
| `5` | Dodge | Select DODGE placement mode. Left-click a Notes lane to place. Re-press `5` in world view to place at the crosshair hit. |
| `6` | Event | Select effect/event authoring mode. |
| `7` | Transform | Select spatial transform mode. Drag a selected note in the world rect to move it on the camera plane; an axis gizmo marks the selection. |
| `8` | Test | In the shell, upload the applied chart and start/stop Test. In the world, start only when the uploaded chart is still current. |
| `9` | Timeline | Toggle the timeline drawer; from the world, reopen the shell. |

Slots `2`-`5` select a note type, then left-click on a timeline Notes lane places a note of
that type at the clicked beat. Right-click on a Notes lane still creates a default TAP note.
While the shell is hidden, re-pressing the active placement tool key places a note at the
crosshair hit on the selected track's plane (grid-snapped to 0.25); when no valid plane hit
exists it falls back to the default position, and before the first Test run (no anchor yet)
it reports "No preview anchor yet". Placement uses the anchor captured at `PREVIEW_READY`.

## Camera Modes

- `B`: Cycle the editor camera: Player → Free → Top → Front → Player. Works in the shell and in world view.
- Free: fly with `WASD` + `Space`/`Shift` (only while the shell is open and no text field is focused).
- Top: camera above the preview anchor looking straight down.
- Front: camera in front of the chart plane facing it.
- The camera detaches via a dummy entity; it restores on Player mode, shell close, or disconnect.
- A "Camera: …" label appears at the top-left of the HUD while the camera is detached.

## World Transform Drag

- With the Transform tool active and a note selected, left-drag anywhere in the world rect
  moves the note on the camera plane (depth-scaled so the note tracks the cursor).
- Deltas are inverse-rotated into track space, divided by track scale, snapped to the 0.25
  grid; HOLD notes keep `Z = -1.0`. Requires a captured anchor (run Test once).

## World-First Shell

- `F`: Hide the shell for normal Minecraft crosshair/camera control, or reopen it without rebuilding the editor session.
- The center of the shell is the already rendered Minecraft world, not a separate fake preview panel.
- The bottom track bar remains available when the timeline is collapsed.
- Minecraft owns camera input only while the shell is hidden; the open Screen owns the mouse cursor.
- Right-click an owner-visible Track or Note interaction in the world to select it and focus the shell.
- The hotbar labels above slots 1-9 appear only during an active editor session.

## Inspector

- Inspector text is staged until `Apply` is pressed.
- Save and Test serialize the applied chart model; they do not silently apply text currently left in Inspector fields.
- `Reset` restores the visible fields from the selected model object.

## Selection

- `Ctrl + Click`: Toggle a note, effect clip, or event clip in the current multi-selection.
- Drag on the focused timeline: Box-select notes, effect clips, and event clips.
- `Ctrl + A`: Select all notes.
- `Esc`: Clear timeline selection. It does not close the world-first shell.

## Clipboard And History

- `Ctrl + C`: Copy selected notes or effect clips.
- `Ctrl + X`: Cut selected notes or effect clips.
- `Ctrl + V`: Paste notes or effect clips at the playhead.
- `Ctrl + D`: Duplicate selected notes or effects.
- `Ctrl + G`: Group selected notes or effects.
- `Ctrl + Z`: Undo.
- `Ctrl + Y` or `Ctrl + Shift + Z`: Redo.
- `Ctrl + S`: Save the applied project state.

## Movement

- `Alt + Left/Right`: Move selected notes by the current shortcut step.
- `Alt + Shift + Left/Right`: Move selected notes by 1 beat.
- `Alt + Up/Down`: Move selected notes between tracks.
- `Delete` or `Backspace`: Delete the current selection.

## Timeline And Audio

- `Space`: Play or pause local audio while the shell is open.
- `R`: Start/stop local audio and server preview through the existing direct shortcut path.
- `Left/Right`: Move the playhead by 1/4 beat in the shell; seek 10 seconds while the shell is hidden.
- `Home/End`: Jump to the beginning/end of the timeline.
- `PageUp/PageDown`: Focus the timeline if needed, then scroll lanes.
- `+/-`: Zoom the timeline.
- `Ctrl + Mouse Wheel`: Zoom the focused timeline.
- Mouse wheel: Scroll timeline lanes; `Shift + Mouse Wheel` scrolls time.
- Click the ruler or Audio strip: Seek the playhead.
- Right-click a Notes lane: Add at the clicked beat.
- Right-click the FX Track header: Add an effect at the playhead.

## Smoothing

- `Ctrl + P`: Open the Smoothing popup.
- Smoothing uses selected notes as beat-ordered control points.
- `Fill: On` generates notes using the selected `1/x` division; `Fill: Off` modifies only selected notes.
- HOLD smoothing keeps generated and selected HOLD notes at `Z = -1.0`.

## Preview Lifecycle

- The compact transport strip separates local Play, Restart, Test, server Stop, and Auto/Judge mode.
- Test stays inside the transparent shell; `PREVIEW_READY` starts local audio without closing the Screen.
- Server Stop fences the current request. A ready preview pauses with its instance and overlay retained; an incomplete startup is cancelled safely. Quit/disconnect fully tears down a retained instance.
- Auto mode renders notes without judgement; Judge mode enables input and visual feedback.

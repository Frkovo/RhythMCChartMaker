# Unified Notes Lane + Inspector Redesign

## Goals

- Replace the three Note X/Y/Z lanes with a single **Notes** lane per expanded track.
- Move XYZ editing to an explicit **Inspector Transform card** and the improved **Scene Map**.
- Reduce timeline vertical clutter and make note editing more intuitive.

## Design

### Timeline

- Each expanded track has one Notes lane instead of three.
- Notes are drawn as type-shaped clips:
  - TAP: square
  - LOOK: diamond
  - HOLD: length rectangle
  - DODGE: X shape
- Color by note type. Selected notes have a bright outline.
- Interactions:
  - Click: select
  - Ctrl+Click: multi-select toggle
  - Drag: move note in time
  - Right-click: add a new note at the playhead on this track

### Inspector

- Add a **Transform** section for notes with three rows:
  - Position X | Y | Z
  - Scale X | Y | Z
  - Rotation X | Y | Z
- Remove the old 13-field effect-style flat layout where possible.

### Scene Map

- Top-down XY grid with numeric coordinate labels.
- Selected note shown as draggable dot with live coordinate readout.
- Z depth controlled by buttons (Z- / Z+ / Center) and/or a small slider.

## Out of Scope

- No in-GUI 3D preview; preview remains in-game after closing the GUI.
- Full top-menu/toolbar redesign is a later step.

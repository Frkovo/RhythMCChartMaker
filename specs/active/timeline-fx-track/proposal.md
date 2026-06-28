# Timeline FX Track Rewrite

## Problem

The current timeline puts Effects in a single thin lane at the top, below the ruler. The user wants a DaVinci-style timeline:

- Effects live in a dedicated **FX Track** at the bottom of the timeline.
- The FX Track is always present, can be expanded/collapsed like a Track header, and can show multiple effects simultaneously as horizontal clips.
- Timed effects (effects with a `duration`) are rendered as draggable clips with a start/end, and can be aligned/snap like note/event clips.
- Each effect clip must show enough identifying information (type + key text/value) so the author can recognize it at a glance.
- Authors can select a group of effects and bundle them into an editor-only **Group** (Copy/Paste supported). The same grouping workflow is desired for Notes.
- Groups are part of the editor draft only and must not appear in the compiled level JSON sent to the Preview plugin.
- Timeline grid/ruler granularity is too coarse; make it finer and label beat numbers clearly.
- New notes, effects, and other objects should be created at the current playhead (red line) position.

## Goals

1. Restructure timeline lanes so the FX Track sits at the bottom, after all Track lanes.
2. Support expand/collapse for the FX Track and render each effect as an independent clip row when expanded.
3. Render timed effect clips with a body, end handle, and label; support drag-to-move and snap.
4. Increase ruler/grid subdivision density and label every whole beat (or subdivision) number.
5. Ensure all "add" shortcuts/buttons insert at the playhead beat.
6. Add editor-only grouping/clipboard support for effects and notes without polluting the chart JSON contract.

## Non-Goals

- Do not change the `rhythmc:chart_preview` plugin channel or chart JSON semantics.
- Do not add server-side gameplay, auth, resource-pack, or network code.
- Do not change how the Preview plugin deserializes effects/notes.

## Risk Level

Medium-High: touches `ChartEditorScreen` rendering, `ChartEditorState` selection/clipboard, and editor-only persistence.

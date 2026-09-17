# Current State

Updated: 2026-08-17

## Origin

RhythMCChartMaker is a Fabric client mod: chart editor UI, local audio playback, `rhythmc:chart_preview` plugin channel client sender. It communicates with a RhythMC-Preview Paper server for visual preview playback.

## Major Work Completed

### World-first editor shell

- The multiplayer Minecraft world is now the transparent central editor canvas; the shell no longer paints an opaque full-frame background or vanilla blur.
- The vanilla nine-slot hotbar is the authoritative editor tool palette: Select, Tap, Look, Hold, Dodge, Event, Transform, Test, Timeline.
- Number keys choose tools instead of immediately creating notes. Slot 8 runs the inline Test loop and slot 9 toggles the timeline drawer.
- The compact transport, translucent side docks, always-visible track bar, and optional focused timeline leave the world visible while editing.
- `F` is the single global shell show/hide path. The shell owns editor ticks while open; the global client tick owns them while hidden.
- Server editor messages are directional: C2S 12/13 and S2C 107/108. World selection focuses the shell without an echo loop.

### Core chart/data/io

- Added chart model classes for manifest, meta, levels, tracks, notes, effects, BPM points, easing, etc.
- Added chart timing/math helpers such as easing functions, timing timeline, chart math, judge math scaffolding.
- Added project io for `manifest.yml` and chart difficulty files.
- Standardized project storage under `.minecraft/projects`.

### Editor state

- Built `ChartEditorState` to manage:
  - project create/load/save
  - active difficulty
  - selection state
  - playhead / scroll / zoom
  - add/delete/move notes, effects, BPM points, tracks
  - dirty/revision tracking
- Added editor snapshot/restore support for undo/redo history.

### Audio

- Added audio loading/playback support.
- Added waveform / basic BPM reference analysis.
- Fixed audio decoding regression by introducing explicit fallback decoders instead of relying only on `AudioSystem` service discovery.
- Improved audio load error reporting so the UI shows more specific failure details.

### In-world editing and world workflow

- Added world launch / editor world support.
- Added world display sync / pick helpers.
- Added in-world display/entity editing support through the world controller/launcher path.

### Project/title screens

- Added a title screen entry for RhythMC projects.
- Added project hub and project browser screens.
- Added new song/project creation flow.
- Added recent project handling.
- Reworked several custom screen render orders after text visibility issues.

### Timeline/editor UI work

- Reworked the timeline into labeled lanes instead of a flat strip.
- Replaced separate XYZ note lanes with one Notes lane per Track; Scene Map and Inspector retain explicit XYZ editing.
- Added larger note lanes and center-line visualization.
- Added event clip selection, edge dragging, and right-click split.
- Added note multi-select, event clip multi-select, and box select.
- Added magnetic snapping / snap guide line behavior.
- Added whole event clip dragging.
- Added a ruler and zoom bar in the timeline.
- Switched timeline grid/division lines to beat/BPM-oriented spacing.

### Note editing workflow improvements

- Split note property editing into separate fields for:
  - `Pos X`, `Pos Y`, `Pos Z`
  - `Scale X`, `Scale Y`, `Scale Z`
  - `Rot X`, `Rot Y`, `Rot Z`
- Added note clipboard/history shortcuts:
  - `Ctrl+A` select all notes
  - `Ctrl+C` copy notes
  - `Ctrl+X` cut notes
  - `Ctrl+V` paste notes at playhead
  - `Ctrl+Z` undo
  - `Ctrl+Y` / `Ctrl+Shift+Z` redo
  - `Alt+Left/Right` move selected notes by beat step
  - `Alt+Up/Down` move selected notes between tracks
  - `Esc` clears note selection

### Track event editor work

- Replaced raw track event string editing with a row-based editor flow.
- Added easing picker/search/grouping support for track event lanes.
- Added track event row copy/delete/reorder attempts.
- Track event lanes currently expose one inspector row, but Reborn supports multiple non-overlapping events per property channel; the current single-event normalization is a known data-loss defect.

### Important Discoveries / Fixes

- SnakeYAML crash was caused by incompatible indent/indicator indent configuration. Removing the custom indicator indent fixed it.
- Minecraft 1.21 screen blur error (`Can only blur once per frame`) happened when background rendering was duplicated. Removing duplicate background handling fixed that issue.
- AWT file picker was unreliable in-game. Switched to TinyFileDialogs.
- Editor reopen behavior in-world required explicit close handling and better hotkey edge logic.
- Reborn track event channels support multiple sorted, non-overlapping clips; treating them as single-event lanes is incorrect.

## Current Known Problems / Risks

- The user still considers the UI bad and not sufficiently rewritten.
- `ProjectHubScreen` and `ProjectBrowserScreen` recently had missing card text due to render order; the text is now drawn after `super.render` so it layers above widgets. In-game visual validation is still required.
- The easing popup had repeated visibility/layout problems. Multiple fixes were attempted, but this area should still be treated as fragile until visually confirmed in-game.
- `ChartEditorScreen` has accumulated many iterative edits and should likely be simplified/restructured instead of patched further.
- Top bar fully rewritten (2026-08-14): two compact rows (52px), no path text field, no empty menu tabs; New/Open go through wizard/browser, Save uses the project path directly; difficulty is a compact segmented control with the active entry highlighted.
- Visual consistency across title/project/editor screens is still not trustworthy without manual in-game review.
- The world-first shell and owner-only world selection still require in-game GUI-scale and two-player validation.
- An open Minecraft Screen owns the cursor, so `F` must hide the shell before direct camera/crosshair interaction.
- Preview selection still identifies notes by mutable list index, and `CHART_LOAD` does not hot-apply to an already running paused instance.
- Inspector input remains staged until Apply; Test and Save use only the applied model state.

## Workspace Notes

- Repeated verification command: `./gradlew compileClientJava`
- Latest compile status: successful.
- There is an untracked `.tmp-jgui/` directory left in the repo root.

## Recommended Next Direction

The next safe sequence is:

1. Validate the transparent shell, hotbar labels, timeline drawer, and owner-only overlay in a live two-player setup.
2. Fix lossless chart handling before expanding world authoring: multi-event lanes, Effect schemas, unknown fields, and validation parity with Reborn.
3. Add stable editor object IDs and hot-apply chart changes to the retained paused Preview instance.
4. Add direct world placement/transform gizmos and deterministic seek only after those foundations are reliable.

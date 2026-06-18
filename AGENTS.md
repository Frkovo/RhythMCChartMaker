# AGENTS.md

## Project Goal

Build a Fabric client-side RhythMC chart editor mod with:

- full chart editing for song manifest, meta, tracks, notes, effects, and BPM
- in-world editing / preview support
- audio playback, sync, and waveform support
- project creation/loading from the title screen
- strong semantic compatibility with `E:/Dev/RhythMC-Reborn`

## User Requirements And Preferences

- Reborn compatibility matters. Track event semantics should follow Reborn utilities/ChartUtils behavior.
- Track `Speed`, `X/Y/Z Transform`, `X/Y/Z Rotation`, and `X/Y/Z Scale/Stretch` should each allow only one event per track lane.
- Note coordinates are center-relative and can be decimal values.
- Note `X`, `Y`, and `Z` should be edited separately, not packed into one confusing field.
- The UI should feel closer to an editor such as Visual Maimai / video timeline tools, not a generic debug panel.
- Important note editing shortcuts are expected: multi-select, copy, cut, paste, move, undo, redo.
- The user is currently unhappy with the editor UI quality and expects broader rewrites rather than tiny cosmetic patches.

## Major Work Completed So Far

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
- Added separate note lanes for `Note X`, `Note Y`, and `Note Z`.
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
- Later aligned track lanes with Reborn semantics so each lane normalizes to a single event.

## Important Discoveries / Fixes

- SnakeYAML crash was caused by incompatible indent/indicator indent configuration. Removing the custom indicator indent fixed it.
- Minecraft 1.21 screen blur error (`Can only blur once per frame`) happened when background rendering was duplicated. Removing duplicate background handling fixed that issue.
- AWT file picker was unreliable in-game. Switched to TinyFileDialogs.
- Editor reopen behavior in-world required explicit close handling and better hotkey edge logic.
- Reborn track event semantics are not free-form multi-event lanes in practice for these track properties; they should be treated as single-event lanes.

## Visual Maimai Reference

The user explicitly asked to follow Visual Maimai ideas and images:

- doc: `https://visual-maimai-manual.github.io/guide/gui.html`
- important reference concepts from that doc:
  - menu-style top controls (`File / Edit / Options`)
  - note editing shortcuts and clipboard workflow
  - clearer editor zoning between note tools, preview, and track area

## Current Code/Behavior State

- `ChartEditorScreen` has been heavily modified multiple times.
- `ChartEditorState` now contains snapshot/restore logic for undo/redo.
- `TitleScreenMixin`, `NewSongWizardScreen`, `ProjectHubScreen`, and `ProjectBrowserScreen` were all edited during UI fixes.
- The top bar was moved toward a `File / Edit / Options` tabbed/menu-like layout.
- Note properties were split into separate XYZ fields.
- Timeline note lanes no longer intentionally draw long per-note text strings on every note block.

## Current Known Problems / Risks

These are the most important current risks and unresolved issues known from the conversation:

- The user still considers the UI bad and not sufficiently rewritten.
- Several custom screens previously showed panels but missing text due to render order/layering problems. Some fixes were applied, but in-game visual validation is still required.
- The easing popup had repeated visibility/layout problems. Multiple fixes were attempted, but this area should still be treated as fragile until visually confirmed in-game.
- `ChartEditorScreen` has accumulated many iterative edits and should likely be simplified/restructured instead of patched further.
- The top menu is only a tabbed/menu-like replacement right now, not a full dropdown menu system yet.
- Visual consistency across title/project/editor screens is still not trustworthy without manual in-game review.

## Files Most Recently Involved

- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/editor/ChartEditorScreen.java`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/editor/ChartEditorState.java`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/project/ProjectBrowserScreen.java`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/project/ProjectHubScreen.java`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/wizard/NewSongWizardScreen.java`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/mixin/client/TitleScreenMixin.java`

## Current Workspace Notes

- No `AGENTS.md` existed before this file was created.
- The workspace has uncommitted UI/editor changes.
- There is also an untracked `.tmp-jgui/` directory left in the repo root.
- Repeated verification command used during this session: `./gradlew compileClientJava`
- Latest compile status before writing this file: successful.

## Recommended Next Direction

If work continues, the safest next step is not more patching on top of the current UI. The recommended path is:

1. fully redesign `ChartEditorScreen` layout around three clear zones: tools/menu, preview, timeline/inspector
2. keep note XYZ editing separate everywhere
3. keep track property lanes as single-event inspector cards instead of pseudo-table clutter
4. validate every rewritten screen in-game before assuming text/layering is fixed

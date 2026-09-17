# Design

## Summary

Use the already rendered multiplayer world as the editor canvas. A normal
non-pausing `ChartEditorScreen` suppresses vanilla background blur/dimming and
draws only translucent edge UI. The existing timeline becomes a bottom drawer.
The selected vanilla hotbar slot stores the current editor tool even when the
shell is hidden.

## Tool Mapping

| Slot | Tool | First-phase behavior |
|---:|---|---|
| 1 | Select | World/timeline selection mode |
| 2 | Tap | TAP placement mode |
| 3 | Look | LOOK placement mode |
| 4 | Hold | HOLD placement mode |
| 5 | Dodge | DODGE placement mode |
| 6 | Event | Effect/event authoring mode |
| 7 | Transform | Spatial transform mode |
| 8 | Test | Start/stop the current preview loop |
| 9 | Timeline | Toggle the timeline drawer in the shell |

Placement and transform tools are explicit modes in this phase. Existing
timeline and Scene Map creation remain available; direct world placement is a
follow-up after live chart synchronization and stable IDs.

## Shell Layout

- The Screen overrides `renderBackground` as a no-op.
- No full-frame editor fill is drawn.
- Top/status bars and left/right docks remain translucent.
- The center contains a compact transport strip, transparent world rectangle,
  track bar, and optional focused timeline plus scrollbar.
- Collapsed timeline geometry is not drawn or hit-tested.
- `EditorLayout` is the single source for world and timeline content bounds.
- The shell owns `ChartEditorState.tick()` while visible; the global client tick
  owns it while hidden.
- The global `F` edge handler is the only shell show/hide owner.

## Preview Behavior

Inline shell actions upload/start without setting the old
`openWorldPreviewOnReady` flag. `PREVIEW_READY` starts local audio but leaves the
Screen open. Hiding the shell with `F` exposes normal crosshair/camera input.

## Directional Editor Messages

All messages use big-endian `int opcode`, then `int UTF-8 byte length`, then raw
UTF-8 bytes.

| Direction | Opcode | Name | Behavior |
|---|---:|---|---|
| C2S | 12 | `EDITOR_OPEN` | Request editor focus; server may answer with 107 |
| C2S | 13 | `EDITOR_SELECT` | Store client selection; no echo |
| S2C | 107 | `EDITOR_OPEN` | Focus client shell; never sends 12 |
| S2C | 108 | `EDITOR_SELECT` | Apply world selection and focus shell; never sends 13 |

Protocol version remains 2 because the written v2 contract already reserves
12/13. Mixed old binaries remain a deployment risk and must be reported.

## Owner-Only Overlay

Overlay entities remain `visibleByDefault=false`. After spawn, the plugin calls
`Player.showEntity` only for the owning player. Interaction events are accepted
only when the clicked entity is still owned by that player's current
`EditorOverlayManager`; off-hand duplicate interaction events are ignored.

## Risks

- A Screen cannot pass normal camera input through; `F` is still required for
  direct world interaction in this phase.
- Existing note index selection is unstable after sorting.
- `CHART_LOAD` does not yet hot-apply to a running paused instance.
- Existing Inspector widgets may still need manual visual validation at narrow
  GUI scales.
- Both repositories contain unrelated dirty work and untracked implementation
  files; changes must stay narrowly scoped.

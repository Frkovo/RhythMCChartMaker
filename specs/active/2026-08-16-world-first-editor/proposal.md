# Proposal: World-First Editor Shell

## Goal

Turn the existing full-screen chart editor into a world-first editing shell. The
Minecraft world is the central preview surface, the vanilla hotbar is the tool
palette, the timeline owns time, and the Inspector owns precise values.

## Requirements

- Keep the Minecraft world visible behind the editor without a custom world
  framebuffer or embedded renderer.
- Use the selected vanilla hotbar slot as the authoritative editor tool:
  `Select`, `Tap`, `Look`, `Hold`, `Dodge`, `Event`, `Transform`, `Test`, and
  `Timeline`.
- Number keys select tools instead of immediately creating notes.
- Keep a compact transport strip, edge panels, an always-visible track bar, and
  a timeline that can expand for focused time editing.
- Starting or syncing a preview from the shell must not close the shell.
- Fix directional editor opcodes and owner-only world overlay interactions in
  both ChartMaker and RhythMC-Preview.
- Preserve all existing chart data and current dirty-worktree behavior.

## Acceptance Criteria

- The central world remains visibly unobstructed while the editor Screen is
  open.
- Slots 1-9 select and label the documented tools in both shell and world view.
- Slot 8 starts/stops the current preview test loop; slot 9 toggles timeline
  focus while the shell is open.
- `F` remains the single shell show/hide action and does not double-toggle.
- C2S editor messages use opcodes 12/13; S2C editor messages use 107/108.
- Only the preview owner sees and can activate that preview's editor overlay.
- Both repositories compile.

## Out Of Scope

- Stable chart object IDs or incremental chart patches.
- New chart JSON fields or parsing semantics.
- Full in-world note placement and 3D transform gizmos.
- Deterministic arbitrary-beat effect reconstruction.
- Orthographic/free cameras, custom rendering, or UI skin replication.
- Fixing all pre-existing chart round-trip and Inspector correctness issues.

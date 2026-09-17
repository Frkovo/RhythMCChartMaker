# Proposal: Plugin becomes Preview-Only MVP

## Goal

Strip the RhythMC-Preview plugin down to a **preview-only MVP**: the smallest
plugin that turns a chart JSON into visible entities in the world, in sync with
a beat clock. Audio stays in the mod. No gameplay leftovers.

## Requirements

- Plugin's only job: receive chart over `rhythmc:chart_preview`, render notes +
  effects + arena, advance a BPM clock, honor start/stop/restart.
- **Two preview modes**, selectable in the mod editor per preview:
  - `AUTO`: notes fly along tracks and despawn at the rail end. No player
    input, no judgement, no feedback. Default.
  - `JUDGE`: classic hit judgement (hit windows, input, feedback) retained.
- Mode is sent by the mod in `PREVIEW_START` as a trailing `byte` (0=AUTO,
  1=JUDGE). `PREVIEW_RESTART` does not resend the mode; the running instance
  keeps its mode.
- Delete from the plugin: Economy, Scoreboard, Guidance (bossbar/judge
  messages), StatisticManager, gameplay config options (NoteSkin, ParticlesQty,
  IndicatorPos, scoreboard lines, bossbar type, collision indicator), unlock
  logic, stats records.
- Keep: deserializers, timing, note rendering, effect rendering, arena
  paste/restore, per-player state, plugin channel server.
- Judgement code is gated behind mode; it may be kept structurally but must not
  run (or register listeners) in AUTO mode.

## Out of Scope

- No new opcodes; only a payload addition to `PREVIEW_START` (opcode 3).
- No chart JSON format changes.
- No mod-side gameplay reintroduction.

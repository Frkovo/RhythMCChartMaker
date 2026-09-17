# Tasks

## Planning

- [x] Audit ChartMaker shell, input, and preview lifecycle.
- [x] Audit Preview overlay visibility and editor message flow.
- [x] Classify as high-risk cross-repository contract work.
- [x] Define first-phase boundaries and acceptance criteria.

## ChartMaker

- [x] Add the nine-slot editor tool model backed by the vanilla hotbar.
- [x] Replace immediate number-key note creation with tool selection.
- [x] Add tool labels to the in-game HUD.
- [x] Make the editor background transparent and reserve a world canvas.
- [x] Add compact inline transport and collapsed/focused timeline layout.
- [x] Remove duplicate editor ticks and duplicate `F` handling.
- [x] Keep server-originated editor focus idempotent.
- [x] Split editor opcode constants by direction.

## Preview Plugin

- [x] Split editor opcode constants by direction.
- [x] Enforce one-way/no-echo editor message semantics.
- [x] Show editor overlay entities only to their owner.
- [x] Validate interaction ownership and suppress off-hand duplicates.
- [x] Centralize safe overlay removal.

## Documentation

- [x] Update both contract documents and AGENTS summaries.
- [x] Update ChartMaker shortcuts and both CURRENT files.
- [x] Add both changelog entries.

## Validation

- [x] Compile ChartMaker client sources.
- [x] Compile Preview plugin.
- [x] Review both diffs without reverting unrelated changes.
- [x] Record validation and residual risks in `handoff.md`.
- [ ] Run live Minecraft visual checks at representative GUI scales.
- [ ] Run the two-player owner-only visibility/interaction test.

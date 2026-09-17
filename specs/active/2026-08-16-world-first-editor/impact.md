# Impact

## Repositories

- [x] ChartMaker: `E:/Dev/RhythMCChartMaker`
- [x] Preview plugin: `E:/Dev/RhythMC-Preview`
- [x] Shared channel documentation

## Contract Impact

- Corrects editor message directions to C2S 12/13 and S2C 107/108.
- Defines no-echo selection semantics to prevent protocol loops.
- Clarifies that `PREVIEW_STOP` fences the current generation: stable previews pause and retain their instance, while incomplete startup is cancelled safely.
- Does not change chart JSON, chunking, trust, or protocol version.

## UX Impact

- World becomes the central authoring preview.
- Vanilla hotbar slots become the primary tool palette.
- Number keys no longer create notes immediately.
- Preview start remains inline instead of closing the shell.

## Required Documentation

- [x] `.agent/CONTRACTS.md`
- [x] `.agent/CURRENT.md`
- [x] `AGENTS.md` opcode summary
- [x] `docs/editor-shortcuts.md`
- [x] `docs/CHANGELOG.md`
- [x] Preview `RhyDocs/docs/api/plugin-channels.md`

## Validation

- [x] `./gradlew compileClientJava`
- [x] `mvn compile` in RhythMC-Preview
- [x] Diff review in both dirty worktrees
- [ ] Live two-player owner-visibility test remains manual

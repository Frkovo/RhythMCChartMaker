# Checklists

Updated: 2026-06-23

## Universal Finish Checklist

- [ ] `git status --short` was run.
- [ ] Unrelated dirty changes were not overwritten or reverted.
- [ ] The change is the smallest correct implementation.
- [ ] Relevant docs were updated for behavior, config, protocol, or workflow changes.
- [ ] `E:/Dev/RhythMCChartMaker/docs/CHANGELOG.md` has an entry using `## yyyy-MM-dd : MOD : 更改内容`.
- [ ] Relevant task `handoff.md` records completed work, validation, risks, and next steps.
- [ ] Residual risks and unverified paths are reported in the final response.

## Mod Checklist

- [ ] No server-side-only code was pulled into the client source set.
- [ ] Mixin screens handle render order properly (no `Can only blur once per frame`).
- [ ] Audio playback stays local; no server-side song playback was reintroduced.
- [ ] Plugin channel changes update both this repo and `E:/Dev/RhythMC-Preview`.
- [ ] Chart JSON serialization stays compatible with Preview plugin `File/Deserializers/*`.
- [ ] Editor UI changes preserve existing keyboard shortcuts.
- [ ] Validation: `./gradlew compileClientJava` when feasible.

## Channel Contract Checklist

- [ ] `.agent/CONTRACTS.md` was reviewed.
- [ ] Both this repo and `E:/Dev/RhythMC-Preview` were updated for channel changes.
- [ ] `ChartPreviewChannel.java` opcodes match the Preview plugin's `ChartPreviewChannel`.
- [ ] Opcodes, payload schema, chunking, lifecycle, and trust model are documented.
- [ ] Success and error responses are documented.

## Contract Change Checklist (when channel changed)

- [ ] Every affected repo was updated or the scope limitation was explicitly recorded.
- [ ] Preview plugin build was checked (`mvn compile`).
- [ ] `docs/` was updated.

## Review Checklist

- [ ] Review diff for unintended file changes.
- [ ] Check for channel contract drift between this repo and the Preview plugin.
- [ ] Check for chart JSON semantic drift between this repo and the Preview plugin.
- [ ] Check for missing docs/changelog/validation.
- [ ] Check for secrets in modified files.

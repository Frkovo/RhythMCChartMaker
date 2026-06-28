# Agent Workflow

Updated: 2026-06-23

## Goal

Keep chart-maker work Git-visible, reviewable, and contract-aware. The `rhythmc:chart_preview` channel is a real contract shared with the Preview plugin.

## Start Procedure

Every agent session starts by classifying the task before editing files.

1. Read `AGENTS.md`.
2. Read `.agent/CURRENT.md`.
3. Read `.agent/PROJECT.md` and `.agent/REPOS.md` if project context is needed.
4. If the `rhythmc:chart_preview` channel, chart parsing, or preview lifecycle may change, read `.agent/CONTRACTS.md`.
5. If a task spec exists, read `specs/active/<task>/proposal.md`, `impact.md`, `tasks.md`, and `handoff.md`.
6. Check `git status --short` in every affected repository.
7. Decide risk level: Low, Medium, or High.
8. Decide whether the task crosses a contract boundary.

## Risk Levels

### Low

Small isolated changes with no channel, chart-parsing, editor-state, or preview-lifecycle impact.

Required:

- Inspect relevant files.
- Make minimal changes.
- Run `./gradlew compileClientJava` when feasible.
- Update changelog if behavior or repo state changed.

### Medium

Single-repo features, non-critical refactors, or behavior changes that do not alter the `rhythmc:chart_preview` channel or chart JSON semantics.

Required:

- Create or update `specs/active/<task>/proposal.md` and `tasks.md`.
- Record affected files and docs in `impact.md`.
- Implement in small patches.
- Run repository validation (`./gradlew compileClientJava`).
- Update docs and changelog.
- Update `handoff.md`.

### High

Changes to the `rhythmc:chart_preview` channel (opcodes, payload schema, chunking, lifecycle, trust model), chart JSON parsing semantics, or that also touch the Preview plugin at `E:/Dev/RhythMC-Preview`.

Required:

- Create or update the full task spec: `proposal.md`, `design.md`, `impact.md`, `tasks.md`, `handoff.md`.
- Inspect both this repo and the Preview plugin before editing.
- Update both repos in the same task.
- Update docs and `docs/CHANGELOG.md`.
- Run all applicable build/compile checks (`./gradlew compileClientJava` here, `mvn compile` in Preview).
- Record residual risk and unverified paths in `handoff.md` and the final response.

## Implementation Rules

- Prefer the smallest correct change.
- Do not overwrite unrelated dirty worktree changes.
- If existing dirty files conflict with the task, stop and ask the user.
- Keep business logic in the appropriate service/module, not in UI glue.
- Do not add backward compatibility unless there is a concrete persisted-data, shipped-contract, or explicit user requirement.
- Do not put secrets, tokens, API keys, or credentials in repo files.

## Finish Procedure

Before finishing a completed modification:

1. Re-run `git status --short` for affected repositories.
2. Review the diff for files touched by this task.
3. Run applicable validation from `.agent/CHECKLISTS.md`.
4. Update docs for behavior, config, protocol, or workflow changes.
5. Add a `docs/CHANGELOG.md` entry using `## yyyy-MM-dd : MOD : 更改内容`.
6. Update the task `handoff.md` with completed work, validation, risks, and next steps.
7. If complete, move the spec from `specs/active/` to `specs/done/` only when the user asks or when the task is fully delivered and validated.

## Review Procedure

For high-risk work, use a separate review pass before finalizing.

Review focus:

- Behavioral regressions in the editor UI.
- Channel contract drift between this repo and the Preview plugin.
- Chart JSON parsing semantic drift.
- Missing docs, changelog, or validation.
- Dirty worktree conflicts.

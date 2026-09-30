# AGENTS.md

RhythMCChartMaker is a Fabric client mod that lets charter authors create and preview RhythMC charts. It connects to a RhythMC-Preview Paper server over the `rhythmc:chart_preview` plugin channel for visual playback; the mod plays audio locally.

## Read First

Before editing, agents should read these files in order:

- `AGENTS.md`
- `.agent/CURRENT.md`
- `.agent/WORKFLOW.md`
- `.agent/PROJECT.md` when project context is needed
- `.agent/REPOS.md` when repository paths, commands, or entry points are needed
- `.agent/CONTRACTS.md` when the `rhythmc:chart_preview` channel, chart parsing, or preview lifecycle may change
- `specs/active/<task>/` when a task spec exists

Use `specs/` for medium/high-risk task planning and handoff. Use `docs/CHANGELOG.md` for completed behavior/workflow records.

## Repository Map

| Area | Path | Responsibility |
|---|---|---|
| Chart maker mod | `D:/Dev/RhythMCChartMaker` | This repo. Fabric client mod: chart editor UI, local audio playback, `rhythmc:chart_preview` client sender, chart project IO |
| Preview plugin | `D:/Dev/RhythMC-Preview` | Paper plugin, preview-only MVP: chart visual playback, arena paste, file cache, `rhythmc:chart_preview` server receiver/sender. No gameplay/scoreboard/economy/stats code. |
| Full-game plugin | `D:/Dev/RhythMC-Reborn` | Reference for chart JSON semantics and gameplay behavior |
| Agent memory | `D:/Dev/RhythMCChartMaker/.agent` | Current state, workflow, repo map, contracts, checklists |
| Task specs | `D:/Dev/RhythMCChartMaker/specs` | Backlog, active task specs, handoff, completion records |

## Prime Directive

This repo is a chart editor. Before adding anything, ask whether it is required for chart editing, audio playback, or preview integration. If it is not, do not add it. Reintroducing server gameplay, network/auth, or resource-pack code is a regression.

The two shared contracts are the `rhythmc:chart_preview` plugin channel and the chart JSON format. If either changes (opcodes, payload schema, chunking, lifecycle, trust model, field names, types, defaults), update this repo, the Preview plugin, and the docs in the same task.

### Contract change definition

A task crosses a contract boundary when it changes any of these:

- `rhythmc:chart_preview` plugin channel opcode set, direction, payload schema, chunking protocol, lifecycle, or trust model.
- Chart JSON parsing semantics (field names, types, defaults) that the Preview plugin's `File/Deserializers/*` also follows.
- Mod config keys or user workflow.
- Preview lifecycle behavior (start/stop/restart semantics, clock sync).

## Non-Negotiable Rules

### Safety

- Never run destructive Git commands: `git revert`, `git rebase`, `git checkout --`, `git reset --hard`, `git push --force`, or equivalent.
- Do not revert unrelated dirty worktree changes.
- Do not write backward-compatibility code, migration scripts, or deprecated-schema tracking unless explicitly requested. There is no production data.
- Do not commit credentials, tokens, secrets, or API keys.
- If secret material is found in tracked docs or Agent files, remove it from the file and report that the credential should be rotated.

### Scope Discipline

- Do not reintroduce server-side gameplay logic, scoring, stat upload, matchmaking, or parties.
- Do not add HTTP/WebSocket network code. The only transport is the Bukkit plugin channel.
- Do not add player auth/session. Server identity = online Bukkit `Player`.
- Do not add resource pack sending. Audio plays locally; the Preview plugin caches uploaded audio but never plays it.

### Documentation

- Update docs before finishing any completed behavior, config, protocol, or workflow change.
- When unsure whether docs are needed, update docs.
- After every completed modification, add a changelog entry to `docs/CHANGELOG.md` using the heading `## yyyy-MM-dd : MOD : 更改内容`.

### Trust

- The plugin channel receiver is a Bukkit `Player` currently online on the preview server. No token, no session binding.
- The mod sends `HELLO` on join; the server validates the protocol version and responds `HELLO_ACK`.
- The editor only unlocks when `HELLO_ACK.ok = true`.

## Architecture Boundaries

### This mod owns ONLY

- Chart editor UI: `ChartEditorScreen`, `ChartEditorState`, timeline, note/track/effect editing, undo/redo.
- Chart project IO: `manifest.yml` + level JSON files under `.minecraft/projects`.
- Audio playback: Java Sound with explicit SPI decoders (vorbis, mp3, flac, wav, aiff, au).
- Plugin channel client: `PreviewClient` + `ChartPreviewPayload` + `ChartPreviewChannel` constants.
- In-world launcher stub (deprecated; retained during rewrite).

### Explicitly out of scope

- Server-side gameplay or visual rendering. The Preview plugin handles visuals.
- Song audio distribution, resource packs.
- Scoring, stat records, gameplay record upload.
- HTTP/WebSocket networking, auth, session management.
- Economy, unlocks, collections.
- Matchmaking, parties, rivals, chat, leaderboards.

## Plugin Channel Contract

The only contract: `rhythmc:chart_preview` (bidirectional Bukkit plugin channel).

- Direction C→S: `HELLO`, `CHART_LOAD` (chunked if large), `PREVIEW_START` (trailing `byte mode`: 0=Auto, 1=Judge), `PREVIEW_STOP`, `PREVIEW_RESTART`, `FILE_UPLOAD_*`, `EDITOR_OPEN` (12), `EDITOR_SELECT` (13).
- Direction S→C: `HELLO_ACK`, `CHART_LOAD_ACK`, `PREVIEW_READY`, `PREVIEW_STOPPED`, `ERROR`, `FILE_UPLOAD_ACK`, `EDITOR_OPEN` (107), `EDITOR_SELECT` (108).
- Payload: raw bytes with big-endian `int` opcode prefix; strings are `int byteLength + UTF-8 bytes`.
- Trust: any online `Player` sender. No token.
- Lifecycle: HELLO → optional FILE_UPLOAD → LOAD → START → READY → STOP (pause/retain when stable, cancel incomplete startup) or RESTART (seek/resume retained instance).

Full opcode table and encoding details in `.agent/CONTRACTS.md`.

## Cross-Repo Contract Map

When modifying one of these, inspect and update every listed area.

| Contract | Main identifiers | Affected areas |
|---|---|---|
| `rhythmc:chart_preview` channel | Opcodes 1–11, 101–106, `ChartPreviewChannel`, `PreviewClient` | ChartMaker ↔ Preview ↔ Docs |
| Chart JSON format | `manifest.yml`, level JSON fields (tracks, notes, effects, events) | ChartMaker ↔ Preview ↔ Reborn (reference) |

## Mod Rules

- Use Fabric Loom's `splitEnvironmentSourceSets()`. Client-only code belongs in `src/client/`.
- Register custom payload types and global receivers in `RmcChartClient.onInitializeClient()`.
- Keep `ChartEditorState` as the single source of truth for editor state; use snapshot/restore for undo/redo.
- Audio decoding uses explicit SPI fallback chain. Do not rely solely on `AudioSystem` service discovery.
- Chart project IO must produce files compatible with the Preview plugin's `File/Deserializers/*`.
- Keep screen rendering order in mind. Avoid duplicate background handling (`Can only blur once per frame`).
- Plugin channel opcode constants in `ChartPreviewChannel.java` must match the Preview plugin's `ChartPreviewChannel` exactly.
- Do not hardcode plugin-facing messages. Prefer `Text.literal()` with descriptive prefixes like `"RhythMCChartMaker: "`.

## Start Here by Task Type

### Editor UI / timeline

- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/editor/ChartEditorScreen.java`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/editor/ChartEditorState.java`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/project/`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/wizard/`

### Plugin channel / preview transport

- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/net/PreviewClient.java`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/net/ChartPreviewChannel.java`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/net/ChartPreviewPayload.java`

### Client lifecycle / entrypoints

- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/RmcChartClient.java`
- `src/client/java/cn/frkovo/rhythmcv2/rmcChart/mixin/client/`

### Chart model / IO

- `src/main/java/cn/frkovo/rhythmcv2/rmcChart/chart/`

## Build and Validation Commands

```text
# Chart maker mod
./gradlew compileClientJava

# Preview plugin (when channel changes)
cd D:/Dev/RhythMC-Preview && mvn compile
```

## Schema Registry Summary

The full opcode/contract schema lives in `.agent/CONTRACTS.md`. This summary identifies the affected contract group quickly.

### Plugin Channel Opcode Groups

| Direction | Opcodes | Purpose |
|---|---|---|
| C→S | `HELLO` (1), `CHART_LOAD` (2), `CHART_LOAD_CHUNK_*` (6–8) | Handshake & chart transfer |
| C→S | `PREVIEW_START/STOP/RESTART` (3–5) | Preview lifecycle control |
| C→S | `FILE_UPLOAD_START/CHUNK/END` (9–11) | Schematic/audio upload |
| C→S | `EDITOR_OPEN/SELECT` (12–13) | Request editor focus / store client selection without echo |
| S→C | `HELLO_ACK` (101), `CHART_LOAD_ACK` (102), `PREVIEW_READY` (103), `PREVIEW_STOPPED` (104) | Lifecycle confirmation |
| S→C | `ERROR` (105), `FILE_UPLOAD_ACK` (106) | Error & upload status |
| S→C | `EDITOR_OPEN/SELECT` (107–108) | Focus shell / apply owner world selection |

### Payload Encoding

All opcodes use big-endian `int` prefix. Strings are `int byteLength + UTF-8 bytes`. Chunks encode `int byteLength + raw bytes`. No Java `writeUTF`, no Minecraft varint/`writeString`.

## Known Caveats

- The user considers the editor UI still not sufficiently rewritten. `ChartEditorScreen` has accumulated many iterative patches.
- Several custom screens previously had text visibility issues due to render order/layering. Validate in-game after screen changes.
- The easing popup is fragile after repeated layout fixes.
- `ChartEditorScreen` should likely be restructured, not further patched.
- The top menu is a tabbed replacement, not a full dropdown menu system yet.
- All three repos (`RhythMCChartMaker`, `RhythMC-Preview`, `RhythMC-Reborn`) may have dirty worktrees mid-refactor.
- `docs/CHANGELOG.md` and `.agent/` files may drift from actual behavior. When in doubt, inspect code.
- The `EditorWorldLauncher` is a deprecated stub retained during the rewrite. Do not rely on its methods.

## Finish Checklist

Before finishing, verify all applicable items:

- All affected repositories were inspected and updated for channel/contract changes.
- Docs were updated.
- `docs/CHANGELOG.md` entry was added using `## yyyy-MM-dd : MOD : 更改内容`.
- Plugin channel opcodes match between `ChartPreviewChannel.java` and the Preview plugin.
- Chart JSON serialization stays compatible with Preview plugin `File/Deserializers/*`.
- No server gameplay / auth / resource-pack code was reintroduced.
- `./gradlew compileClientJava` was run when feasible.
- Residual risk and unverified paths were reported clearly.

## Cross-Repo Memory & Standing Protocol (2026-09)

- For live cross-repo state (what the plugin/backend agents just did, what's parked or decided — e.g. judgment-granularity and chart-i18n decisions that affect chart semantics), read `D:/Dev/RhythMC-AGENTS/tasks/active/current-focus.md` first. 30s onboarding, avoids redoing decided work.
- Standing instruction: auto-commit + push after each finished task; stage only your own file paths (never `git add -A`); incorporate compatible changes from parallel agents, never force-push. Big work (contract/channel/chart-format changes, delete-rename, breaking, new dep, irreversible) asks the user first — contract changes additionally require updating ChartMaker + Preview + docs in the same task (see Contract change definition above).
- Chart i18n is embedded in the chart files, not sidecar (see `RhythMC-Reborn/AGENTS.md` cross-repo conventions + `docs/chart-i18n-design.md` there). Chart JSON written by this mod must follow it: `name`/`composer`/`charters` never localized, `_`-prefixed keys are tool metadata.

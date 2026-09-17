# Task Breakdown

## Plugin: strip to preview-only

- [ ] Delete `Economy/` package (unlock logic).
- [ ] Delete `Game/Scoreboard/` (PlayerScoreboard, rendered scoreboards) and its
      wiring in `Main.java` (scoreboards map, onLogin/onQuit, default lines).
- [ ] Delete `Game/Guidance/` (bossbar, judge messages, particle guidance used
      only by judgement).
- [ ] Delete `StatisticManager` + `StatsRecord` + stats wiring.
- [ ] Collapse gameplay config: `GameOptions` -> fixed constants; remove
      `NoteSkin`, `ParticlesQty`, `IndicatorPos`, `SbCustomizedType`,
      `BossbarType`, `CiCustomizedType` from the preview path.
- [ ] `Main.java` onEnable reduced to: config, arena, channel, tick loop.
- [ ] `GameInstance`/`GameManager` slimmed: keep load/seek/stop; remove
      pause/resume/stat/reload gameplay paths not needed by preview.
- [ ] `mvn compile` green.

## Plugin: mode byte + AUTO path

- [ ] `ChartPreviewHandler.handlePreviewStart` reads trailing `byte mode`.
- [ ] `PreviewGameRunner.startPreview(player, startBeat, mode)` forwards mode.
- [ ] AUTO mode: notes despawn at rail end, no input listeners, no
      JudgeManager/FeedbackManager execution.
- [ ] JUDGE mode: existing judgement behavior.
- [ ] `mvn compile` green.

## Mod: contract + editor toggle

- [ ] `PreviewClient.sendPreviewStart` appends mode byte.
- [ ] Editor UI toggle (Auto/判定) in the preview/play control area; state
      stored in `ChartEditorState`.
- [ ] `./gradlew compileClientJava` green.

## Docs

- [ ] `.agent/CONTRACTS.md`: document mode byte in `PREVIEW_START` payload.
- [ ] `AGENTS.md`: update plugin responsibility map + opcode note.
- [ ] `docs/CHANGELOG.md` entry: `## yyyy-MM-dd : MOD : 更改内容`.

## Pending / Follow-up

- [ ] In-game validation: AUTO despawn behavior, JUDGE feedback, arena
      restore on stop.
- [ ] Decide whether `FeedbackManager` survives as JUDGE-only.

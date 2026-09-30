# Impact Analysis

## Plugin (D:/Dev/RhythMC-Preview)

- **Deleted**: `Game/Scoreboard/` (7 classes), `Game/Guidance/BossBar/` (6
  classes), `Game/Guidance/JudgeMessages/ActionbarGuidance.java`,
  `Game/Utils/Enums/SbCustomizedType.java`, `BossbarType.java`,
  `CiCustomizedType.java`, `Game/Utils/GameData.java`, `Economy/` package.
- **Slimmed**:
  - `GameOptions` -> `(defaultArena, playerSpeed, noteSkin, judgeSound,
    particlesQty, indicatorPos, indicatorOn)`; removed AUTOPLAY,
    customizedLines, customizedComboIndicator, bossbarType.
  - `FeedbackManager` -> `(Player, GameOptions, GameInstance)`, always
    Hologram guidance with fixed "Combo {combo}", no scoreboard/bossbar.
  - `StatisticManager`: kept (combo/ACC records for HologramGuidance),
    removed `getGameData` + GameData usage.
  - `Main.java`: removed scoreboard map, onLogin/onQuit scoreboard wiring,
    defaultCustomizedLines; channel/listeners/arena init unchanged.
  - `GameManager.createPreviewGame` now takes `byte judgeMode`, no
    scoreboard lookup; `getGameData(Player)` removed.
  - `GameInstance`: stores `judgeMode`, `isAutoMode()`; FeedbackManager +
    StatisticManager created only in JUDGE mode; judge tick and feedback
    skipped in AUTO; `reloadPreview` can switch mode.
- **Mode byte contract**:
  - `ChartPreviewHandler.handlePreviewStart` reads trailing `byte mode`.
  - `PreviewGameRunner.startPreview(player, startBeat, mode)`; restart
    fallback defaults to JUDGE.
  - AUTO: `NoteObject` zNear branch judges the note (releases display) and
    `JudgementListeners` ignores clicks.
  - `NoteObject.judge()` always releases the display (removed AUTOPLAY skip).
- **Compile**: `mvn compile` green.

## Mod (D:/Dev/RhythMCChartMaker)

- `ChartPreviewChannel`: `PREVIEW_MODE_AUTO = 0`, `PREVIEW_MODE_JUDGE = 1`.
- `PreviewClient.sendPreviewStart(double startBeat, byte mode)` appends the
  mode byte; callers in `RmcChartClient`, `ChartEditorScreen`,
  `EditorPreviewBridge` updated.
- `ChartEditorState`: `previewMode` field + `previewMode()` getter +
  `togglePreviewMode()` (flips Auto/判定, updates status line).
- `EditorChrome.drawPreviewPanel`: fifth transport button "Auto"/"判定".
- `EditorDragHandler.handlePreviewClick`: click region for the toggle.
- **Compile**: `./gradlew compileClientJava` green.

## Contract Boundaries

- `rhythmc:chart_preview` opcode set unchanged; **`PREVIEW_START` (3) payload
  gains a trailing `byte mode`** (0=Auto, 1=Judge). `PREVIEW_RESTART` payload
  unchanged. Old mod + new plugin and new mod + old plugin are NOT
  interoperable for opcode 3; both repos updated in the same task.
- Chart JSON format untouched.

## Residual Risk / Known Limitations

- In-game validation still required: AUTO despawn at rail end, JUDGE feedback
  (particles/hologram/sound), arena restore on stop, mode toggle mid-preview
  (mode applies on next START; running instance keeps its mode).
- `FeedbackManager` survives as JUDGE-only; a future task may delete guidance
  code entirely if JUDGE mode is dropped.
- Mod UI text uses mixed "Auto"/"判定" labels; visual check in-game after
  screen changes (known rendering-order caveats).

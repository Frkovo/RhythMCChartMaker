# Repository Map

Updated: 2026-06-23

## Chart Maker Mod

- Path: `D:/Dev/RhythMCChartMaker`
- Build: `./gradlew compileClientJava`
- Runtime: Fabric client mod (Java 21, Minecraft 1.21)
- Responsibilities: chart editor UI, local audio playback, `rhythmc:chart_preview` client sender, chart project IO
- Key files:
  - `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/RmcChartClient.java` — client entrypoint
  - `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/editor/ChartEditorScreen.java` — editor screen
  - `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/editor/ChartEditorState.java` — editor state + undo/redo
  - `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/net/PreviewClient.java` — plugin channel client transport
  - `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/net/ChartPreviewChannel.java` — opcode constants
  - `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/project/ProjectHubScreen.java` — project hub
  - `src/client/java/cn/frkovo/rhythmcv2/rmcChart/client/wizard/NewSongWizardScreen.java` — new project wizard
  - `src/main/java/cn/frkovo/rhythmcv2/rmcChart/chart/` — model, io, core

## Preview Plugin

- Path: `D:/Dev/RhythMC-Preview`
- Build: `mvn compile`
- Responsibilities: chart visual playback, arena paste, file cache, `rhythmc:chart_preview` server receiver/sender

## Docs

- Changelog: `D:/Dev/RhythMCChartMaker/docs/CHANGELOG.md`
- Editor shortcuts: `D:/Dev/RhythMCChartMaker/docs/editor-shortcuts.md`

## Git Start Commands

Run in every affected repo before editing:

```text
git status --short
git log --oneline -10
```

Do not run destructive git commands unless the user explicitly requests them.

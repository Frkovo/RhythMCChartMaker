# Changelog

## 2026-06-28 : MOD : 时间轴 FX Track 重写与编辑分组

- 将 Effects 从时间轴顶部移到独立的底部 **FX Track**，永远存在，可展开/折叠。
- FX Track 展开时每个 Effect 独占一行，显示为可拖动的横向 Clip；带 `duration` 的 Timed Effect 显示起点/终点和主要信息标签。
- 提升时间轴网格粒度（最低 1/16 beat）并在标尺上标记 Beat 号。
- 在 FX Track 头部或 Note 轨道上右键新建对象时，统一插入到当前播放头红线位置。
- 新增编辑器独占的分组功能：选中多个 Effect 或 Note 后按 `Ctrl+G` 或点击 Edit > Group 命名成组，组内成员以彩色高亮显示。
- 新增 Effect 的复制/剪切/粘贴；选中 Effect 时 `Ctrl+C/V/X` 作用于 Effect，否则作用于 Note。
- 分组数据保存在 `EditorDraft` 中，不进入编译后的 Chart JSON 或 Preview 载荷。
- 已知限制：Note 分组以 `(trackId, beat, type)` 为键，移动/粘贴后不会自动跟随；分组目前仅在内存中，尚未持久化到项目文件。

## 2026-06-28 : MOD : 游戏内编辑快捷键与HUD

- 将 `F` 调整为打开/关闭 ChartMaker GUI 的切换键。
- 为编辑会话添加全局快捷键：`R` 开始播放、`T` 停止播放、`Shift+Left/Right` 前进或后退 10 秒。
- 增加常驻 HUD：右下角显示编辑快捷键，顶部接近 BossBar 位置显示当前 `BPM`、`beat`、`tick` 以及当前时长 / 总时长。
- 为顶部进度显示补充了可复用的编辑器状态格式化逻辑。
- 修正编辑器重开/seek 时误触发重复 Arena 生成的问题。

## 2026-06-28 : MOD : 编辑选择通道与游戏内右键拾取

- 新增 `rhythmc:chart_preview` opcodes 12/13 (`EDITOR_OPEN`/`EDITOR_SELECT`) 客户端侧支持。
- `PreviewClient` 新增 `EDITOR_OPEN`/`EDITOR_SELECT` 事件类型、`sendEditorOpen`/`sendEditorSelect` 方法，`PreviewEvent` 增加 `data` 字段。
- `RmcChartClient` 收到 `EDITOR_OPEN` 时自动打开编辑器 GUI，收到 `EDITOR_SELECT` 时解析 `track:<id>`/`note:<trackId>:<noteIndex>`/`effect:<index>`/`bpm:<index>` 并更新 `ChartEditorState` 选择，随后自动拉起 GUI。
- `ChartEditorState` 新增 `selectTrackById`/`selectNoteByTrackAndIndex`/`selectEffectByIndex`/`selectBpmByIndex`。
- `ChartPreviewChannel` 新增 `OP_EDITOR_OPEN`(12)/`OP_EDITOR_SELECT`(13) 常量。
- 更新 `docs/editor-shortcuts.md`。

## 2026-06-28 : MOD : 读谱面自动生成Arena、JSON编辑器与游戏内可视化

- 编辑器 `init()` 首次打开时自动上传谱面并 `PREVIEW_START`，实现"读谱面→生成Arena→进入编辑器"。
- 新增 `RawLevelJsonEditorScreen`：只读显示当前难度 level JSON，支持 `Ctrl+C` 复制到剪贴板、`Ctrl+V/Enter` 从剪贴板解析并应用、`Ctrl+R` 重新生成。
- `ChartProjectIo` 新增 `parseLevel(JsonObject, ChartDifficulty)` 公共方法。
- `ChartProject` 新增 `setLevel(ChartDifficulty, LevelData)`。
- `ChartEditorState` 新增 `applyLevelJson(JsonObject)`，应用后标记 dirty、重置 playhead、清除预览同步状态。
- 工具栏 File 菜单新增 `JSON` 按钮入口。
- 更新 `docs/editor-shortcuts.md` 和 `.agent/CONTRACTS.md`。

## 2026-06-27 : MOD : 修复标题按钮重叠与项目界面文字不显示

- 将标题屏幕 `RhythMC Chart Maker` 按钮下移到 Options/Quit Game 下方，避免与 `Minecraft Realms` 按钮重叠。
- 调整 `ProjectHubScreen` 与 `ProjectBrowserScreen` 的绘制顺序：先绘制面板与卡片背景，再调用 `super.render`，最后在所有 widget 之上绘制文字，解决项目卡片内文字不显示的问题。

## 2026-06-23 : MOD : 更改内容

- Removed the editor's central GUI stage demo and replaced it with a `Game Preview` control panel.
- Kept `Space` and `Edit > Play` as local editor audio playback.
- Changed `Preview > Play` / `Preview > Restart` and the center `Game Preview` Play button to start the real server-backed in-world preview and close the editor GUI.
- Added Esc return from in-world preview: it pauses playback, stops the server preview, and reopens the chart editor GUI.
- Added client-side autoplay hit sounds during in-world preview, matching `RhythMC-Reborn` defaults (bass drum for TAP/LOOK; pressure-plate clicks for HOLD start/end).
- Ensured HOLD notes default to `Z = -1.0` when their note type is set to HOLD.

## 2026-06-23 : MOD : 更改内容

- Added a title-screen `RhythMC Chart Maker` entry that connects directly to the local preview server at `localhost`.
- Kept the chart editor session resident across GUI close/reopen; reopening restores the same in-memory editor state instead of reloading from disk.
- Removed implicit autosave on editor close/navigation/preview start. Leaving the active editor session is blocked until the user explicitly saves.

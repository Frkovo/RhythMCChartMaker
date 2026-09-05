# Changelog

## 2026-09-05 : MOD : 标尺壳元数据发送 + SceneMap/世界壳 stub 删除

- `SongManifestData` 新增 `beatsPerBar`（默认 4，1..32 钳制）；`toOrderedMap` 末尾加 `editor:{beatsPerBar}` 块（manifest.yml 持久化 + 上传 JSON 自动携带）；`ChartProjectIo.loadManifest` 回读；`toManifestJson` 新增 `(data, activeTrackId)` 重载往 editor 块注入 activeTrackId（文件保存不用此重载）；`serializeCurrentChart` 改调重载（active 取 selectedTrack()，非 TRACK/NOTE 选择时回退首轨）。
- `MetaEditorScreen` Song Manifest 区 Version 后加 "Beats Per Bar" 字段（hint 1-32），populateFields 回填、apply() 经 parseInt + setter 写入。
- `ChartEditorState.setSelection` 加中央钩子：仅新 selection 为 TRACK/NOTE 且 track id 变化时发 `PreviewClient.sendEditorSelect('shell:track='+id)`（sendEditorSelect 内部 ensureReady 静默守卫，切轨即重染壳）。
- 删除：`EditorWorldLauncher.java`（stub）、`EditorSceneMap.java`、`SceneMapOverlayUi.java`；`DragMode` 删 MAP_NOTE/MAP_EFFECT/WORLD_SELECTION；`ChartEditorScreen` 删 MAP_OVERLAY 常量/sceneMap/sceneMapZoom/map 拖拽源/map zoom/close() 的 onEditorClosed；`EditorChrome` 左栏改直排 Tracks，删 drawCurrentFrameMap 系列 + drawPreviewDepthBand/drawPreviewNoteGlyph/drawSchematicEditorOverlay/drawLine 及无用 imports，statusHint 去 world 分支；`EditorDragHandler` 左栏点击/rowY/clear 去 map；`EditorMiscActions` 删 toggleWorldAutoPlay/syncWorldDisplays/pickWorldDisplay；`RmcChartClient` 删 launcher 字段/getter。world/ 包其余（Camera/Gizmo/Placement/WorldTransform）未动。
- 协议镜像（Preview 侧见同日条目）：`rhythmc:chart_preview` 无 opcode 变更；manifest JSON `editor` 块 + `EDITOR_SELECT` 的 `shell:track` 命名空间为新增约定；track JSON 现携带 `beatDivision`（Preview 缺省 16）。
- 编译：Preview `mvn -o compile` 通过；ChartMaker 侧本机 gradle daemon 固定 Java 17 跑不动 fabric-loom（需 21+，`-Dorg.gradle.java.home` 覆盖无效），未编译验证，需在配好 JDK 21+/25 的环境补跑 `compileClientJava`。

## 2026-09-04 : MOD : runClient 自动拉起 ProdTestServer

- `build.gradle` 新增 `rhythmc` 任务组：`startProdTestServer`（后台启动，端口被占用则跳过）、`runProdTestServer`（前台阻塞运行，方便调试）、`stopProdTestServer`（按 pid 文件停服）。
- `runClient` 新增 `dependsOn("startProdTestServer")`：`./gradlew runClient` 会先在后台拉起 `ProdTestServer/`（自动选 purpur 优先的 server jar，缺失时生成 `eula.txt` 与最小 `server.properties`，已有配置绝不覆盖），最多等待 90s 至 25565 端口就绪。
- 缺失时的默认配置为本地联调取值：`online-mode=false`（Loom 离线客户端 PlayerXXX 可进服）、`server-port=25565`；可用 `-PprodServerPort` / `-PprodServerXmx` 覆盖。
- 实测验证：后台启动约 15s 后端口就绪，RhythMC-Preview 正常启用（`rhythmc:chart_preview`），二次执行正确跳过，停服后端口关闭；`compileClientJava` 通过。

## 2026-08-20 : MOD : 世界放置、相机模式与变换 Gizmo（Phase A）

- A1 全局同步：隐藏编辑外壳时 `RmcChartClient.tickGlobalChartSync()` 以 LOAD_ONLY 语义自动上传脏谱面；新增 `globalSyncInFlight` 闸，避免 `markDirty()` 重置 `previewUploading` 造成拖拽期间的重叠上传（ACK/ERROR/断线时清除）。
- A2 世界放置：新增 `editor/world/EditorWorldPlacement`——`PREVIEW_READY` 时以玩家脚下 + (0, 0.5, -1.5) 捕获锚点，断线失效；准星射线与选中轨道平面求交（`crosshairToNotePos`），逆旋转回轨道空间并 0.25 网格吸附，最大距离 64；无命中回退默认位置，无锚点提示先运行一次 Test。
- A3 相机模式：新增 `editor/world/EditorCameraController`——`B` 键循环 Player→Free→Top→Front；通过 dummy ArmorStand + `setCameraEntity` 分离视角；Free 模式仅在壳打开且文本框未聚焦时用 WASD/Space/Shift 飞行；Top 俯视锚点（pitch +90），Front 位于平面前方 8 格回看；HUD 左上角显示相机标签；断线或 Player 模式自动恢复。
- A4 变换 Gizmo：新增 `EditorWorldGizmoRenderer`（Fabric `WorldRenderEvents.END_MAIN`，顶点按相机相对坐标发射，`RenderLayers.lines()` 绘制选中音符的 RGB 三轴十字）与 `EditorWorldTransform`（Transform 工具 + 选中音符时在世界区域左键拖拽，像素增量按音符深度换算世界位移，逆旋转除轨道缩放后 0.25 吸附；HOLD 保持 Z=-1）。
- 修复 Top 相机俯角符号（MC pitch +90 朝下）；`EditorWorldPlacement.rotateInverse` 改为 public 供拖拽复用；`ChartEditorScreen` 新增 public `state()` 访问器。
- 1.21.11 yarn 适配：`RenderLayer.getLines()` 已迁移为 `RenderLayers.lines()`；Fabric 世界渲染 API 顶点须相机相对。
- 编译验证通过：`compileClientJava`（JDK 25 override）。相机/拖拽/Gizmo 的实际手感尚未进游戏验证。

## 2026-08-17 : MOD : 恢复工具驱动的音符创建闭环

- 快捷栏 2–5 选择 Tap/Look/Hold/Dodge 放置模式后，左键点击时间轴 Notes lane 即可在该 beat 创建对应类型的音符。
- 隐藏编辑外壳（按 F）时，再次按下当前放置工具的数字键会在当前播放头和选中轨道上创建音符。
- 右键 Notes lane 仍可创建默认 TAP 音符。
- `EditorTool` 现在携带 `NoteType` 映射；`ChartEditorState` 新增 `addNoteOfType()` 作为跨上下文的统一创建方法。

## 2026-08-17 : MOD : 世界优先编辑外壳与原版快捷栏工具

- 编辑器改为非暂停的透明边缘外壳：Minecraft 世界直接作为中央预览画布，保留半透明顶栏、侧栏、紧凑传输条和底部 Track bar。
- 原版九格快捷栏成为主工具选择器：1 Select、2 Tap、3 Look、4 Hold、5 Dodge、6 Event、7 Transform、8 Test、9 Timeline；数字键不再直接创建 Note。
- 时间轴支持折叠与聚焦布局，折叠时保留 Track bar 和世界视野；响应式布局统一时间轴绘制、拖拽、框选和缩放命中坐标。
- Test/自动 LOAD 改为内联上传，不再在 `PREVIEW_READY` 后关闭编辑外壳；预览序列化不再隐式 Apply Inspector，避免未确认输入改写谱面。
- Test/Stop 使用全局请求所有权与停止屏障：本地音频等待 `PREVIEW_READY`，`PREVIEW_STOPPED` 隔离旧 generation 后才允许下一次 Start/Restart。
- Stop 屏障只在插件通道确认消息已发送后建立；本地通道/握手不可用不会再把编辑器永久锁在等待状态。
- 修复编辑器打开时状态重复 tick、重复处理 `F`、保存后会话失活、服务端选择重复聚焦以及 Inspector Transform 控件越界问题。
- 明确 `rhythmc:chart_preview` 编辑消息方向：C→S 使用 12/13，S→C 使用 107/108；服务端世界选择由全局处理器应用，不产生回声循环。
- 编译验证通过：`compileClientJava` 使用 JDK 25 daemon override 完成；Preview 插件 `mvn compile` 同步通过。

## 2026-08-14 : MOD : 完全重写编辑器顶栏

- 删除占整行的可编辑项目路径输入框：路径改为第一行只读暗色文本（自动截断），保存直接使用 `state.project().projectPath()`。
- 删除 File/Edit/Options/Preview 伪菜单 Tab（后三个 Tab 从来没有任何功能）及 `ToolbarMenu` / 两份重复的 `ToolbarActionButton` 死代码。
- 顶栏压缩为两行（86px → 52px）：
  - 第一行：标题 + 项目名 + 截断路径（左），Server / Save / Chart / Srv / Assets 状态 pill（右对齐）。
  - 第二行：New / Open / Save / Audio 动作按钮 + 难度分段选择器（左），当前 Beat / 轨道过滤 pill（右）。
- New 改走 NewSongWizard 向导，Open 走项目浏览器，不再从路径框解析裸路径。
- 难度按钮不再撑满整行，宽度自适应（46–72px），当前难度以青色高亮。
- 删除重复的 In/Out/Playing pill（信息已在预览面板与 Play/Stop 按钮中显示）。
- `./gradlew compileClientJava` 通过。


## 2026-08-01 : MOD : 插件瘦身 Preview-only MVP + PREVIEW_START 新增 mode 字节

- 插件（RhythMC-Preview）瘦身为 Preview-only MVP：删除 Economy、`Game/Scoreboard/`、`Game/Guidance/BossBar/`、ActionbarGuidance、SbCustomizedType/BossbarType/CiCustomizedType、GameData、统计上传与玩法配置项，仅保留谱面渲染、效果、Arena 粘贴与预览生命周期。
- 预览新增双模式，由 MOD 在 `PREVIEW_START` 尾部追加 `byte mode`（0=Auto，1=判定）发送，`PREVIEW_RESTART` 不重发 mode，实例保持原模式：
  - AUTO：Note 沿轨道飞行到轨尾自动消失，无玩家输入、无判定、无反馈，为默认模式。
  - 判定（JUDGE）：保留经典判定行为（JudgeManager、FeedbackManager 粒子/全息/音效反馈）。
- MOD 编辑器预览控制区新增 Auto/判定 切换按钮，状态存于 `ChartEditorState.previewMode()`。
- 插件 `GameOptions` 收敛为固定预览参数；`NoteObject.judge()` 移除 AUTOPLAY 跳过释放逻辑；`JudgementListeners` 在 AUTO 模式下忽略点击。
- 编译验证：插件 `mvn compile` 通过；MOD `./gradlew compileClientJava` 通过。


## 2026-06-30 : MOD : 音频波形、事件 Clip 缩放、Scene Map 网格与缩放

- 提高音频波形解析度：`AudioAnalysisService.WAVEFORM_BINS` 从 512 提升到 2048，时间轴音频条显示更细腻。
- 修复事件 Clip 边缘缩放手柄检测失效的问题：以边缘 8px/中心 12px 阈值分别命中，避免绝对距离与平方距离混用导致手柄识别错误。
- 修复时间轴右键添加 Note 总是使用红线（播放头）位置的问题：现在优先在鼠标点击位置添加，仅在点击位置非法时回退到播放头。
- Scene Map 增加网格与 Desmos 式缩放：
  - 根据可视范围自适应主/次网格线疏密，并在主网格线上绘制世界坐标标签。
  - 坐标轴改为加粗中心线，X/Y 方向均可见。
  - 在 Scene Map 区域滚动鼠标滚轮可缩放（0.25x - 8x），防止 zoom 过小导致网格消失。
  - 拖拽 Note / Effect 时自动吸附到最近的网格交点。
- 修复 Scene Map 底部 Z- / Z+ / Center 按钮与提示文字重叠的问题。
- `./gradlew compileClientJava` passes.

- 参考 MajDataEdit，在当前模块化编辑器中重新实现统一 Notes lane：
  - 每个展开的 Track 只保留一个 **Notes** lane，替代原来的 X/Y/Z 三条 lane。
  - Note 按 `(x, y)` 极角 `atan2(y, x) / π` 做 2D->1D 投影，垂直位置反映 Note 在场景平面上的方向。
- 拖动 Note 时水平改 beat，垂直改投影方向：按角度旋转 `(x, y)`，保持到中心距离不变；半径为 0 时位置不变。
- 更新 `EditorTimeline`、`EditorDragHandler`、`EditorUtils`、`NoteDragSnapshot`、`LaneType` 以支持投影绘制、命中检测、框选和拖动。
- Track 默认事件补齐：每个 Track 默认包含 Speed（8.0）、Position（0,0,0）、Rotation（0,0,0）、Scale（1,1,1）的单一 NumEvent。
- 在 `TrackData.createDefault` 中生成全部默认事件；新增 `TrackData.ensureDefaultEvents()`，在加载、复制、排序归一化时自动补齐缺失事件。
- Inspector 中 Clear/Reset Track 事件时重置为对应默认值，而不是清空事件列表。
- `./gradlew compileClientJava` passes.

## 2026-06-29 : MOD : 在重构拆分后的编辑器代码上修复交互问题并补充快捷键

- 修复时间轴框选释放后因 `applySelectionBox()` 处理异常导致的选择框残留问题。
- 修复 GUI 内 **Space** 播放/暂停（及 R 等注册快捷键）不响应的问题：改用 `KeyBinding.matchesKey(KeyInput)` + `consumedKeyCodes` 去重。
- 精简顶部 Preview 面板：移除 Server/Chart/Preview/Range/BPM 状态说明框，改为四个传输按钮 + 当前 beat/音频信息。
- 修复 `EditorChrome` 中 `drawLeftPanel` 和 `drawPropertyPanel` 方法误删导致的编译错误。
- 补充常用 GUI 快捷键：
  - **Ctrl+D**：复制当前选中的 Note / Effect。
  - **Backspace**：与 Delete 一样删除当前选中。
  - **Home/End**：播放头跳到谱面起点 / 终点。
  - **PageUp/PageDown**：在时间轴轨道列表中上下翻页。
  - **+/-**：缩放时间轴。
- 修复 Track / Note / Effect / BPM 删除后未生成历史快照的问题，现在 **Ctrl+Z** 可以回退删除操作。
- `./gradlew compileClientJava` passes。

## 2026-06-30 : MOD : 编辑器大文件拆分与工具类整合

- 将 `EditorPropertyPanel` 中的 Track Event 编辑器与 Easing Popup 逻辑提取到新的包私有类 `EditorTrackEventPanel`。
- 将 `EditorDragHandler` 中的 Scene Map 交互逻辑提取到新的 `EditorSceneMap`。
- 把重复的音符轴/轨道 Lane、格式化、序列化、深拷贝、层级排序等辅助方法统一迁移到 `EditorUtils`、`ChartProjectCopier`、`EditorLevelSorter`。
- 修复因拆分导致的 `NoteAxis`、`DragMode` 引用与 Group Name Popup 方法缺失等编译错误。
- 所有编辑器源文件当前均控制在 1000 行以内。
- 不改变 `rhythmc:chart_preview` 通道、Chart JSON 格式或任何 gameplay 行为。

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

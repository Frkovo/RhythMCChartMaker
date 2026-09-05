package cn.frkovo.rhythmcv2.rmcChart.client;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorScreen;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorState;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.EditorTool;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.world.EditorCameraController;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.world.EditorWorldGizmoRenderer;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.world.EditorWorldPlacement;
import cn.frkovo.rhythmcv2.rmcChart.client.net.PreviewClient;
import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectHubScreen;
import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectCreationService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;

/**
 * RhythMCChartMaker client entrypoint.
 *
 * <p>Rewritten: all in-world display/playback removed. The editor opens a screen UI,
 * and chart preview is delegated to a real RhythMC-Preview server over the
 * {@code rhythmc:chart_preview} plugin channel via {@link PreviewClient}.
 * The mod plays audio locally; the server renders visuals only.
 */
public class RmcChartClient implements ClientModInitializer {
    private static final String PREVIEW_SERVER_ADDRESS = "localhost";
    private static final ChartEditorState EDITOR_STATE = new ChartEditorState();
    private static final PreviewClient PREVIEW_CLIENT = new PreviewClient();
    private static final int HELLO_RETRY_INTERVAL_TICKS = 20;
    private static final int HELLO_RETRY_TIMEOUT_TICKS = 200;
    private static KeyBinding openEditorKey;
    private static KeyBinding playPreviewKey;
    private static KeyBinding cancelKey;
    private static KeyBinding deleteKey;
    private static KeyBinding toggleLocalPlaybackKey;
    private static KeyBinding seekBackwardKey;
    private static KeyBinding seekForwardKey;
    private static KeyBinding smoothingPopupKey;
    private static KeyBinding undoKey;
    private static KeyBinding redoKey;
    private static KeyBinding newProjectKey;
    private static KeyBinding openProjectKey;
    private static KeyBinding selectAllKey;
    private static KeyBinding copyKey;
    private static KeyBinding cutKey;
    private static KeyBinding pasteKey;
    private static KeyBinding saveKey;
    private static KeyBinding cameraModeKey;
    private static boolean editorHotkeyDown;
    private static int helloRetryTicks = -1;
    private static int nextHelloRetryTick = 0;
    private static boolean editorSessionActive = false;
    private static boolean editorSessionSaved = true;
    private static boolean worldPreviewActive = false;
    private static double pendingWorldTestBeat = Double.NaN;
    private static boolean previewStopPending = false;
    // True while a global (shell-hidden) chart upload is awaiting its ACK. markDirty()
    // clears previewUploading during drag bursts, so this gate prevents overlapping uploads.
    private static boolean globalSyncInFlight = false;
    private static final boolean[] toolKeyDown = new boolean[9];
    @Override
    public void onInitializeClient() {
        openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.open_editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                KeyBinding.Category.MISC
        ));
        playPreviewKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.play_preview",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyBinding.Category.MISC
        ));
        cancelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.cancel",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_ESCAPE,
                KeyBinding.Category.MISC
        ));
        deleteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.delete",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DELETE,
                KeyBinding.Category.MISC
        ));
        toggleLocalPlaybackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.toggle_local_playback",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_SPACE,
                KeyBinding.Category.MISC
        ));
        seekBackwardKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.seek_backward",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                KeyBinding.Category.MISC
        ));
        seekForwardKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.seek_forward",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                KeyBinding.Category.MISC
        ));
        smoothingPopupKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.smoothing_popup",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KeyBinding.Category.MISC
        ));
        undoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.undo",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                KeyBinding.Category.MISC
        ));
        redoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.redo",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                KeyBinding.Category.MISC
        ));
        newProjectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.new_project",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                KeyBinding.Category.MISC
        ));
        openProjectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.open_project",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KeyBinding.Category.MISC
        ));
        selectAllKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.select_all",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_A,
                KeyBinding.Category.MISC
        ));
        copyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.copy",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                KeyBinding.Category.MISC
        ));
        cutKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.cut",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                KeyBinding.Category.MISC
        ));
        pasteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.paste",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyBinding.Category.MISC
        ));
        saveKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.save",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_S,
                KeyBinding.Category.MISC
        ));
        cameraModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.camera_mode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KeyBinding.Category.MISC
        ));

        PREVIEW_CLIENT.register();
        PREVIEW_CLIENT.setGlobalEventHandler(RmcChartClient::handlePreviewEvent);
        WorldRenderEvents.END_MAIN.register(EditorWorldGizmoRenderer::endMain);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PREVIEW_CLIENT.resetHandshake("Waiting for RhythMC-Preview handshake");
            helloRetryTicks = 0;
            nextHelloRetryTick = 5;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            helloRetryTicks = -1;
            worldPreviewActive = false;
            pendingWorldTestBeat = Double.NaN;
            previewStopPending = false;
            globalSyncInFlight = false;
            EditorWorldPlacement.invalidateAnchor();
            EditorCameraController.detach(client);
            EDITOR_STATE.stopServerPreviewAudio("Disconnected from preview server");
            PREVIEW_CLIENT.resetHandshake("Disconnected");
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickPreviewHandshake(client);
            if (client != null) {
                if (worldPreviewActive || editorSessionActive) {
                    syncEditorToolFromHotbar(client, false);
                    EditorCameraController.tick(client);
                    if (!(client.currentScreen instanceof ChartEditorScreen)) {
                        EDITOR_STATE.tick();
                        tickGlobalChartSync();
                    }
                    handleGlobalEditorHotkeys(client);
                    handleGlobalToolHotkeys(client);
                }
                boolean hotkeyPressed = InputUtil.isKeyPressed(client.getWindow(), openEditorKey.getDefaultKey().getCode());
                if (hotkeyPressed && !editorHotkeyDown && client.player != null) {
                    toggleEditorScreen();
                }
                editorHotkeyDown = hotkeyPressed;
            }
        });
    }

    private static void handleGlobalToolHotkeys(MinecraftClient client) {
        if (client.getWindow() == null) {
            return;
        }
        boolean canActivate = client.currentScreen == null;
        for (int slot = 0; slot < toolKeyDown.length; slot++) {
            boolean pressed = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_1 + slot);
            if (canActivate && pressed && !toolKeyDown[slot]) {
                EditorTool previousTool = EDITOR_STATE.activeTool();
                selectEditorTool(slot, true);
                EditorTool tool = EditorTool.fromHotbarSlot(slot);
                if (tool == EditorTool.TEST) {
                    toggleTestFromWorld();
                } else if (tool == EditorTool.TIMELINE) {
                    openActiveEditor();
                } else if (tool.isNotePlacement() && tool == previousTool) {
                    placeNoteFromWorld(tool);
                }
            }
            toolKeyDown[slot] = pressed;
        }
    }

    private static void placeNoteFromWorld(EditorTool tool) {
        TrackData track = EDITOR_STATE.selectedTrack();
        double beat = EDITOR_STATE.playheadBeat();
        MinecraftClient client = MinecraftClient.getInstance();
        double[] pos = EditorWorldPlacement.crosshairToNotePos(client, track, beat);
        if (pos != null) {
            EDITOR_STATE.addNoteOfTypeAt(track, beat, tool.noteType(), pos[0], pos[1]);
            EDITOR_STATE.setStatus("Placed " + tool.label() + " at (" + pos[0] + ", " + pos[1] + ") beat " + beat);
        } else if (EditorWorldPlacement.hasAnchor()) {
            EDITOR_STATE.addNoteOfType(track, beat, tool.noteType());
        } else {
            EDITOR_STATE.setStatus("No preview anchor yet; start Test once before world placement");
        }
    }

    /**
     * Uploads dirty chart edits while the editor shell is hidden (world view).
     * LOAD_ONLY semantics: no START, no pending beat. The ACK is consumed by the
     * global handler when no ChartEditorScreen is open.
     */
    private static void tickGlobalChartSync() {
        if (!EDITOR_STATE.previewChartDirty()
                || EDITOR_STATE.previewUploading()
                || globalSyncInFlight
                || previewStopPending
                || Double.isFinite(pendingWorldTestBeat)
                || !PREVIEW_CLIENT.isReady()) {
            return;
        }
        try {
            ChartEditorState.SerializedPreviewChart payload = EDITOR_STATE.serializeCurrentChart();
            EDITOR_STATE.markPreviewUploadStarted();
            PREVIEW_CLIENT.sendChartLoad(payload.manifestJson(), payload.levelJson());
            globalSyncInFlight = true;
            EDITOR_STATE.setStatus("Syncing chart to preview server");
        } catch (RuntimeException exception) {
            EDITOR_STATE.markPreviewUploadFailed();
            EDITOR_STATE.setStatus("Chart sync failed: " + exception.getMessage());
        }
    }

    private static void toggleTestFromWorld() {
        if (!PREVIEW_CLIENT.isReady()) {
            EDITOR_STATE.setStatus("Preview server is not ready");
            return;
        }
        if (previewStopPending) {
            EDITOR_STATE.setStatus("Waiting for preview server to stop");
            return;
        }
        if (EDITOR_STATE.serverPreviewRunning() || Double.isFinite(pendingWorldTestBeat)) {
            pendingWorldTestBeat = Double.NaN;
            if (requestPreviewStop()) {
                EDITOR_STATE.stopServerPreviewAudio("Test stopped");
            } else {
                EDITOR_STATE.stopServerPreviewAudio("Preview stop could not be sent");
            }
            return;
        }
        if (EDITOR_STATE.previewChartDirty() || !EDITOR_STATE.previewChartUploaded()) {
            EDITOR_STATE.setStatus("Open the editor and press 8 to upload the current chart before testing");
            return;
        }
        pendingWorldTestBeat = EDITOR_STATE.playheadBeat();
        PREVIEW_CLIENT.sendPreviewStart(pendingWorldTestBeat, EDITOR_STATE.previewMode());
        EDITOR_STATE.setStatus("Test requested; waiting for preview server");
    }

    private static void handleGlobalEditorHotkeys(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        if (client.currentScreen != null) {
            return;
        }
        while (playPreviewKey.wasPressed()) {
            toggleTestFromWorld();
        }
        while (seekBackwardKey.wasPressed()) {
            EDITOR_STATE.seekByMillis(-10_000L);
            if (canRestartPreviewFromWorld()) {
                PREVIEW_CLIENT.sendPreviewRestart(EDITOR_STATE.playheadBeat());
            }
        }
        while (seekForwardKey.wasPressed()) {
            EDITOR_STATE.seekByMillis(10_000L);
            if (canRestartPreviewFromWorld()) {
                PREVIEW_CLIENT.sendPreviewRestart(EDITOR_STATE.playheadBeat());
            }
        }
        while (cameraModeKey.wasPressed()) {
            EditorCameraController.cycleMode(client);
            EDITOR_STATE.setStatus("Camera: " + EditorCameraController.mode().label());
        }
    }

    private static boolean canRestartPreviewFromWorld() {
        return PREVIEW_CLIENT.isReady()
                && !previewStopPending
                && !Double.isFinite(pendingWorldTestBeat)
                && EDITOR_STATE.serverPreviewRunning();
    }

    private static void toggleEditorScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        if (client.currentScreen instanceof ChartEditorScreen) {
            ((ChartEditorScreen) client.currentScreen).hideShellForWorldView();
            return;
        }
        if (editorSessionActive) {
            openActiveEditor();
        } else {
            openEditor(null);
        }
    }

    private static void tickPreviewHandshake(MinecraftClient client) {
        if (helloRetryTicks < 0) {
            return;
        }
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        if (PREVIEW_CLIENT.isReady()) {
            helloRetryTicks = -1;
            return;
        }

        helloRetryTicks++;
        if (PREVIEW_CLIENT.canSend() && helloRetryTicks >= nextHelloRetryTick) {
            PREVIEW_CLIENT.sendHello("RhythMCChartMaker");
            nextHelloRetryTick = helloRetryTicks + HELLO_RETRY_INTERVAL_TICKS;
        }

        if (helloRetryTicks >= HELLO_RETRY_TIMEOUT_TICKS) {
            helloRetryTicks = -1;
            String reason = PREVIEW_CLIENT.canSend()
                    ? "RhythMCChartMaker: preview plugin did not answer handshake. Check the server console/plugin version."
                    : "RhythMCChartMaker: preview plugin channel is not available on this server.";
            sendPlayerMessage(reason, false);
        }
    }

    private static void openEditor(Path path) {
        MinecraftClient client = MinecraftClient.getInstance();
        String blockReason = editorBlockReason(client);
        if (blockReason != null) {
            sendPlayerMessage(blockReason, false);
            return;
        }
        if (path != null) {
            beginEditorSession(false);
            EDITOR_STATE.loadProject(path);
            client.setScreen(new ChartEditorScreen(EDITOR_STATE));
            return;
        }
        if (editorSessionActive) {
            client.setScreen(new ChartEditorScreen(EDITOR_STATE));
            return;
        }
        client.setScreen(new ProjectHubScreen(client.currentScreen));
    }

    public static boolean canOpenEditor(MinecraftClient client) {
        return client != null
                && client.player != null
                && client.world != null
                && !client.isInSingleplayer()
                && PREVIEW_CLIENT.isReady();
    }

    public static ChartEditorState getEditorState() {
        return EDITOR_STATE;
    }

    public static PreviewClient getPreviewClient() {
        return PREVIEW_CLIENT;
    }

    public static KeyBinding openEditorKeyBinding() { return openEditorKey; }
    public static KeyBinding playPreviewKeyBinding() { return playPreviewKey; }
    public static KeyBinding cancelKeyBinding() { return cancelKey; }
    public static KeyBinding deleteKeyBinding() { return deleteKey; }
    public static KeyBinding toggleLocalPlaybackKeyBinding() { return toggleLocalPlaybackKey; }
    public static KeyBinding seekBackwardKeyBinding() { return seekBackwardKey; }
    public static KeyBinding seekForwardKeyBinding() { return seekForwardKey; }
    public static KeyBinding smoothingPopupKeyBinding() { return smoothingPopupKey; }
    public static KeyBinding undoKeyBinding() { return undoKey; }
    public static KeyBinding redoKeyBinding() { return redoKey; }
    public static KeyBinding newProjectKeyBinding() { return newProjectKey; }
    public static KeyBinding openProjectKeyBinding() { return openProjectKey; }
    public static KeyBinding selectAllKeyBinding() { return selectAllKey; }
    public static KeyBinding copyKeyBinding() { return copyKey; }
    public static KeyBinding cutKeyBinding() { return cutKey; }
    public static KeyBinding pasteKeyBinding() { return pasteKey; }
    public static KeyBinding saveKeyBinding() { return saveKey; }

    public static void selectEditorTool(int hotbarSlot, boolean announce) {
        MinecraftClient client = MinecraftClient.getInstance();
        int slot = Math.max(0, Math.min(8, hotbarSlot));
        if (client != null && client.player != null) {
            client.player.getInventory().setSelectedSlot(slot);
        }
        EDITOR_STATE.setActiveTool(EditorTool.fromHotbarSlot(slot), announce);
    }

    public static void syncEditorToolFromHotbar(MinecraftClient client, boolean announce) {
        if (client == null || client.player == null) {
            return;
        }
        EDITOR_STATE.setActiveTool(EditorTool.fromHotbarSlot(client.player.getInventory().getSelectedSlot()), announce);
    }

    public static boolean hasPendingWorldTest() {
        return Double.isFinite(pendingWorldTestBeat);
    }

    public static void clearPendingWorldTest() {
        pendingWorldTestBeat = Double.NaN;
    }

    public static boolean isPreviewStopPending() {
        return previewStopPending;
    }

    public static void beginPreviewStop() {
        pendingWorldTestBeat = Double.NaN;
        previewStopPending = true;
    }

    public static void clearPreviewStopPending() {
        previewStopPending = false;
    }

    public static boolean requestPreviewStop() {
        if (previewStopPending) return true;
        if (!PREVIEW_CLIENT.sendPreviewStop()) return false;
        beginPreviewStop();
        return true;
    }

    public static boolean consumeSinglePress(KeyBinding binding) {
        return binding != null && binding.wasPressed();
    }

    public static void connectToPreviewServer(Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        ServerAddress address = ServerAddress.parse(PREVIEW_SERVER_ADDRESS);
        ServerInfo serverInfo = new ServerInfo("RhythMC Preview", PREVIEW_SERVER_ADDRESS, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(parent == null ? new MultiplayerScreen(null) : parent, client, address, serverInfo, false, null);
    }

    public static void openActiveEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        String blockReason = editorBlockReason(client);
        if (blockReason != null) {
            sendPlayerMessage(blockReason, false);
            return;
        }
        beginEditorSession(editorSessionSaved);
        worldPreviewActive = false;
        client.setScreen(new ChartEditorScreen(EDITOR_STATE));
    }

    public static void enterWorldPreviewFromEditor() {
        worldPreviewActive = true;
    }

    public static boolean shouldReturnToEditorFromWorldPreview() {
        return worldPreviewActive && editorSessionActive;
    }

    public static void returnToEditorFromWorldPreview() {
        worldPreviewActive = false;
        requestPreviewStop();
        EDITOR_STATE.stopServerPreviewAudio("Preview paused");
        openActiveEditor();
    }

    public static void beginEditorSession(boolean saved) {
        editorSessionActive = true;
        editorSessionSaved = saved;
    }

    public static void markEditorSessionDirty() {
        editorSessionActive = true;
        editorSessionSaved = false;
    }

    public static void markEditorSessionSaved() {
        editorSessionActive = true;
        editorSessionSaved = true;
    }

    public static boolean isEditorSessionActive() {
        return editorSessionActive;
    }

    public static boolean isEditorSessionSaved() {
        return editorSessionSaved;
    }

    public static void openExistingProject(Path projectPath) {
        openEditor(projectPath);
    }

    public static void launchNewSongEditor() {
        openEditor(null);
    }

    public static void launchNewSongEditor(cn.frkovo.rhythmcv2.rmcChart.client.wizard.NewSongWizardData data) {
        MinecraftClient client = MinecraftClient.getInstance();
        String blockReason = editorBlockReason(client);
        if (blockReason != null) {
            sendPlayerMessage(blockReason, false);
            return;
        }
        try {
            Path projectPath = ProjectCreationService.createProject(data);
            beginEditorSession(false);
            openEditor(projectPath);
        } catch (IOException exception) {
            sendPlayerMessage("RhythMCChartMaker: project creation failed: " + exception.getMessage(), false);
        }
    }

    private static String editorBlockReason(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return "RhythMCChartMaker: enter a RhythMC-Preview server first, then press F to open the editor.";
        }
        if (client.isInSingleplayer()) {
            return "RhythMCChartMaker: chart preview only works on a RhythMC-Preview multiplayer server.";
        }
        if (!PREVIEW_CLIENT.canSend()) {
            return "RhythMCChartMaker: this server does not expose the RhythMC-Preview plugin channel yet.";
        }
        if (!PREVIEW_CLIENT.isReady()) {
            return "RhythMCChartMaker: waiting for preview plugin handshake. " + PREVIEW_CLIENT.handshakeMessage();
        }
        return null;
    }

    private static void handlePreviewEvent(PreviewClient.PreviewEvent event) {
        switch (event.type()) {
            case HELLO_ACK -> {
                helloRetryTicks = -1;
                if (event.ok()) {
                    sendPlayerMessage("RhythMCChartMaker: preview plugin connected. Press F to start making a chart.", true);
                } else {
                    sendPlayerMessage("RhythMCChartMaker: preview plugin handshake failed: " + event.error(), false);
                }
            }
            case EDITOR_OPEN -> focusEditorFromServer();
            case EDITOR_SELECT -> {
                applyEditorSelection(event.data());
                focusEditorFromServer();
            }
            case PREVIEW_READY -> {
                EditorWorldPlacement.captureAnchorFromPlayer(MinecraftClient.getInstance());
                if (!previewStopPending && Double.isFinite(pendingWorldTestBeat)) {
                    double beat = pendingWorldTestBeat;
                    pendingWorldTestBeat = Double.NaN;
                    EDITOR_STATE.startServerPreviewAudio(beat);
                }
            }
            case PREVIEW_STOPPED -> {
                previewStopPending = false;
                pendingWorldTestBeat = Double.NaN;
                if (!(MinecraftClient.getInstance().currentScreen instanceof ChartEditorScreen)) {
                    EDITOR_STATE.stopServerPreviewAudio("Server preview stopped");
                }
            }
            case CHART_LOAD_ACK -> {
                // Global sync (shell hidden) owns the ACK; when the shell is open the
                // per-screen EditorPreviewBridge handles it instead. The in-flight flag
                // is cleared regardless of which side consumes the ACK.
                globalSyncInFlight = false;
                if (!(MinecraftClient.getInstance().currentScreen instanceof ChartEditorScreen)
                        && !previewStopPending) {
                    if (event.ok()) {
                        EDITOR_STATE.markPreviewChartUploaded();
                        EDITOR_STATE.setStatus("Chart synced to preview server");
                    } else {
                        EDITOR_STATE.markPreviewUploadFailed();
                        EDITOR_STATE.setStatus("Chart sync rejected: " + event.error());
                    }
                }
            }
            case ERROR -> {
                globalSyncInFlight = false;
                if (Double.isFinite(pendingWorldTestBeat)
                        || (!(MinecraftClient.getInstance().currentScreen instanceof ChartEditorScreen)
                        && EDITOR_STATE.serverPreviewRunning())) {
                    pendingWorldTestBeat = Double.NaN;
                    EDITOR_STATE.stopServerPreviewAudio("Server preview error: " + event.error());
                }
                sendPlayerMessage("RhythMCChartMaker: " + event.error(), false);
            }
            default -> {
            }
        }
    }

    private static void focusEditorFromServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (editorBlockReason(client) != null) {
            return;
        }
        beginEditorSession(editorSessionSaved);
        if (!(client.currentScreen instanceof ChartEditorScreen)) {
            client.setScreen(new ChartEditorScreen(EDITOR_STATE));
        }
    }

    private static void applyEditorSelection(String data) {
        if (data == null || data.isBlank()) {
            return;
        }
        String[] parts = data.split(":");
        boolean applied = switch (parts[0]) {
            case "track" -> parts.length >= 2 && trySelectTrack(parts[1]);
            case "note" -> parts.length >= 3 && trySelectNote(parts[1], parts[2]);
            case "effect" -> parts.length >= 2 && trySelectEffect(parts[1]);
            case "bpm" -> parts.length >= 2 && trySelectBpm(parts[1]);
            default -> false;
        };
        if (!applied) {
            sendPlayerMessage("RhythMCChartMaker: selection updated: " + data, true);
        }
    }

    private static boolean trySelectTrack(String trackIdText) {
        try {
            int trackId = Integer.parseInt(trackIdText);
            return EDITOR_STATE.selectTrackById(trackId);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean trySelectNote(String trackIdText, String noteIndexText) {
        try {
            int trackId = Integer.parseInt(trackIdText);
            int noteIndex = Integer.parseInt(noteIndexText);
            return EDITOR_STATE.selectNoteByTrackAndIndex(trackId, noteIndex);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean trySelectEffect(String effectIndexText) {
        try {
            return EDITOR_STATE.selectEffectByIndex(Integer.parseInt(effectIndexText));
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean trySelectBpm(String bpmIndexText) {
        try {
            return EDITOR_STATE.selectBpmByIndex(Integer.parseInt(bpmIndexText));
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static void sendPlayerMessage(String message, boolean actionBar) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(message), actionBar);
        }
    }
}

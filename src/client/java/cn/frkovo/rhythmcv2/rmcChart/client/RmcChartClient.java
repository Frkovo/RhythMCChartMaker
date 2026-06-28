package cn.frkovo.rhythmcv2.rmcChart.client;

import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorScreen;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorState;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.world.EditorWorldLauncher;
import cn.frkovo.rhythmcv2.rmcChart.client.net.PreviewClient;
import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectHubScreen;
import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectCreationService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
    private static final EditorWorldLauncher WORLD_LAUNCHER = new EditorWorldLauncher(null);
    private static final int HELLO_RETRY_INTERVAL_TICKS = 20;
    private static final int HELLO_RETRY_TIMEOUT_TICKS = 200;
    private static KeyBinding openEditorKey;
    private static KeyBinding playPreviewKey;
    private static KeyBinding cancelKey;
    private static KeyBinding deleteKey;
    private static KeyBinding toggleLocalPlaybackKey;
    private static KeyBinding seekBackwardKey;
    private static KeyBinding seekForwardKey;
    private static KeyBinding noteTapKey;
    private static KeyBinding noteLookKey;
    private static KeyBinding noteHoldKey;
    private static KeyBinding noteDodgeKey;
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
    private static boolean editorHotkeyDown;
    private static int helloRetryTicks = -1;
    private static int nextHelloRetryTick = 0;
    private static boolean editorSessionActive = false;
    private static boolean editorSessionSaved = true;
    private static boolean worldPreviewActive = false;
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
        noteTapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.note_tap",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_1,
                KeyBinding.Category.MISC
        ));
        noteLookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.note_look",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_2,
                KeyBinding.Category.MISC
        ));
        noteHoldKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.note_hold",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_3,
                KeyBinding.Category.MISC
        ));
        noteDodgeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.note_dodge",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_4,
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

        PREVIEW_CLIENT.register();
        PREVIEW_CLIENT.setGlobalEventHandler(RmcChartClient::handlePreviewEvent);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PREVIEW_CLIENT.resetHandshake("Waiting for RhythMC-Preview handshake");
            helloRetryTicks = 0;
            nextHelloRetryTick = 5;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            helloRetryTicks = -1;
            worldPreviewActive = false;
            PREVIEW_CLIENT.resetHandshake("Disconnected");
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickPreviewHandshake(client);
            if (client != null) {
                if (worldPreviewActive || editorSessionActive) {
                    EDITOR_STATE.tick();
                    handleGlobalEditorHotkeys(client);
                }
                boolean hotkeyPressed = InputUtil.isKeyPressed(client.getWindow(), openEditorKey.getDefaultKey().getCode());
                if (hotkeyPressed && !editorHotkeyDown && client.player != null) {
                    toggleEditorScreen();
                }
                editorHotkeyDown = hotkeyPressed;
            }
        });
    }

    private static void handleGlobalEditorHotkeys(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        if (client.currentScreen != null) {
            return;
        }
        while (playPreviewKey.wasPressed()) {
            if (EDITOR_STATE.playing()) {
                EDITOR_STATE.stopPlayback();
                if (PREVIEW_CLIENT.isReady()) {
                    PREVIEW_CLIENT.sendPreviewStop();
                }
            } else {
                EDITOR_STATE.startPlaybackAt(EDITOR_STATE.playheadBeat(), "Playback started");
                if (PREVIEW_CLIENT.isReady()) {
                    PREVIEW_CLIENT.sendPreviewStart(EDITOR_STATE.playheadBeat());
                }
            }
        }
        while (seekBackwardKey.wasPressed()) {
            EDITOR_STATE.seekByMillis(-10_000L);
            if (PREVIEW_CLIENT.isReady()) {
                PREVIEW_CLIENT.sendPreviewRestart(EDITOR_STATE.playheadBeat());
            }
        }
        while (seekForwardKey.wasPressed()) {
            EDITOR_STATE.seekByMillis(10_000L);
            if (PREVIEW_CLIENT.isReady()) {
                PREVIEW_CLIENT.sendPreviewRestart(EDITOR_STATE.playheadBeat());
            }
        }
    }

    private static void toggleEditorScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        if (client.currentScreen instanceof ChartEditorScreen) {
            client.setScreen(null);
            return;
        }
        openEditor(null);
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
    public static KeyBinding noteTapKeyBinding() { return noteTapKey; }
    public static KeyBinding noteLookKeyBinding() { return noteLookKey; }
    public static KeyBinding noteHoldKeyBinding() { return noteHoldKey; }
    public static KeyBinding noteDodgeKeyBinding() { return noteDodgeKey; }
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
        if (PREVIEW_CLIENT.isReady()) {
            PREVIEW_CLIENT.sendPreviewStop();
        }
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
        editorSessionActive = false;
        editorSessionSaved = true;
        worldPreviewActive = false;
    }

    public static boolean isEditorSessionActive() {
        return editorSessionActive;
    }

    public static boolean isEditorSessionSaved() {
        return editorSessionSaved;
    }

    /** @deprecated In-world launcher stub retained during the reborn rewrite. All methods are no-ops. */
    @Deprecated
    public static EditorWorldLauncher getWorldLauncher() {
        return WORLD_LAUNCHER;
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
            case EDITOR_OPEN -> openActiveEditor();
            case EDITOR_SELECT -> applyEditorSelection(event.data());
            case ERROR -> sendPlayerMessage("RhythMCChartMaker: " + event.error(), false);
            default -> {
            }
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && !(client.currentScreen instanceof ChartEditorScreen)) {
            openActiveEditor();
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

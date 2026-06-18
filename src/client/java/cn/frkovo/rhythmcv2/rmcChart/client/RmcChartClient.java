package cn.frkovo.rhythmcv2.rmcChart.client;

import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorScreen;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorState;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.world.EditorWorldLauncher;
import cn.frkovo.rhythmcv2.rmcChart.client.net.PreviewClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

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
    private static final ChartEditorState EDITOR_STATE = new ChartEditorState();
    private static final PreviewClient PREVIEW_CLIENT = new PreviewClient();
    private static final EditorWorldLauncher WORLD_LAUNCHER = new EditorWorldLauncher(null);
    private static KeyBinding openEditorKey;
    private static boolean editorHotkeyDown;

    @Override
    public void onInitializeClient() {
        openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rmc-chart.open_editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                KeyBinding.Category.MISC
        ));

        PREVIEW_CLIENT.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean hotkeyPressed = InputUtil.isKeyPressed(client.getWindow(), openEditorKey.getDefaultKey().getCode());
            if (hotkeyPressed && !editorHotkeyDown && client.player != null) {
                openEditor(null);
            }
            editorHotkeyDown = hotkeyPressed;
        });
    }

    private static void openEditor(Path path) {
        if (path != null) {
            EDITOR_STATE.loadProject(path);
        }
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ChartEditorScreen(EDITOR_STATE));
    }

    public static ChartEditorState getEditorState() {
        return EDITOR_STATE;
    }

    public static PreviewClient getPreviewClient() {
        return PREVIEW_CLIENT;
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
        openEditor(null);
    }
}

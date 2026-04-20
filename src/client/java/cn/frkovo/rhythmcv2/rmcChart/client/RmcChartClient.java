package cn.frkovo.rhythmcv2.rmcChart.client;

import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorScreen;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorState;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.world.EditorWorldLauncher;
import cn.frkovo.rhythmcv2.rmcChart.client.wizard.NewSongWizardData;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

public class RmcChartClient implements ClientModInitializer {
    private static final ChartEditorState EDITOR_STATE = new ChartEditorState();
    private static final EditorWorldLauncher WORLD_LAUNCHER = new EditorWorldLauncher(EDITOR_STATE);
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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WORLD_LAUNCHER.tick(client);
            boolean hotkeyPressed = InputUtil.isKeyPressed(client.getWindow(), openEditorKey.getDefaultKey().getCode());
            if (hotkeyPressed && !editorHotkeyDown) {
                if (WORLD_LAUNCHER.isEditorWorldActive(client)) {
                    WORLD_LAUNCHER.toggleOverlay();
                } else {
                    openEditor(null);
                }
            }
            editorHotkeyDown = hotkeyPressed;
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("rmcchart")
                        .executes(context -> {
                            openEditor(null);
                            context.getSource().sendFeedback(Text.literal("Opened RhythMC Chart Maker"));
                            return 1;
                        })
                        .then(ClientCommandManager.argument("path", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String rawPath = StringArgumentType.getString(context, "path");
                                    openEditor(Path.of(rawPath));
                                    context.getSource().sendFeedback(Text.literal("Opened RhythMC Chart Maker: " + rawPath));
                                    return 1;
                                })))
        );
    }

    private static void openEditor(Path path) {
        if (path != null) {
            EDITOR_STATE.loadProject(path);
        }
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ChartEditorScreen(EDITOR_STATE));
    }

    public static void launchNewSongEditor() {
        WORLD_LAUNCHER.launchNewSongWorld(MinecraftClient.getInstance());
    }

    public static void launchNewSongEditor(NewSongWizardData data) {
        WORLD_LAUNCHER.launchNewSongWorld(MinecraftClient.getInstance(), data);
    }

    public static void openExistingProject(Path projectPath) {
        WORLD_LAUNCHER.openProjectWorld(MinecraftClient.getInstance(), projectPath);
    }

    public static ChartEditorState getEditorState() {
        return EDITOR_STATE;
    }

    public static EditorWorldLauncher getWorldLauncher() {
        return WORLD_LAUNCHER;
    }
}

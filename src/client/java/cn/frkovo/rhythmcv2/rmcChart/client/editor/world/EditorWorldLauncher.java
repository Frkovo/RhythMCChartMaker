package cn.frkovo.rhythmcv2.rmcChart.client.editor.world;

import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorScreen;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorState;
import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectStorage;
import cn.frkovo.rhythmcv2.rmcChart.client.wizard.ImportedSongMetadata;
import cn.frkovo.rhythmcv2.rmcChart.client.wizard.NewSongWizardData;
import cn.frkovo.rhythmcv2.rmcChart.client.wizard.SongImportHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.rule.GameRules;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class EditorWorldLauncher {
    private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private final ChartEditorState editorState;
    private final EditorWorldController worldController;

    private boolean launchPending;
    private boolean scaffoldPrepared;
    private Path pendingProjectPath;
    private String pendingWorldId;
    private boolean editorVisible;

    public EditorWorldLauncher(ChartEditorState editorState) {
        this.editorState = editorState;
        this.worldController = new EditorWorldController(editorState);
    }

    public void launchNewSongWorld(MinecraftClient client) {
        launchNewSongWorld(client, new NewSongWizardData(
                "Untitled " + ID_FORMAT.format(LocalDateTime.now()),
                "Unknown Composer",
                "song-" + ID_FORMAT.format(LocalDateTime.now()),
                "RhythMC chart project created from main menu",
                null
        ));
    }

    public void launchNewSongWorld(MinecraftClient client, NewSongWizardData data) {
        String timestamp = ID_FORMAT.format(LocalDateTime.now());
        String projectId = data.folderName();
        Path projectPath = ProjectStorage.projectPath(projectId);

        editorState.newProject(projectPath);
        editorState.project().manifest().setName(data.title());
        editorState.project().manifest().setComposer(data.composer());
        editorState.project().manifest().setAlias(projectId);
        editorState.project().manifest().setDescription(data.description());
        editorState.project().manifest().setSongId(Math.abs(projectId.hashCode()));
        editorState.project().manifest().comments().add("Created in editor world");
        editorState.level().meta().charters().add(data.composer());

        if (data.songPath() != null) {
            ImportedSongMetadata metadata = SongImportHelper.readMetadata(data.songPath());
            if (!metadata.description().isBlank() && editorState.project().manifest().description().isBlank()) {
                editorState.project().manifest().setDescription(metadata.description());
            }
            if (metadata.lengthMillis() > 0) {
                editorState.project().manifest().setLength(metadata.lengthMillis());
            }
            try {
                SongImportHelper.copySongToProject(data.songPath(), projectPath);
            } catch (Exception exception) {
                editorState.setStatus("Failed to copy song file: " + exception.getMessage());
            }
        }

        editorState.reloadAudio();
        editorState.applyEstimatedBpmReferenceIfDefault();
        editorState.saveProject(projectPath);
        ProjectStorage.recordRecentProject(projectPath);

        LevelInfo levelInfo = new LevelInfo(
                "RhythMC Chart Editor",
                GameMode.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                new GameRules(DataConfiguration.SAFE_MODE.enabledFeatures()),
                DataConfiguration.SAFE_MODE
        );

        this.pendingProjectPath = projectPath;
        this.pendingWorldId = "rmc_chart_editor_" + timestamp;
        this.launchPending = true;
        this.scaffoldPrepared = false;
        this.editorVisible = false;

        client.createIntegratedServerLoader().createAndStart(
                pendingWorldId,
                levelInfo,
                new GeneratorOptions(0L, false, false),
                WorldPresets::createTestOptions,
                client.currentScreen
        );
    }

    public void tick(MinecraftClient client) {
        worldController.tick(client);

        if (client.world == null || client.player == null || !client.isIntegratedServerRunning()) {
            if (!launchPending) {
                pendingProjectPath = null;
                scaffoldPrepared = false;
                editorVisible = false;
            }
            return;
        }

        if (launchPending) {
            if (!scaffoldPrepared) {
                prepareEditorWorld(client);
                scaffoldPrepared = true;
            }

            if (client.currentScreen == null) {
                showEditor(client);
                launchPending = false;
            }
        }
    }

    public void openDetailedEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        showEditor(client);
    }

    public void toggleOverlay() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isEditorWorldActive(client)) {
            return;
        }
        if (editorVisible && client.currentScreen instanceof ChartEditorScreen) {
            hideEditor(client);
        } else {
            showEditor(client);
        }
    }

    public void leaveEditorWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        editorVisible = false;
        client.disconnect(new TitleScreen(), false);
    }

    public void onEditorClosed() {
        editorVisible = false;
    }

    public void syncDisplays() {
        worldController.syncNow(MinecraftClient.getInstance());
    }

    public boolean pickLookTarget() {
        return worldController.pickLookTarget(MinecraftClient.getInstance());
    }

    public boolean dragSelection(double deltaX, double deltaY, int button, boolean shiftDown) {
        return worldController.dragSelection(MinecraftClient.getInstance(), deltaX, deltaY, button, shiftDown);
    }

    public boolean isEditorWorldActive(MinecraftClient client) {
        return client.world != null && client.player != null && client.isIntegratedServerRunning() && pendingProjectPath != null;
    }

    public Path currentProjectPath() {
        return pendingProjectPath;
    }

    public void openProjectWorld(MinecraftClient client, Path projectPath) {
        if (projectPath == null) {
            return;
        }
        ProjectStorage.recordRecentProject(projectPath);
        String timestamp = ID_FORMAT.format(LocalDateTime.now());
        this.pendingProjectPath = projectPath;
        this.pendingWorldId = "rmc_chart_editor_" + timestamp;
        this.launchPending = true;
        this.scaffoldPrepared = false;
        this.editorVisible = false;

        LevelInfo levelInfo = new LevelInfo(
                "RhythMC Chart Editor",
                GameMode.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                new GameRules(DataConfiguration.SAFE_MODE.enabledFeatures()),
                DataConfiguration.SAFE_MODE
        );

        client.createIntegratedServerLoader().createAndStart(
                pendingWorldId,
                levelInfo,
                new GeneratorOptions(0L, false, false),
                WorldPresets::createTestOptions,
                client.currentScreen
        );
    }

    private void prepareEditorWorld(MinecraftClient client) {
        IntegratedServer server = client.getServer();
        if (server == null) {
            return;
        }

        String playerName = client.player.getName().getString();
        editorState.loadProject(pendingProjectPath);
        editorState.seekToBeat(0.0);

        server.execute(() -> {
            run(server, "gamerule doDaylightCycle false");
            run(server, "gamerule doWeatherCycle false");
            run(server, "gamerule doMobSpawning false");
            run(server, "weather clear");
            run(server, "time set 6000");
            run(server, "kill @e[type=text_display,tag=rmc_editor]");
            run(server, "fill -36 0 -48 36 140 48 air");
            run(server, "fill -14 63 -34 14 63 34 white_stained_glass");
            run(server, "fill -14 64 -34 -14 64 34 light_blue_stained_glass");
            run(server, "fill 14 64 -34 14 64 34 light_blue_stained_glass");
            run(server, "fill -14 64 -34 14 64 -34 cyan_stained_glass");
            run(server, "fill -14 64 34 14 64 34 cyan_stained_glass");
            run(server, "fill -10 64 -30 -10 64 30 yellow_stained_glass");
            run(server, "fill -5 64 -30 -5 64 30 yellow_stained_glass");
            run(server, "fill 0 64 -30 0 64 30 yellow_stained_glass");
            run(server, "fill 5 64 -30 5 64 30 yellow_stained_glass");
            run(server, "fill 10 64 -30 10 64 30 yellow_stained_glass");
            run(server, "summon minecraft:text_display 0.5 69 -30.5 {Tags:[\"rmc_editor\"],billboard:\"center\",background:0,see_through:1b,text:'{\"text\":\"RhythMC Chart Editor\",\"bold\":true,\"color\":\"aqua\"}'}");
            run(server, "summon minecraft:text_display 0.5 67.5 -30.5 {Tags:[\"rmc_editor\"],billboard:\"center\",background:0,see_through:1b,text:'{\"text\":\"Visual grid loaded - open overlay to edit\",\"color\":\"white\"}'}");
            run(server, "summon minecraft:text_display -10 66.5 -31 {Tags:[\"rmc_editor\"],billboard:\"center\",background:0,see_through:1b,text:'{\"text\":\"Track 0\",\"color\":\"yellow\"}'}");
            run(server, "summon minecraft:text_display -5 66.5 -31 {Tags:[\"rmc_editor\"],billboard:\"center\",background:0,see_through:1b,text:'{\"text\":\"Track 1\",\"color\":\"yellow\"}'}");
            run(server, "summon minecraft:text_display 0 66.5 -31 {Tags:[\"rmc_editor\"],billboard:\"center\",background:0,see_through:1b,text:'{\"text\":\"Track 2\",\"color\":\"yellow\"}'}");
            run(server, "summon minecraft:text_display 5 66.5 -31 {Tags:[\"rmc_editor\"],billboard:\"center\",background:0,see_through:1b,text:'{\"text\":\"Track 3\",\"color\":\"yellow\"}'}");
            run(server, "summon minecraft:text_display 10 66.5 -31 {Tags:[\"rmc_editor\"],billboard:\"center\",background:0,see_through:1b,text:'{\"text\":\"Track 4\",\"color\":\"yellow\"}'}");
            run(server, "tp " + playerName + " 0.5 66.0 0.5 180 10");
            run(server, "gamemode creative " + playerName);
            worldController.syncNow(client);
        });
    }

    private void run(IntegratedServer server, String command) {
        server.getCommandManager().parseAndExecute(server.getCommandSource(), command);
    }

    private void showEditor(MinecraftClient client) {
        client.setScreen(new ChartEditorScreen(editorState));
        editorVisible = true;
    }

    private void hideEditor(MinecraftClient client) {
        client.setScreen(null);
        editorVisible = false;
    }
}

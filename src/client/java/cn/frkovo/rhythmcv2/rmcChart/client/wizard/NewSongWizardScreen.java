package cn.frkovo.rhythmcv2.rmcChart.client.wizard;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectStorage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class NewSongWizardScreen extends Screen {
    private final Screen parent;

    private TextFieldWidget titleField;
    private TextFieldWidget composerField;
    private TextFieldWidget folderField;
    private TextFieldWidget descriptionField;
    private TextFieldWidget songPathField;
    private String status = "Fill song info, then create the editor world.";
    private Path selectedSongPath;
    private boolean autoFolder = true;
    private boolean suppressFolderChange;

    public NewSongWizardScreen(Screen parent) {
        super(Text.literal("Create New RhythMC Song"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = 360;
        int left = this.width / 2 - panelWidth / 2;
        int top = this.height / 2 - 122;

        titleField = addField(left, top + 20, panelWidth, "Song Title", "", "Visible song title");
        composerField = addField(left, top + 60, panelWidth, "Composer", "", "Artist / composer name");
        folderField = addField(left, top + 100, panelWidth, "Folder Name", "", "Project folder name");
        descriptionField = addField(left, top + 140, panelWidth, "Description", "", "Optional description");
        songPathField = addField(left, top + 180, panelWidth - 98, "Song File", "", "Choose an audio file to copy into the project");
        songPathField.setEditable(false);
        songPathField.setSuggestion("No song selected");

        titleField.setChangedListener(value -> {
            if (autoFolder) {
                suppressFolderChange = true;
                folderField.setText(slugify(value));
                suppressFolderChange = false;
            }
        });
        folderField.setChangedListener(value -> {
            if (!suppressFolderChange) {
                autoFolder = false;
            }
        });

        addDrawableChild(ButtonWidget.builder(Text.literal("Pick..."), button -> pickSong())
                .dimensions(left + panelWidth - 92, top + 180, 92, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Create World"), button -> createWorld())
                .dimensions(left, top + 222, 174, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(left + 186, top + 222, 174, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = 380;
        int panelHeight = 288;
        int left = this.width / 2 - panelWidth / 2;
        int top = this.height / 2 - panelHeight / 2;

        context.fill(left, top, left + panelWidth, top + panelHeight, 0xCC10141C);
        context.fill(left, top, left + panelWidth, top + 1, 0xFF4FC3F7);
        context.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, 0xFF4FC3F7);
        context.fill(left, top, left + 1, top + panelHeight, 0xFF4FC3F7);
        context.fill(left + panelWidth - 1, top, left + panelWidth, top + panelHeight, 0xFF4FC3F7);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, this.width / 2, top + 8, 0xFFFFFF);
        context.drawText(textRenderer, Text.literal("Song Title"), left + 10, top + 10, 0xA0D8FF, false);
        context.drawText(textRenderer, Text.literal("Composer"), left + 10, top + 50, 0xA0D8FF, false);
        context.drawText(textRenderer, Text.literal("Folder Name"), left + 10, top + 90, 0xA0D8FF, false);
        context.drawText(textRenderer, Text.literal("Description"), left + 10, top + 130, 0xA0D8FF, false);
        context.drawText(textRenderer, Text.literal("Song File"), left + 10, top + 170, 0xA0D8FF, false);
        context.drawText(textRenderer, Text.literal("Audio metadata will auto-fill title/composer/length when available."), left + 10, top + 206, 0x90B4CC, false);
        context.drawText(textRenderer, Text.literal(status), left + 10, top + 254, 0xE8E8E8, false);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private TextFieldWidget addField(int x, int y, int width, String label, String defaultValue, String placeholder) {
        TextFieldWidget widget = addDrawableChild(new TextFieldWidget(textRenderer, x, y, width, 18, Text.literal(label)));
        widget.setMaxLength(256);
        widget.setText(defaultValue);
        widget.setPlaceholder(Text.literal(placeholder));
        return widget;
    }

    private void pickSong() {
        Path picked = SongFilePicker.chooseAudioFile();
        if (picked == null) {
            status = "Song selection cancelled.";
            return;
        }

        selectedSongPath = picked;
        songPathField.setText(picked.toString());

        ImportedSongMetadata metadata = SongImportHelper.readMetadata(picked);
        if (titleField.getText().isBlank() && !metadata.title().isBlank()) {
            titleField.setText(metadata.title());
        }
        if (composerField.getText().isBlank() && !metadata.composer().isBlank()) {
            composerField.setText(metadata.composer());
        }
        if (descriptionField.getText().isBlank() && !metadata.description().isBlank()) {
            descriptionField.setText(metadata.description());
        }
        if (autoFolder && !metadata.title().isBlank()) {
            suppressFolderChange = true;
            folderField.setText(slugify(metadata.title()));
            suppressFolderChange = false;
        }

        status = metadata.lengthMillis() > 0
                ? "Imported song metadata. Length: " + metadata.lengthMillis() + " ms"
                : "Selected song file. No embedded metadata found.";
    }

    private void createWorld() {
        String title = titleField.getText().trim();
        String composer = composerField.getText().trim();
        String folder = slugify(folderField.getText().trim());
        String description = descriptionField.getText().trim();

        if (title.isBlank()) {
            status = "Song title is required.";
            return;
        }
        if (composer.isBlank()) {
            status = "Composer is required.";
            return;
        }
        if (folder.isBlank()) {
            status = "Folder name is required.";
            return;
        }

        Path baseDir = ProjectStorage.projectsRoot();
        Path projectPath = baseDir.resolve(folder);
        if (Files.exists(projectPath)) {
            status = "Folder already exists: " + folder;
            return;
        }

        RmcChartClient.launchNewSongEditor(new NewSongWizardData(title, composer, folder, description, selectedSongPath));
    }

    private String slugify(String input) {
        String base = input.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return base.isBlank() ? "untitled-song" : base;
    }
}

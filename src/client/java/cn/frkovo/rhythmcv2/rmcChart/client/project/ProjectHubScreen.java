package cn.frkovo.rhythmcv2.rmcChart.client.project;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.client.wizard.NewSongWizardScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProjectHubScreen extends Screen {
    private final Screen parent;
    private final List<ProjectSummary> recentProjects = new ArrayList<>();
    private ProjectSummary continueProject;

    public ProjectHubScreen(Screen parent) {
        super(Text.literal("RhythMC Chart Workspace"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!RmcChartClient.canOpenEditor(MinecraftClient.getInstance())) {
            client.setScreen(parent);
            return;
        }
        recentProjects.clear();
        recentProjects.addAll(ProjectStorage.listRecentProjectSummaries());
        continueProject = ProjectStorage.mostRecentProjectSummary();

        int centerX = width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Continue Last Edit"), button -> {
                    if (continueProject != null) {
                        RmcChartClient.openExistingProject(continueProject.path());
                    }
                })
                .dimensions(centerX - 156, 92, 150, 20)
                .build()).active = continueProject != null;
        addDrawableChild(ButtonWidget.builder(Text.literal("New Project"), button -> client.setScreen(new NewSongWizardScreen(this)))
                .dimensions(centerX + 6, 92, 150, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Open Projects"), button -> client.setScreen(new ProjectBrowserScreen(this)))
                .dimensions(centerX - 156, 116, 312, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(centerX - 70, height - 30, 140, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = width / 2 - 220;
        int top = 36;
        int panelWidth = 440;
        int panelHeight = height - 76;

        context.fill(left, top, left + panelWidth, top + panelHeight, 0xCC10141C);
        context.fill(left, top, left + panelWidth, top + 1, 0xFF4FC3F7);
        context.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, 0xFF4FC3F7);
        context.fill(left, top, left + 1, top + panelHeight, 0xFF4FC3F7);
        context.fill(left + panelWidth - 1, top, left + panelWidth, top + panelHeight, 0xFF4FC3F7);

        int rowY = 154;
        if (!recentProjects.isEmpty()) {
            for (int index = 0; index < Math.min(5, recentProjects.size()); index++) {
                ProjectSummary summary = recentProjects.get(index);
                int cardY = rowY + 14 + index * 54;
                drawProjectCardBackground(context, summary, left + 12, cardY, panelWidth - 24, index == 0);
                context.fill(left + panelWidth - 100, cardY + 12, left + panelWidth - 20, cardY + 32, 0x88407B39);
            }
        }

        super.render(context, mouseX, mouseY, delta);

        TextRenderer renderer = textRenderer != null ? textRenderer : client.textRenderer;
        if (renderer == null) {
            return;
        }

        context.drawCenteredTextWithShadow(renderer, title, width / 2, top + 10, 0xFFFFFFFF);
        context.drawText(renderer, Text.literal("Connected to RhythMC-Preview. Start a new chart or reopen an existing project."), left + 12, top + 30, 0xFFFFFFFF, false);
        context.drawText(renderer, Text.literal(continueProject == null ? "Continue Last Edit: none yet" : "Continue Last Edit: " + continueProject.name()), left + 12, top + 46, 0xFFFFD580, false);

        context.drawText(renderer, Text.literal("Recent Projects"), left + 12, rowY, 0xFFFFD580, false);
        rowY += 14;
        if (recentProjects.isEmpty()) {
            context.drawText(renderer, Text.literal("No recent projects yet."), left + 12, rowY + 4, 0xFF90A8B8, false);
        } else {
            for (int index = 0; index < Math.min(5, recentProjects.size()); index++) {
                ProjectSummary summary = recentProjects.get(index);
                int cardY = rowY + index * 54;
                drawProjectCardText(context, renderer, summary, left + 12, cardY, panelWidth - 24);
                context.drawCenteredTextWithShadow(renderer, Text.literal("Open"), left + panelWidth - 60, cardY + 18, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        int left = width / 2 - 220;
        int panelWidth = 440;
        int rowY = 168;
        for (int index = 0; index < Math.min(5, recentProjects.size()); index++) {
            ProjectSummary summary = recentProjects.get(index);
            int cardY = rowY + index * 54;
            if (click.x() >= left + 12 && click.x() <= left + panelWidth - 12 && click.y() >= cardY && click.y() <= cardY + 46) {
                RmcChartClient.openExistingProject(summary.path());
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        if (client != null && client.world != null) {
            client.setScreen(parent instanceof TitleScreen ? null : parent);
            return;
        }
        client.setScreen(parent instanceof TitleScreen ? parent : new TitleScreen());
    }

    private String formatMillis(int millis) {
        int totalSeconds = Math.max(0, millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private void drawProjectCardBackground(DrawContext context, ProjectSummary summary, int x, int y, int width, boolean latest) {
        context.fill(x, y, x + width, y + 46, latest ? 0x66503828 : 0x66403A28);
        drawOutline(context, x, y, width, 46, latest ? 0xFFCC9B52 : 0xFF6F6A58);
        drawThumbnailBackground(context, summary, x + 8, y + 5, 42, 36, latest ? 0xFFCC9B52 : 0xFF4FC3F7);
    }

    private void drawProjectCardText(DrawContext context, TextRenderer renderer, ProjectSummary summary, int x, int y, int width) {
        drawThumbnailInitial(context, renderer, summary, x + 8, y + 5, 42, 36);
        context.drawText(renderer, Text.literal(summary.name()), x + 58, y + 8, 0xFFFFFFFF, false);
        context.drawText(renderer, Text.literal(summary.composer() + " | " + summary.folderName()), x + 58, y + 20, 0xFFBFD8E6, false);
        context.drawText(renderer, Text.literal("Length " + formatMillis(summary.lengthMillis()) + " | Updated " + formatDate(summary.lastModifiedMillis())), x + 58, y + 32, 0xFF9AC7A5, false);
    }

    private void drawThumbnailBackground(DrawContext context, ProjectSummary summary, int x, int y, int width, int height, int accent) {
        context.fill(x, y, x + width, y + height, 0x88161C24);
        drawOutline(context, x, y, width, height, accent);
        int bars = Math.max(1, Math.min(4, summary.difficultyCount()));
        for (int i = 0; i < bars; i++) {
            int barX = x + 5 + i * 8;
            context.fill(barX, y + height - 9, barX + 5, y + height - 4, accent);
        }
    }

    private void drawThumbnailInitial(DrawContext context, TextRenderer renderer, ProjectSummary summary, int x, int y, int width, int height) {
        String initials = summary.name().isBlank() ? "P" : summary.name().substring(0, 1).toUpperCase(Locale.ROOT);
        context.drawCenteredTextWithShadow(renderer, Text.literal(initials), x + width / 2, y + 5, 0xFFFFFFFF);
    }

    private void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private String formatDate(long millis) {
        if (millis <= 0L) {
            return "unknown";
        }
        java.util.Date date = new java.util.Date(millis);
        return String.format(Locale.ROOT, "%1$tm-%1$td %1$tH:%1$tM", date);
    }
}

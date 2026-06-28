package cn.frkovo.rhythmcv2.rmcChart.client.project;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectBrowserScreen extends Screen {
    private final Screen parent;
    private final List<ProjectSummary> allProjects = new ArrayList<>();
    private final List<ProjectSummary> recentProjects = new ArrayList<>();
    private final List<ProjectSummary> filteredProjects = new ArrayList<>();

    private TextFieldWidget searchField;
    private int scroll;
    private String status = "Choose a project from .minecraft/projects";
    private SortMode sortMode = SortMode.UPDATED;

    public ProjectBrowserScreen(Screen parent) {
        super(Text.literal("Open RhythMC Project"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 220;
        searchField = addDrawableChild(new TextFieldWidget(textRenderer, left + 12, 82, 300, 18, Text.literal("Search")));
        searchField.setSuggestion("Search by project folder / song / composer");
        searchField.setChangedListener(value -> applyFilter());

        refreshProjects();
        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> refreshProjects())
                .dimensions(width / 2 + 98, 82, 70, 18)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sort: Updated"), button -> toggleSort(button))
                .dimensions(width / 2 + 174, 82, 110, 18)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(width / 2 - 70, height - 30, 140, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = width / 2 - 220;
        int top = 36;
        int panelWidth = 440;
        int panelHeight = height - 76;

        context.fill(left, top, left + panelWidth, top + panelHeight, 0xCC10141C);
        drawOutline(context, left, top, panelWidth, panelHeight, 0xFF4FC3F7);

        int contentTop = top + 72;
        int rowY = contentTop + 24 - scroll;

        if (searchField.getText().isBlank() && !recentProjects.isEmpty()) {
            for (ProjectSummary summary : recentProjects) {
                drawProjectCardBackground(context, summary, left + 12, rowY, panelWidth - 24, true, true);
                rowY += 58;
            }
            rowY += 8;
        }

        for (ProjectSummary summary : filteredProjects) {
            drawProjectCardBackground(context, summary, left + 12, rowY, panelWidth - 24, false, false);
            rowY += 58;
        }

        super.render(context, mouseX, mouseY, delta);

        TextRenderer renderer = textRenderer != null ? textRenderer : client.textRenderer;
        if (renderer == null) {
            return;
        }

        context.drawCenteredTextWithShadow(renderer, title, width / 2, top + 8, 0xFFFFFFFF);
        context.drawText(renderer, Text.literal(status), left + 12, top + 24, 0xFFFFFFFF, false);

        rowY = contentTop + 24 - scroll;
        if (searchField.getText().isBlank() && !recentProjects.isEmpty()) {
            context.drawText(renderer, Text.literal("Recent Projects"), left + 12, rowY - 14, 0xFFFFD580, false);
            for (ProjectSummary summary : recentProjects) {
                drawProjectCardText(context, renderer, summary, left + 12, rowY, panelWidth - 24, true);
                rowY += 58;
            }
            rowY += 8;
        }

        context.drawText(renderer, Text.literal((searchField.getText().isBlank() ? "All Projects" : "Search Results") + " - Sort: " + sortMode.label), left + 12, rowY - 14, 0xFF9FD7FF, false);
        for (ProjectSummary summary : filteredProjects) {
            drawProjectCardText(context, renderer, summary, left + 12, rowY, panelWidth - 24, false);
            rowY += 58;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        ClickTarget target = findClickTarget(click.x(), click.y());
        if (target != null) {
            RmcChartClient.openExistingProject(target.summary.path());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int cardCount = filteredProjects.size() + (searchField != null && searchField.getText().isBlank() ? recentProjects.size() : 0);
        int maxScroll = Math.max(0, cardCount * 58 - (height - 160));
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(verticalAmount * 22.0)));
        return true;
    }

    @Override
    public void close() {
        if (client != null && client.world != null) {
            client.setScreen(parent);
            return;
        }
        client.setScreen(parent);
    }

    private void refreshProjects() {
        allProjects.clear();
        recentProjects.clear();
        allProjects.addAll(ProjectStorage.listProjectSummaries());
        recentProjects.addAll(ProjectStorage.listRecentProjectSummaries());
        applyFilter();
        status = allProjects.isEmpty() ? "No saved projects found in .minecraft/projects" : "Found " + allProjects.size() + " project(s) in .minecraft/projects";
        scroll = 0;
    }

    private void applyFilter() {
        filteredProjects.clear();
        String search = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        for (ProjectSummary summary : allProjects) {
            if (search.isBlank() || matches(summary, search)) {
                filteredProjects.add(summary);
            }
        }
        sortProjects(filteredProjects);
    }

    private boolean matches(ProjectSummary summary, String search) {
        return summary.folderName().toLowerCase(Locale.ROOT).contains(search)
                || summary.name().toLowerCase(Locale.ROOT).contains(search)
                || summary.composer().toLowerCase(Locale.ROOT).contains(search);
    }

    private void drawProjectCardBackground(DrawContext context, ProjectSummary summary, int x, int y, int width, boolean recent, boolean recentBadge) {
        if (y + 52 < 84 || y > height - 44) {
            return;
        }
        context.fill(x, y, x + width, y + 50, recent ? 0x66403A28 : 0x66303030);
        drawOutline(context, x, y, width, 50, recent ? 0xFFCC9B52 : 0xFF506070);
        drawThumbnailBackground(context, summary, x + 8, y + 6, 46, 38, recent ? 0xFFCC9B52 : 0xFF4FC3F7);
        if (recentBadge) {
            context.fill(x + 62, y + 6, x + 108, y + 16, 0x88CC9B52);
        }
        context.fill(x + width - 92, y + 12, x + width - 12, y + 36, 0x88407B39);
    }

    private void drawProjectCardText(DrawContext context, TextRenderer renderer, ProjectSummary summary, int x, int y, int width, boolean recentBadge) {
        if (y + 52 < 84 || y > height - 44) {
            return;
        }
        drawThumbnailInitial(context, renderer, summary, x + 8, y + 6, 46, 38);
        context.drawText(renderer, Text.literal(summary.name()), x + 62, y + 8, 0xFFFFFFFF, false);
        context.drawText(renderer, Text.literal("By " + summary.composer() + " | " + summary.folderName()), x + 62, y + 20, 0xFFBFD8E6, false);
        context.drawText(renderer, Text.literal("Length " + formatMillis(summary.lengthMillis()) + " | Difficulties " + summary.difficultyCount() + " | Updated " + formatDate(summary.lastModifiedMillis())), x + 62, y + 32, 0xFF9AC7A5, false);
        if (recentBadge) {
            context.drawText(renderer, Text.literal("RECENT"), x + 68, y + 8, 0xFFFFFFFF, false);
        }
        context.drawCenteredTextWithShadow(renderer, Text.literal("Open"), x + width - 52, y + 20, 0xFFFFFFFF);
    }

    private void drawThumbnailBackground(DrawContext context, ProjectSummary summary, int x, int y, int width, int height, int accent) {
        context.fill(x, y, x + width, y + height, 0x88161C24);
        drawOutline(context, x, y, width, height, accent);
        int bars = Math.max(1, Math.min(4, summary.difficultyCount()));
        for (int i = 0; i < bars; i++) {
            int barX = x + 6 + i * 9;
            context.fill(barX, y + height - 10, barX + 6, y + height - 4, accent);
        }
    }

    private void drawThumbnailInitial(DrawContext context, TextRenderer renderer, ProjectSummary summary, int x, int y, int width, int height) {
        String initials = summary.name().isBlank() ? "P" : summary.name().substring(0, 1).toUpperCase(Locale.ROOT);
        context.drawCenteredTextWithShadow(renderer, Text.literal(initials), x + width / 2, y + 6, 0xFFFFFFFF);
    }

    private void toggleSort(ButtonWidget button) {
        sortMode = sortMode == SortMode.UPDATED ? SortMode.NAME : SortMode.UPDATED;
        button.setMessage(Text.literal("Sort: " + sortMode.label));
        sortProjects(allProjects);
        sortProjects(recentProjects);
        applyFilter();
    }

    private void sortProjects(List<ProjectSummary> projects) {
        Comparator<ProjectSummary> comparator = sortMode == SortMode.UPDATED
                ? Comparator.comparingLong(ProjectSummary::lastModifiedMillis).reversed().thenComparing(ProjectSummary::folderName)
                : Comparator.comparing(ProjectSummary::folderName, String.CASE_INSENSITIVE_ORDER);
        projects.sort(comparator);
    }

    private ClickTarget findClickTarget(double mouseX, double mouseY) {
        int left = width / 2 - 220;
        int panelWidth = 440;
        int rowY = 132 - scroll;

        if (searchField.getText().isBlank() && !recentProjects.isEmpty()) {
            for (ProjectSummary summary : recentProjects) {
                if (isInside(mouseX, mouseY, left + 12, rowY, panelWidth - 24, 50)) {
                    return new ClickTarget(summary);
                }
                rowY += 58;
            }
            rowY += 8;
        }

        for (ProjectSummary summary : filteredProjects) {
            if (isInside(mouseX, mouseY, left + 12, rowY, panelWidth - 24, 50)) {
                return new ClickTarget(summary);
            }
            rowY += 58;
        }
        return null;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private String formatMillis(int millis) {
        int totalSeconds = Math.max(0, millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private String formatDate(long millis) {
        if (millis <= 0L) {
            return "unknown";
        }
        Date date = new Date(millis);
        return String.format(Locale.ROOT, "%1$tm-%1$td %1$tH:%1$tM", date);
    }

    private record ClickTarget(ProjectSummary summary) {
    }

    private enum SortMode {
        UPDATED("Updated"),
        NAME("Name");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }
    }
}

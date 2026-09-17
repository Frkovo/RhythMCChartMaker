package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class EditorChrome {
    private final ChartEditorScreen screen;

    public EditorChrome(ChartEditorScreen screen) {
        this.screen = screen;
    }

    void drawEditorChrome(DrawContext context) {
        int width = screen.publicWidth();
        context.fill(0, 0, width, screen.TOP_BAR_HEIGHT - 1, screen.UI_TOP_BG);
        context.fill(0, screen.TOP_BAR_HEIGHT - 1, width, screen.TOP_BAR_HEIGHT, screen.UI_BORDER);

        // Row 1: identity on the left, connection/save status pills right-aligned.
        int statusPillX = width - screen.OUTER_PADDING - screen.TOP_BAR_STATUS_PILLS_WIDTH;
        int pillY = screen.TOP_BAR_ROW1_Y - 1;
        drawPill(context, statusPillX, pillY, 88, 18, previewHandshakeLabel(), previewHandshakeColor());
        drawPill(context, statusPillX + 92, pillY, 72, 18, projectSaveLabel(), screen.state.projectDirty() ? screen.UI_WARN : screen.UI_GREEN);
        drawPill(context, statusPillX + 168, pillY, 88, 18, previewChartLabel(), previewChartColor());
        drawPill(context, statusPillX + 260, pillY, 72, 18, screen.state.serverPreviewRunning() ? "Srv Live" : "Srv Idle", screen.state.serverPreviewRunning() ? screen.UI_GREEN : screen.UI_DIM);
        drawPill(context, statusPillX + 336, pillY, 80, 18, previewAssetsLabel(), previewAssetsColor());

        int textX = screen.OUTER_PADDING;
        int textY = screen.TOP_BAR_ROW1_Y + 4;
        context.drawText(screen.getTextRenderer(), Text.literal("RhythMC Chart Maker"), textX, textY, screen.UI_ACCENT, false);
        textX += screen.getTextRenderer().getWidth("RhythMC Chart Maker") + 8;
        String projectName = screen.state.project().manifest().name();
        int projectNameWidth = Math.max(0, Math.min(180, statusPillX - textX - 72));
        drawTrimmedText(context, projectName, textX, textY, projectNameWidth, screen.UI_TEXT);
        textX += projectNameWidth + 8;
        int pathMaxWidth = statusPillX - 8 - textX;
        if (pathMaxWidth > 60) {
            drawTrimmedText(context, screen.state.project().projectPath().toString(), textX, textY, pathMaxWidth, screen.UI_DIM);
        }

        // Row 2: action/difficulty buttons are real widgets; only playback pills are drawn here.
        int playbackPillX = width - screen.OUTER_PADDING - screen.TOP_BAR_PLAYBACK_PILLS_WIDTH;
        drawPill(context, playbackPillX, screen.TOP_BAR_ROW2_Y, 76, 18, "Beat " + screen.propertyPanel.format(screen.state.playheadBeat()), screen.UI_WARN);
        drawPill(context, playbackPillX + 80, screen.TOP_BAR_ROW2_Y, 76, 18, screen.state.showOnlySelectedTrack() ? "1 Track" : "All Tracks", screen.state.showOnlySelectedTrack() ? screen.UI_ACCENT : screen.UI_DIM);
    }

    void drawStatusBar(DrawContext context) {
        int y = screen.publicHeight() - screen.STATUS_BAR_HEIGHT;
        int hotbarLeft = Math.max(0, screen.publicWidth() / 2 - 96);
        int hotbarRight = Math.min(screen.publicWidth(), screen.publicWidth() / 2 + 96);
        context.fill(0, y, hotbarLeft, screen.publicHeight(), 0xF0161C22);
        context.fill(hotbarRight, y, screen.publicWidth(), screen.publicHeight(), 0xF0161C22);
        context.fill(0, y, hotbarLeft, y + 1, screen.UI_BORDER_SOFT);
        context.fill(hotbarRight, y, screen.publicWidth(), y + 1, screen.UI_BORDER_SOFT);
        String hint = statusHint();
        drawTrimmedText(context, screen.state.statusMessage(), screen.OUTER_PADDING, y + 6,
                Math.max(0, hotbarLeft - screen.OUTER_PADDING * 2), screen.UI_TEXT);
        drawTrimmedText(context, hint, hotbarRight + 6, y + 6,
                Math.max(0, screen.publicWidth() - hotbarRight - screen.OUTER_PADDING - 6), screen.UI_MUTED);
    }

    private String statusHint() {
        if (screen.smoothing.smoothingPopupOpen) {
            return "Enter apply | Esc close | Fill uses 1/x division";
        }
        if (screen.selectionManager.hasSelectedNotes()) {
            return "Ctrl+Click/drag multi-select | Ctrl+P smoothing | Alt+Arrows move";
        }
        return "1-9 hotbar tools | Space local play";
    }

    private String previewHandshakeLabel() {
        return RmcChartClient.getPreviewClient().isReady() ? "Server Ready" : "Server Wait";
    }

    private int previewHandshakeColor() {
        return RmcChartClient.getPreviewClient().isReady() ? screen.UI_GREEN : screen.UI_WARN;
    }

    private String projectSaveLabel() {
        return screen.state.projectDirty() ? "Unsaved" : "Saved";
    }

    private String previewChartLabel() {
        if (screen.state.previewUploading()) {
            return "Uploading";
        }
        if (screen.state.previewChartDirty()) {
            return "Chart Dirty";
        }
        if (screen.state.previewChartUploaded()) {
            return "Chart Sent";
        }
        return "Chart Idle";
    }

    private int previewChartColor() {
        if (screen.state.previewUploading()) {
            return screen.UI_ACCENT;
        }
        if (screen.state.previewChartDirty()) {
            return screen.UI_WARN;
        }
        if (screen.state.previewChartUploaded()) {
            return screen.UI_GREEN;
        }
        return screen.UI_DIM;
    }

    private String previewAssetsLabel() {
        String schematic = screen.state.previewSchematicUploaded() ? "S" : "-";
        String audio = screen.state.previewAudioUploaded() ? "A" : "-";
        return "Assets " + schematic + audio;
    }

    private int previewAssetsColor() {
        return screen.state.previewAudioUploaded() || screen.state.previewSchematicUploaded() ? screen.UI_ACCENT : screen.UI_DIM;
    }

    void drawSmoothingPopupBackground(DrawContext context) {
        if (!screen.smoothing.smoothingPopupOpen) {
            return;
        }
        int popupX = smoothingPopupX();
        int popupY = smoothingPopupY();
        context.fill(0, 0, screen.publicWidth(), screen.publicHeight(), 0x99000000);
        context.fill(popupX, popupY, popupX + screen.SMOOTHING_POPUP_WIDTH, popupY + screen.SMOOTHING_POPUP_HEIGHT, 0xF01A222A);
        drawOutline(context, popupX, popupY, screen.SMOOTHING_POPUP_WIDTH, screen.SMOOTHING_POPUP_HEIGHT, screen.UI_ACCENT);
        context.fill(popupX, popupY, popupX + screen.SMOOTHING_POPUP_WIDTH, popupY + 32, screen.UI_PANEL_STRONG);
    }

    void drawSmoothingPopupText(DrawContext context) {
        if (!screen.smoothing.smoothingPopupOpen) {
            return;
        }
        int popupX = smoothingPopupX();
        int popupY = smoothingPopupY();
        context.drawText(screen.getTextRenderer(), Text.literal("Smoothing"), popupX + 14, popupY + 11, screen.UI_TEXT, false);
        context.drawText(screen.getTextRenderer(), Text.literal(screen.selectionManager.currentSelectedNotes().size() + " selected note(s)"), popupX + 222, popupY + 11, screen.UI_MUTED, false);
        context.drawText(screen.getTextRenderer(), Text.literal("Curve Type"), popupX + 18, popupY + 42, screen.UI_MUTED, false);
        context.drawText(screen.getTextRenderer(), Text.literal("Note Type"), popupX + 18, popupY + 82, screen.UI_MUTED, false);
        context.drawText(screen.getTextRenderer(), Text.literal("Fill"), popupX + 128, popupY + 82, screen.UI_MUTED, false);
        context.drawText(screen.getTextRenderer(), Text.literal("Division"), popupX + 238, popupY + 82, screen.UI_MUTED, false);
        drawTrimmedText(context, "Curve cycles Catmull, Circle, plus every EasingType curve.", popupX + 18, popupY + 122, screen.SMOOTHING_POPUP_WIDTH - 36, screen.UI_DIM);
        drawTrimmedText(context, "Fill creates notes on the selected track's 1/x grid between anchors.", popupX + 18, popupY + 138, screen.SMOOTHING_POPUP_WIDTH - 36, screen.UI_DIM);
    }

    void drawGroupNamePopupBackground(DrawContext context) {
        if (!screen.groupNamePopupOpen) {
            return;
        }
        int popupX = screen.groupNamePopupX();
        int popupY = screen.groupNamePopupY();
        context.fill(0, 0, screen.publicWidth(), screen.publicHeight(), 0x99000000);
        context.fill(popupX, popupY, popupX + 220, popupY + 72, 0xF01A222A);
        drawOutline(context, popupX, popupY, 220, 72, screen.UI_ACCENT);
    }

    void drawGroupNamePopupText(DrawContext context) {
        if (!screen.groupNamePopupOpen) {
            return;
        }
        int popupX = screen.groupNamePopupX();
        int popupY = screen.groupNamePopupY();
        context.drawText(screen.getTextRenderer(), Text.literal("Group Name"), popupX + 10, popupY + 8, screen.UI_TEXT, false);
    }

    int smoothingPopupX() {
        return Math.max(10, screen.publicWidth() / 2 - screen.SMOOTHING_POPUP_WIDTH / 2);
    }

    int smoothingPopupY() {
        return Math.max(10, screen.publicHeight() / 2 - screen.SMOOTHING_POPUP_HEIGHT / 2);
    }

    int transportButtonWidth(int width) {
        return Math.max(18, (width - 12 - 4 * 4) / 5);
    }

    int transportButtonX(int x, int width, int index) {
        return x + 6 + index * (transportButtonWidth(width) + 4);
    }

    private void drawPill(DrawContext context, int x, int y, int pillWidth, int pillHeight, String label, int color) {
        context.fill(x, y, x + pillWidth, y + pillHeight, 0x66232D36);
        context.fill(x, y, x + 2, y + pillHeight, color);
        drawOutline(context, x, y, pillWidth, pillHeight, screen.UI_BORDER_SOFT);
        drawTrimmedText(context, label, x + 7, y + 5, pillWidth - 10, color);
    }

    void drawLeftPanel(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, screen.UI_PANEL);
        drawOutline(context, x, y, width, height, screen.UI_BORDER);
        context.enableScissor(x, y, x + width, y + height);
        context.drawText(screen.getTextRenderer(), Text.literal("Tracks"), x + 10, y + 9, screen.UI_TEXT, false);
        context.drawText(screen.getTextRenderer(), Text.literal(screen.state.activeDifficulty().displayName()), x + width - 74, y + 9, screen.UI_ACCENT, false);

        int rowY = y + 26;
        for (TrackData track : screen.state.tracks()) {
            boolean selected = screen.state.selection().track() == track || (screen.state.selection().kind() == EditorSelection.Kind.NOTE && screen.state.selection().track() == track);
            int color = selected ? 0xCC315F91 : 0x66303A44;
            context.fill(x + 8, rowY, x + width - 8, rowY + 26, color);
            context.fill(x + 12, rowY + 6, x + 24, rowY + 20, selected ? screen.UI_ACCENT : 0x8848596A);
            context.drawText(screen.getTextRenderer(), Text.literal(screen.state.isTrackExpanded(track) ? "-" : "+"), x + 16, rowY + 9, screen.UI_TEXT, false);
            drawTrimmedText(context, "Track " + track.id(), x + 32, rowY + 4, width - 82, screen.UI_TEXT);
            drawTrimmedText(context, track.notes().size() + " notes | " + screen.timeline.countTrackEvents(track) + " events", x + 32, rowY + 15, width - 48, screen.UI_MUTED);
            rowY += 30;
        }
        rowY += 8;
        drawStatLine(context, x + 10, rowY, "Effects", Integer.toString(screen.state.level().effects().size()), screen.UI_WARN);
        rowY += 15;
        drawStatLine(context, x + 10, rowY, "BPM", Integer.toString(screen.state.level().meta().bpms().size()), screen.UI_ACCENT);
        rowY += 15;
        drawTrimmedText(context, "Audio: " + screen.state.audioStatus(), x + 10, rowY, width - 20, screen.UI_GREEN);
        rowY += 15;
        MinecraftClient client = screen.getClient();
        if (client != null && client.world != null) {
            drawTrimmedText(context, "World preview active", x + 10, rowY, width - 20, screen.UI_WARN);
            rowY += 15;
        }
        context.disableScissor();
    }

    void drawPreviewPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, screen.UI_PANEL);
        drawOutline(context, x, y, width, height, screen.UI_BORDER);

        int transportY = y + 6;
        int buttonWidth = transportButtonWidth(width);
        drawTransportButton(context, transportButtonX(x, width, 0), transportY, buttonWidth, screen.state.playing() ? "Stop" : "Play", screen.state.playing() ? screen.UI_WARN : screen.UI_GREEN);
        drawTransportButton(context, transportButtonX(x, width, 1), transportY, buttonWidth, "Restart", screen.UI_ACCENT);
        boolean previewPending = screen.previewBridge.previewRequestPending();
        String testLabel = screen.state.serverPreviewRunning() ? "Live" : previewPending ? "Sync" : "Test";
        int testColor = screen.state.serverPreviewRunning() ? screen.UI_GREEN : previewPending ? screen.UI_WARN : screen.UI_ACCENT;
        drawTransportButton(context, transportButtonX(x, width, 2), transportY, buttonWidth, testLabel, testColor);
        drawTransportButton(context, transportButtonX(x, width, 3), transportY, buttonWidth, "Stop", screen.UI_WARN);
        boolean autoMode = screen.state.previewMode() == cn.frkovo.rhythmcv2.rmcChart.client.net.ChartPreviewChannel.PREVIEW_MODE_AUTO;
        drawTransportButton(context, transportButtonX(x, width, 4), transportY, buttonWidth, autoMode ? "Auto" : "Judge", autoMode ? screen.UI_ACCENT : screen.UI_WARN);
    }

    void drawTransportButton(DrawContext context, int x, int y, int width, String label, int color) {
        context.fill(x, y, x + width, y + 18, 0x66303A44);
        drawOutline(context, x, y, width, 18, color);
        context.drawCenteredTextWithShadow(screen.getTextRenderer(), Text.literal(label), x + width / 2, y + 5, color);
    }

    void drawPropertyPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, screen.UI_PANEL);
        drawOutline(context, x, y, width, height, screen.UI_BORDER);
        context.drawText(screen.getTextRenderer(), Text.literal("Inspector"), x + 10, y + 9, screen.UI_ACCENT, false);
        drawTrimmedText(context, screen.propertyPanel.propertyTitle().getString(), x + 10, y + 21, width - 20, screen.UI_TEXT);
        drawTrimmedText(context, screen.propertyPanel.selectedObjectSummary(), x + 10, y + 33, width - 20, screen.UI_MUTED);
        context.enableScissor(x + 1, y + 47, x + width - 1, y + height - 42);
        for (LabeledField field : screen.propertyPanel.activeFieldGroup()) {
            if (field.sectionY >= 0 && field.section != null && !field.section.isBlank()) {
                drawPropertySectionHeader(context, screen.propertyPanel.propertySectionKey(screen.state.selection().kind(), field.section), field.section, x + 8, field.sectionY, width - 16);
            }
            if (field.widget.visible) {
                context.drawText(screen.getTextRenderer(), Text.literal(screen.propertyPanel.displayLabel(field)), field.widget.getX(), field.widget.getY() - 18, screen.UI_TEXT, false);
                if (field.hint != null && !field.hint.isBlank()) {
                    drawTrimmedText(context, field.hint, field.widget.getX(), field.widget.getY() - 9, screen.rightPanelInnerWidth(), screen.UI_DIM);
                }
            }
        }
        drawTrackEventEditors(context);
        screen.propertyPanel.drawNoteTransformCard(context);
        context.disableScissor();
        context.fill(x + 1, y + height - 41, x + width - 1, y + height - 40, screen.UI_BORDER_SOFT);
        drawTrimmedText(context, "Scroll position " + Math.abs(screen.propertyPanel.propertyScroll), x + 10, y + height - 18, width - 20, screen.UI_DIM);
    }

    void drawPropertySectionHeader(DrawContext context, String key, String title, int x, int y, int width) {
        boolean collapsed = screen.propertyPanel.isSectionCollapsed(key);
        context.drawText(screen.getTextRenderer(), Text.literal((collapsed ? "+ " : "- ") + title), x, y, 0x8FD6FF, false);
        context.fill(x + 64, y + 5, x + width, y + 6, 0x335F86A1);
    }

    void drawTrackEventEditors(DrawContext context) {
        if (screen.state.selection().kind() != EditorSelection.Kind.TRACK) {
            return;
        }
        for (TrackEventEditor editor : screen.propertyPanel.trackEvents.trackEventEditors) {
            if (editor.sectionY >= 0) {
                drawPropertySectionHeader(context, screen.propertyPanel.propertySectionKey(EditorSelection.Kind.TRACK, editor.section()), editor.section(), editor.baseX, editor.sectionY, screen.rightPanelInnerWidth());
            }
            if (editor.titleY < 0) {
                continue;
            }
            int titleX = editor.baseX;
            TrackData track = screen.state.selection().track();
            int eventCount = track == null ? 0 : screen.eventsForLane(track, editor.eventType()).size();
            context.drawText(screen.getTextRenderer(), Text.literal(editor.eventType().label), titleX, editor.titleY, editor.eventType().color, false);
            String label = AutomationCardUi.subtitle();
            context.drawText(screen.getTextRenderer(), Text.literal(label), titleX, editor.columnsY, eventCount > 1 ? screen.UI_WARN : 0x7F97A8, false);
            for (int index = 0; index < editor.rows.size(); index++) {
                TrackEventRow row = editor.rows.get(index);
                if (!row.visible) {
                    continue;
                }
                AutomationCardUi.drawCardBackground(context, row.dragX, row.y, row.rowRight);
            }
        }
    }

    void drawTrackEventEasingPopup(DrawContext context, int mouseX, int mouseY) {
        if (screen.propertyPanel.trackEvents.easingPopupRow == null) {
            return;
        }
        EasingPopupLayout layout = screen.propertyPanel.trackEvents.buildEasingPopupLayout();
        context.getMatrices().pushMatrix();
        context.fill(screen.propertyPanel.trackEvents.easingPopupX, screen.propertyPanel.trackEvents.easingPopupY, screen.propertyPanel.trackEvents.easingPopupX + layout.width(), screen.propertyPanel.trackEvents.easingPopupY + layout.height(), 0xEE182129);
        drawOutline(context, screen.propertyPanel.trackEvents.easingPopupX, screen.propertyPanel.trackEvents.easingPopupY, layout.width(), layout.height(), 0xFF6B93B0);
        screen.propertyPanel.trackEvents.easingPopupSearchField.render(context, mouseX, mouseY, 0.0f);
        for (EasingPopupGroup group : layout.groups()) {
            context.drawText(screen.getTextRenderer(), Text.literal(group.title()), screen.propertyPanel.trackEvents.easingPopupX + 6, group.y(), 0x8FD6FF, false);
            context.fill(screen.propertyPanel.trackEvents.easingPopupX + 62, group.y() + 5, screen.propertyPanel.trackEvents.easingPopupX + layout.width() - 6, group.y() + 6, 0x335F86A1);
        }
        for (EasingPopupEntry entry : layout.entries()) {
            boolean hovered = mouseX >= entry.x() && mouseX < entry.x() + entry.width() && mouseY >= entry.y() && mouseY < entry.y() + entry.height();
            boolean selected = screen.propertyPanel.trackEvents.easingPopupRow.easingType == entry.easingType();
            if (hovered || selected) {
                context.fill(entry.x() - 1, entry.y(), entry.x() + entry.width(), entry.y() + entry.height(), selected ? 0x665AA8D8 : 0x334B6478);
            }
            context.drawText(screen.getTextRenderer(), Text.literal(entry.label()), entry.x(), entry.y() + 2, selected ? 0xFFFFFF : 0xC7DAE7, false);
        }
        if (layout.entries().isEmpty()) {
            context.drawText(screen.getTextRenderer(), Text.literal("No easing matches"), screen.propertyPanel.trackEvents.easingPopupX + 8, screen.propertyPanel.trackEvents.easingPopupSearchField.getY() + 26, 0xC7DAE7, false);
        }
        context.getMatrices().popMatrix();
    }

    void drawTrimmedText(DrawContext context, String text, int x, int y, int maxWidth, int color) {
        String display = trimToWidth(text == null ? "" : text, maxWidth);
        context.drawText(screen.getTextRenderer(), Text.literal(display), x, y, color, false);
    }

    String trimToWidth(String text, int maxWidth) {
        if (screen.getTextRenderer().getWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = screen.getTextRenderer().getWidth(suffix);
        int length = text.length();
        while (length > 0 && screen.getTextRenderer().getWidth(text.substring(0, length)) + suffixWidth > maxWidth) {
            length--;
        }
        return text.substring(0, Math.max(0, length)) + suffix;
    }

    private void drawStatLine(DrawContext context, int x, int y, String label, String value, int color) {
        context.drawText(screen.getTextRenderer(), Text.literal(label), x, y, screen.UI_MUTED, false);
        context.drawText(screen.getTextRenderer(), Text.literal(value), x + 58, y, color, false);
    }

    void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
    }

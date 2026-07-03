package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.Vec3Data;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Locale;

import com.google.gson.JsonObject;

public class EditorChrome {
    private final ChartEditorScreen screen;

    public EditorChrome(ChartEditorScreen screen) {
        this.screen = screen;
    }

    void drawEditorChrome(DrawContext context) {
        context.fill(0, 0, screen.publicWidth(), screen.publicHeight(), screen.UI_BG);
        context.fill(0, 0, screen.publicWidth(), screen.TOP_BAR_HEIGHT - 1, screen.UI_TOP_BG);
        context.fill(0, screen.TOP_BAR_HEIGHT - 1, screen.publicWidth(), screen.TOP_BAR_HEIGHT, screen.UI_BORDER);
        context.drawText(screen.getTextRenderer(), Text.literal("RhythMC Chart Maker"), screen.OUTER_PADDING, 10, screen.UI_TEXT, false);
        context.drawText(screen.getTextRenderer(), Text.literal(screen.state.project().manifest().name()), Math.max(112, screen.publicWidth() - 270), 10, screen.UI_MUTED, false);
        int statusPillX = Math.max(112, screen.publicWidth() - 446);
        drawPill(context, statusPillX, 34, 88, 18, previewHandshakeLabel(), previewHandshakeColor());
        statusPillX += 92;
        drawPill(context, statusPillX, 34, 84, 18, projectSaveLabel(), screen.state.projectDirty() ? screen.UI_WARN : screen.UI_GREEN);
        statusPillX += 88;
        drawPill(context, statusPillX, 34, 88, 18, previewChartLabel(), previewChartColor());
        statusPillX += 92;
        drawPill(context, statusPillX, 34, 86, 18, screen.state.serverPreviewRunning() ? "Srv Live" : "Srv Idle", screen.state.serverPreviewRunning() ? screen.UI_GREEN : screen.UI_DIM);
        statusPillX += 90;
        drawPill(context, statusPillX, 34, 86, 18, previewAssetsLabel(), previewAssetsColor());
        int pillX = Math.max(112, screen.publicWidth() - 430);
        drawPill(context, pillX, 58, 82, 18, "In " + screen.propertyPanel.format(screen.state.playbackStartBeat()), screen.UI_ACCENT);
        pillX += 86;
        drawPill(context, pillX, 58, 86, 18, screen.state.hasPlaybackEndBeat() ? "Out " + screen.propertyPanel.format(screen.state.playbackEndBeat()) : "Out --", screen.UI_WARN);
        pillX += 90;
        drawPill(context, pillX, 58, 74, 18, screen.state.showOnlySelectedTrack() ? "1 Track" : "All Tracks", screen.state.showOnlySelectedTrack() ? screen.UI_ACCENT : screen.UI_DIM);
        pillX += 78;
        drawPill(context, pillX, 58, 76, 18, screen.state.playing() ? "Playing" : "Paused", screen.state.playing() ? screen.UI_GREEN : screen.UI_DIM);
        pillX += 80;
        drawPill(context, pillX, 58, 80, 18, screen.propertyPanel.format(screen.state.playheadBeat()), screen.UI_WARN);
    }

    void drawStatusBar(DrawContext context) {
        int y = screen.publicHeight() - screen.STATUS_BAR_HEIGHT;
        context.fill(0, y, screen.publicWidth(), screen.publicHeight(), 0xF0161C22);
        context.fill(0, y, screen.publicWidth(), y + 1, screen.UI_BORDER_SOFT);
        String hint = statusHint();
        int hintWidth = Math.min(520, screen.getTextRenderer().getWidth(hint) + 8);
        drawTrimmedText(context, screen.state.statusMessage(), screen.OUTER_PADDING, y + 6, Math.max(80, screen.publicWidth() - hintWidth - screen.OUTER_PADDING * 3), screen.UI_TEXT);
        drawTrimmedText(context, hint, Math.max(screen.OUTER_PADDING, screen.publicWidth() - hintWidth - screen.OUTER_PADDING), y + 6, hintWidth, screen.UI_MUTED);
    }

    private String statusHint() {
        MinecraftClient client = screen.getClient();
        if (client != null && RmcChartClient.getWorldLauncher().isEditorWorldActive(client)) {
            String mode = RmcChartClient.getWorldLauncher().playbackEngine().isAutoPlay() ? "AutoPlay on" : "AutoPlay off";
            String scope = screen.state.showOnlySelectedTrack() && screen.state.selectedTrack() != null ? "Track " + screen.state.selectedTrack().id() : "All tracks";
            return "P start/pause | O stop | End close | F GUI | I AutoPlay | " + scope + " | " + mode;
        }
        if (screen.smoothing.smoothingPopupOpen) {
            return "Enter apply | Esc close | Fill uses 1/x division";
        }
        if (screen.selectionManager.hasSelectedNotes()) {
            return "Ctrl+Click/drag multi-select | Ctrl+3 smoothing | Alt+Arrows move";
        }
        return "Ctrl+N/O/S | 1/2/3/4 create | Ctrl+A/C/X/V/D | Del/Backspace del | Home/End jump | PgUp/PgDn scroll | +/- zoom | Space play/pause";
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

    int transportButtonX(int x) {
        return x + 10;
    }

    int previewShowButtonX(int x, int width) {
        return x + width - 106;
    }

    int previewStopButtonX(int x, int width) {
        return x + width - 48;
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
        context.drawText(screen.getTextRenderer(), Text.literal("Scene Map"), x + 10, y + 9, screen.UI_TEXT, false);
        context.drawText(screen.getTextRenderer(), Text.literal("Beat " + screen.propertyPanel.format(screen.state.playheadBeat())), x + 88, y + 9, screen.UI_MUTED, false);
        drawTrimmedText(context, "Drag selected note/effect for X/Y", x + 10, y + 18, width - 20, screen.UI_DIM);

        int mapX = x + 10;
        int mapY = y + 28;
        int mapSize = width - 20;
        drawCurrentFrameMap(context, mapX, mapY, mapSize, mapSize);
        drawCurrentFrameMapEditorOverlay(context, mapX, mapY, mapSize, mapSize);

        int rowY = mapY + mapSize + 12;
        context.drawText(screen.getTextRenderer(), Text.literal("Tracks"), x + 10, rowY, screen.UI_TEXT, false);
        context.drawText(screen.getTextRenderer(), Text.literal(screen.state.activeDifficulty().displayName()), x + width - 74, rowY, screen.UI_ACCENT, false);
        rowY += 14;
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

    void drawCurrentFrameMap(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, screen.UI_PANEL_ALT);
        drawOutline(context, x, y, width, height, screen.UI_BORDER_SOFT);
        context.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);

        int centerX = x + width / 2;
        int centerY = y + height / 2;
        double beat = screen.state.playheadBeat();
        double baseScale = Math.max(6.0, width / 22.0);
        double scale = baseScale * Math.max(0.25, screen.sceneMapZoom);

        double visibleHalfWorld = (width / 2.0) / scale;
        double[] steps = EditorSceneMap.computeGridSteps(visibleHalfWorld);
        double majorStep = steps[0];
        double minorStep = steps[1];

        int firstMajorX = (int) Math.floor((-width / 2.0) / (majorStep * scale));
        int lastMajorX = (int) Math.ceil((width / 2.0) / (majorStep * scale));
        for (int i = firstMajorX; i <= lastMajorX; i++) {
            int sx = centerX + (int) Math.round(i * majorStep * scale);
            if (i == 0) continue;
            context.fill(sx, y + 1, sx + 1, y + height - 1, 0x305FBCD3);
            String label = formatGridLabel(i * majorStep);
            int labelWidth = screen.getTextRenderer().getWidth(label);
            if (sx - labelWidth / 2 >= x + 4 && sx + labelWidth / 2 <= x + width - 4) {
                context.drawText(screen.getTextRenderer(), Text.literal(label), sx - labelWidth / 2, centerY + 4, 0xFF6F8394, false);
            }
        }
        int firstMinorX = (int) Math.floor((-width / 2.0) / (minorStep * scale));
        int lastMinorX = (int) Math.ceil((width / 2.0) / (minorStep * scale));
        for (int i = firstMinorX; i <= lastMinorX; i++) {
            if (i % 5 == 0) continue;
            int sx = centerX + (int) Math.round(i * minorStep * scale);
            context.fill(sx, y + 1, sx + 1, y + height - 1, 0x155FBCD3);
        }
        int firstMajorY = (int) Math.floor((-height / 2.0) / (majorStep * scale));
        int lastMajorY = (int) Math.ceil((height / 2.0) / (majorStep * scale));
        for (int i = firstMajorY; i <= lastMajorY; i++) {
            int sy = centerY - (int) Math.round(i * majorStep * scale);
            if (i == 0) continue;
            context.fill(x + 1, sy, x + width - 1, sy + 1, 0x305FBCD3);
            String label = formatGridLabel(i * majorStep);
            int labelWidth = screen.getTextRenderer().getWidth(label);
            if (sy + 4 >= y + 4 && sy + 12 <= y + height - 4 && centerX + 4 + labelWidth <= x + width - 4) {
                context.drawText(screen.getTextRenderer(), Text.literal(label), centerX + 4, sy - 4, 0xFF6F8394, false);
            }
        }
        int firstMinorY = (int) Math.floor((-height / 2.0) / (minorStep * scale));
        int lastMinorY = (int) Math.ceil((height / 2.0) / (minorStep * scale));
        for (int i = firstMinorY; i <= lastMinorY; i++) {
            if (i % 5 == 0) continue;
            int sy = centerY - (int) Math.round(i * minorStep * scale);
            context.fill(x + 1, sy, x + width - 1, sy + 1, 0x155FBCD3);
        }

        context.fill(centerX, y + 1, centerX + 1, y + height - 1, 0xFF5FBCD3);
        context.fill(x + 1, centerY, x + width - 1, centerY + 1, 0xFF5FBCD3);
        context.drawText(screen.getTextRenderer(), Text.literal("Y"), centerX + 4, y + 6, screen.UI_DIM, false);
        context.drawText(screen.getTextRenderer(), Text.literal("X"), x + width - 12, centerY + 4, screen.UI_DIM, false);

        for (TrackData track : screen.state.visibleTracks()) {
            ChartEvaluator.TrackState trackState = ChartEvaluator.evaluateTrack(track, beat);

            int trackCenterX = centerX + (int) Math.round(trackState.xTransform() * scale);
            int trackCenterY = centerY - (int) Math.round(trackState.yTransform() * scale);
            context.fill(trackCenterX - 2, trackCenterY - 2, trackCenterX + 2, trackCenterY + 2, 0xFFFFFFFF);

            for (NoteData note : track.notes()) {
                ChartEvaluator.NoteState noteState = ChartEvaluator.evaluateNote(trackState, note);
                if (Math.abs(noteState.distanceToHitPlane()) > 24.0) {
                    continue;
                }

                int drawX = centerX + (int) Math.round(noteState.worldX() * scale);
                int drawY = centerY - (int) Math.round(noteState.worldY() * scale);
                int color = note == screen.state.selection().note() ? 0xFFFFFFFF : EditorUtils.noteColor(note.noteType());
                context.fill(drawX - 3, drawY - 3, drawX + 3, drawY + 3, color);
            }
        }

        for (EffectData effect : screen.state.triggeredEffects()) {
            JsonObject properties = effect.properties();
            double fx = screen.propertyPanel.getDouble(properties, "x", 0.0);
            double fy = screen.propertyPanel.getDouble(properties, "y", 0.0);
            int drawX = centerX + (int) Math.round(fx * scale);
            int drawY = centerY - (int) Math.round(fy * scale);
            int color = effect == screen.state.selection().effect() ? 0xFFFFF08A : 0xFFFFA040;
            context.fill(drawX - 2, drawY - 2, drawX + 2, drawY + 2, color);
            context.drawText(screen.getTextRenderer(), Text.literal(screen.propertyPanel.shortEffectLabel(effect.effectType())), drawX + 4, drawY - 4, color, false);
        }
        context.disableScissor();
    }

    private String formatGridLabel(double value) {
        if (Math.abs(value) >= 1000.0) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        if (Math.abs(value) >= 10.0 || value == Math.rint(value)) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    void drawCurrentFrameMapEditorOverlay(DrawContext context, int x, int y, int width, int height) {
        SceneMapOverlayUi.Model overlayModel = screen.sceneMap.buildSceneMapOverlayModel(x, y, width, height);
        if (overlayModel == null) {
            return;
        }
        SceneMapOverlayUi.draw(context, screen.getTextRenderer(), overlayModel, 0xC8161C24, screen.UI_BORDER_SOFT, screen.UI_TEXT, screen.UI_DIM);
    }

    void drawPreviewPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, screen.UI_PANEL);
        drawOutline(context, x, y, width, height, screen.UI_BORDER);

        int transportX = x + 10;
        int transportY = y + 6;
        drawTransportButton(context, transportX, transportY, 52, screen.state.playing() ? "Stop" : "Play", screen.state.playing() ? screen.UI_WARN : screen.UI_GREEN);
        drawTransportButton(context, transportX + 58, transportY, 52, "Restart", screen.UI_ACCENT);
        drawTransportButton(context, transportX + 116, transportY, 64, screen.state.serverPreviewRunning() ? "Live" : "Preview", screen.state.serverPreviewRunning() ? screen.UI_GREEN : screen.UI_ACCENT);
        drawTransportButton(context, transportX + 186, transportY, 46, "Stop", screen.UI_WARN);

        int textY = y + 10;
        int infoX = x + width - 260;
        context.drawText(screen.getTextRenderer(), Text.literal(screen.propertyPanel.format(screen.state.playheadBeat()) + " / " + (screen.state.hasPlaybackEndBeat() ? screen.propertyPanel.format(screen.state.playbackEndBeat()) : "--") + " beats"), infoX, textY, screen.UI_TEXT, false);
        textY += 12;
        context.drawText(screen.getTextRenderer(), Text.literal(screen.propertyPanel.stateAudioMillis() + " / " + screen.propertyPanel.audioSummary()), infoX, textY, screen.UI_MUTED, false);

        int contentX = x + 10;
        int contentY = y + 32;
        int contentWidth = width - 20;
        int contentHeight = Math.max(24, height - 40);
        context.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, 0xB512171D);
        drawOutline(context, contentX, contentY, contentWidth, contentHeight, 0x665FBCD3);

        int statusY = contentY + 5;
        drawTrimmedText(context, "Server: " + previewHandshakeLabel() + "  |  Chart: " + previewChartLabel() + "  |  " + (screen.state.serverPreviewRunning() ? "Preview live" : "Preview idle"), contentX + 10, statusY, contentWidth - 20, previewChartColor());
        statusY += 12;
        drawTrimmedText(context, "Range: " + screen.propertyPanel.format(screen.state.playbackStartBeat()) + " -> " + (screen.state.hasPlaybackEndBeat() ? screen.propertyPanel.format(screen.state.playbackEndBeat()) : "--") + "  |  BPM: " + screen.propertyPanel.format(screen.state.timing().bpmAt(screen.state.playheadBeat())), contentX + 10, statusY, contentWidth - 20, screen.UI_MUTED);
    }

    void drawTransportButton(DrawContext context, int x, int y, int width, String label, int color) {
        context.fill(x, y, x + width, y + 18, 0x66303A44);
        drawOutline(context, x, y, width, 18, color);
        context.drawCenteredTextWithShadow(screen.getTextRenderer(), Text.literal(label), x + width / 2, y + 5, color);
    }

    void drawPreviewDepthBand(DrawContext context, int x, int y, int width, int height) {
        int bottom = y + height - 18;
        int top = y + 16;
        for (int index = 0; index < 5; index++) {
            double progress = index / 4.0;
            int bandY = bottom - (int) Math.round(progress * (bottom - top));
            int bandWidth = (int) Math.round(width * (0.88 - progress * 0.42));
            int left = x + width / 2 - bandWidth / 2;
            int color = index == 0 ? 0x558FE1B2 : 0x225FBCD3;
            context.fill(left, bandY, left + bandWidth, bandY + 1, color);
            if (index > 0) {
                drawTrimmedText(context, "+" + index + " beat", left + 4, bandY - 8, 54, screen.UI_DIM);
            }
        }
    }

    void drawPreviewNoteGlyph(DrawContext context, NoteData note, int x, int y, int size, double depth) {
        int color = note == screen.state.selection().note() ? 0xFFFFFFFF : EditorUtils.noteColor(note.noteType());
        int dimColor = (color & 0x00FFFFFF) | (depth > 0.65 ? 0xAA000000 : 0xDD000000);
        if (note.noteType() == NoteType.HOLD) {
            int tail = Math.max(size + 4, (int) Math.round(note.holdLengthBeats() * 10.0));
            context.fill(x - 2, y, x + 2, y + tail, 0x9926A69A);
        }
        context.fill(x - size / 2, y - size / 2, x + size / 2, y + size / 2, dimColor);
        drawOutline(context, x - size / 2, y - size / 2, size, size, color);
        if (note.noteType() == NoteType.DODGE) {
            drawLine(context, x - size / 2, y - size / 2, x + size / 2, y + size / 2, 0xFFFFFFFF, 1);
            drawLine(context, x + size / 2, y - size / 2, x - size / 2, y + size / 2, 0xFFFFFFFF, 1);
        } else if (note.noteType() == NoteType.LOOK) {
            context.fill(x - size / 2 - 2, y, x + size / 2 + 2, y + 1, 0xFFFFFFFF);
            context.fill(x, y - size / 2 - 2, x + 1, y + size / 2 + 2, 0xFFFFFFFF);
        }
        drawTrimmedText(context, EditorUtils.shortNoteLabel(note.noteType()), x + size / 2 + 3, y - 4, 34, color);
    }

    void drawSchematicEditorOverlay(DrawContext context, int x, int y, int width, int height) {
        TrackData track = screen.state.selectedTrack();
        if (track == null || width < 80 || height < 64) {
            return;
        }
        context.fill(x, y, x + width, y + height, 0xB8141A20);
        drawOutline(context, x, y, width, height, screen.UI_BORDER_SOFT);
        context.drawText(screen.getTextRenderer(), Text.literal("Schematic"), x + 6, y + 5, screen.UI_TEXT, false);
        context.drawText(screen.getTextRenderer(), Text.literal("Track " + track.id()), x + width - 48, y + 5, screen.UI_MUTED, false);

        int gridX = x + 8;
        int gridY = y + 20;
        int gridWidth = width - 16;
        int gridHeight = height - 28;
        int cellWidth = Math.max(1, gridWidth / 3);
        int cellHeight = Math.max(1, gridHeight / 4);
        for (int gx = 0; gx < 3; gx++) {
            for (int gy = 0; gy < 4; gy++) {
                int left = gridX + gx * cellWidth;
                int top = gridY + (3 - gy) * cellHeight;
                context.fill(left, top, left + cellWidth - 1, top + cellHeight - 1, 0x55303A44);
                drawOutline(context, left, top, cellWidth - 1, cellHeight - 1, 0x335FBCD3);
            }
        }
        int centerX = gridX + cellWidth + cellWidth / 2;
        int centerY = gridY + cellHeight + cellHeight / 2;
        context.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, screen.UI_ACCENT);

        double currentBeat = screen.state.playheadBeat();
        double window = Math.max(2.0, screen.state.beatsPerScreen() / 8.0);
        for (NoteData note : track.notes()) {
            double delta = Math.abs(note.beat() - currentBeat);
            if (delta > window) {
                continue;
            }
            int cellX = EditorUtils.clamp((int) Math.round(note.pos().x()) + 1, 0, 2);
            int cellY = EditorUtils.clamp((int) Math.round(note.pos().y()) + 1, 0, 3);
            int drawX = gridX + cellX * cellWidth + cellWidth / 2;
            int drawY = gridY + (3 - cellY) * cellHeight + cellHeight / 2;
            int size = note == screen.state.selection().note() ? 5 : 3;
            int color = EditorUtils.noteColor(note.noteType());
            context.fill(drawX - size, drawY - size, drawX + size, drawY + size, color);
        }
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

    void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color, int thickness) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            context.fill(x1, y1, x1 + thickness, y1 + thickness, color);
            return;
        }
        int half = Math.max(0, thickness / 2);
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            context.fill(x - half, y - half, x + half + 1, y + half + 1, color);
        }
    }
}

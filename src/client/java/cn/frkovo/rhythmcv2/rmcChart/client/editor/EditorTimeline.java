package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.core.EasingFunctions;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.Vec3Data;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.EditorSelection;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.DragMode;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.EditorLayout;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.EventLaneType;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.LaneType;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.TimelineLane;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.SelectedEventHandle;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.TimelineLaneLayout;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;

public final class EditorTimeline {
    static final int TIMELINE_TOGGLE_WIDTH = 68;

    final ChartEditorScreen screen;

    public EditorTimeline(ChartEditorScreen screen) {
        this.screen = screen;
    }

    void drawTimeline(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, screen.UI_PANEL);
        screen.chrome.drawOutline(context, x, y, width, height, screen.UI_BORDER);

        EditorLayout editorLayout = screen.editorLayout();
        int contentX = editorLayout.timelineContentX();
        int contentWidth = editorLayout.timelineContentWidth();
        int contentY = y + screen.TIMELINE_RULER_HEIGHT;
        int audioY = y + height - screen.TIMELINE_AUDIO_STRIP_HEIGHT;
        int contentHeight = Math.max(48, audioY - contentY);
        int contentBottom = contentY + contentHeight;
        context.fill(x, y, x + width, contentY, screen.UI_PANEL_STRONG);
        context.fill(x, contentY, contentX, contentBottom, screen.UI_PANEL_ALT);
        context.fill(x, audioY, x + width, y + height, screen.UI_PANEL_STRONG);
        context.fill(contentX, y, contentX + 1, y + height, screen.UI_BORDER);
        screen.chrome.drawOutline(context, contentX, y, contentWidth, screen.TIMELINE_RULER_HEIGHT, screen.UI_BORDER_SOFT);
        screen.chrome.drawOutline(context, x, audioY, width, screen.TIMELINE_AUDIO_STRIP_HEIGHT, screen.UI_BORDER_SOFT);

        drawTimelineRuler(context, x, y, contentX, contentWidth);
        context.enableScissor(contentX + 1, contentY, contentX + contentWidth - 1, contentBottom);
        drawTimelineGrid(context, contentX, contentY, contentWidth, contentHeight);
        context.disableScissor();
        drawAudioStrip(context, x, audioY, width, screen.TIMELINE_AUDIO_STRIP_HEIGHT, contentX, contentWidth);

        List<TimelineLaneLayout> layouts = buildTimelineLaneLayouts(contentY);
        for (int index = 0; index < layouts.size(); index++) {
            TimelineLaneLayout layout = layouts.get(index);
            int laneTop = layout.top();
            int laneBottom = laneTop + layout.height() - 1;
            if (laneTop >= contentBottom || laneBottom < contentY) {
                if (laneTop >= contentBottom) break;
                continue;
            }
            laneTop = Math.max(laneTop, contentY);
            laneBottom = Math.min(laneBottom, contentBottom);
            int fill = index % 2 == 0 ? 0x331F1F1F : 0x33272727;
            int gutterFill = index % 2 == 0 ? 0xCC1B232D : 0xCC202A35;
            context.fill(x, laneTop, x + width, laneBottom, fill);
            context.fill(x, laneTop, contentX, laneBottom, gutterFill);
        }

        context.enableScissor(x + 1, contentY, x + width - 1, contentBottom);
        for (TimelineLaneLayout layout : layouts) {
            int laneTop = layout.top();
            int laneBottom = laneTop + layout.height() - 1;
            if (laneTop >= contentBottom || laneBottom < contentY) {
                if (laneTop >= contentBottom) break;
                continue;
            }
            drawTimelineLane(context, layout, x, contentX, contentWidth);
        }
        context.disableScissor();

        if (Double.isFinite(screen.snapGuideBeat)) {
            int snapX = screen.beatToScreen(contentX, contentWidth, screen.snapGuideBeat);
            context.enableScissor(contentX + 1, y, contentX + contentWidth - 1, contentBottom);
            context.fill(snapX, y, snapX + 1, contentBottom, 0xFF7CE8FF);
            context.disableScissor();
        }

        if (screen.selectionBox != null) {
            int left = Math.max(contentX, Math.min(screen.selectionBox.startX(), screen.selectionBox.endX()));
            int right = Math.min(contentX + contentWidth, Math.max(screen.selectionBox.startX(), screen.selectionBox.endX()));
            int top = Math.max(contentY, Math.min(screen.selectionBox.startY(), screen.selectionBox.endY()));
            int bottom = Math.min(contentBottom, Math.max(screen.selectionBox.startY(), screen.selectionBox.endY()));
            if (right > left && bottom > top) {
                context.fill(left, top, right, bottom, 0x224FC3F7);
                screen.chrome.drawOutline(context, left, top, right - left, bottom - top, 0xFF7CE8FF);
            }
        }

        int playheadX = screen.beatToScreen(contentX, contentWidth, screen.state.playheadBeat());
        context.enableScissor(contentX + 1, y, contentX + contentWidth - 1, y + height - 1);
        context.fill(playheadX, y, playheadX + 2, y + height, 0xFFFF4444);
        context.disableScissor();
        context.drawText(screen.getTextRenderer(), Text.literal("Timeline"), x + 10, y + 7, screen.UI_TEXT, false);
    }

    private void drawTimelineRuler(DrawContext context, int x, int y, int contentX, int contentWidth) {
        TrackData rulerTrack = screen.state.selectedTrack();
        double trackStep = rulerTrack == null ? timelineGridStep() : trackGridStep(rulerTrack);
        double gridStep = rulerTrack != null && contentWidth * trackStep / Math.max(1.0, screen.state.beatsPerScreen()) >= 28.0
                ? trackStep
                : timelineGridStep();
        double startBeat = Math.floor(screen.state.visibleStartBeat() / gridStep) * gridStep;
        double endBeat = screen.state.visibleEndBeat() + gridStep;
        int previousLabelX = Integer.MIN_VALUE;
        context.enableScissor(contentX + 1, y, contentX + contentWidth - 1, y + screen.TIMELINE_RULER_HEIGHT);
        for (double beat = startBeat; beat <= endBeat; beat += gridStep) {
            int lineX = screen.beatToScreen(contentX, contentWidth, beat);
            int tickHeight = isMeasureBeat(beat) ? screen.TIMELINE_RULER_HEIGHT - 8 : isWholeBeat(beat) ? screen.TIMELINE_RULER_HEIGHT - 10 : screen.TIMELINE_RULER_HEIGHT - 12;
            int tickColor = isMeasureBeat(beat) ? 0xC4FFFFFF : isWholeBeat(beat) ? 0x88CDE3F6 : 0x44678096;
            context.fill(lineX, y + screen.TIMELINE_RULER_HEIGHT - tickHeight, lineX + 1, y + screen.TIMELINE_RULER_HEIGHT, tickColor);
            if (lineX - previousLabelX > 32 && (isWholeBeat(beat) || rulerTrack != null)) {
                String label = rulerTrack == null || isWholeBeat(beat) ? beatNumberLabel(beat) : subdivisionLabel(beat, rulerTrack);
                int labelColor = isMeasureBeat(beat) ? 0xFFFFFFFF : isWholeBeat(beat) ? 0xE7F4FF : 0x8EAABD;
                context.drawText(screen.getTextRenderer(), Text.literal(label), lineX + 3, y + 3, labelColor, false);
                previousLabelX = lineX;
            }
        }
        for (BpmPoint bpm : screen.state.level().meta().bpms()) {
            int markerX = screen.beatToScreen(contentX, contentWidth, bpm.beat());
            context.fill(markerX, y, markerX + 1, y + screen.TIMELINE_RULER_HEIGHT, 0xFF5DE1FF);
            int labelX = EditorUtils.clamp(markerX + 4, contentX + 4, contentX + Math.max(4, contentWidth - 64));
            context.drawText(screen.getTextRenderer(), Text.literal(format(bpm.bpm()) + " BPM"), labelX, y + 12, 0xFF8EEFFF, false);
        }
        context.disableScissor();
        int zoomBarWidth = zoomBarWidth(contentWidth);
        int zoomBarX = zoomBarX(contentX, contentWidth);
        int zoomBarY = y + 7;
        context.drawText(screen.getTextRenderer(), Text.literal("Zoom"), zoomBarX - 32, y + 4, 0x9BC1D7, false);
        context.fill(zoomBarX, zoomBarY, zoomBarX + zoomBarWidth, zoomBarY + screen.TIMELINE_ZOOM_BAR_HEIGHT, 0x66354757);
        int handleX = zoomBarX + (int) Math.round(((screen.state.beatsPerScreen() - 4.0) / 124.0) * Math.max(1, zoomBarWidth - 8));
        context.fill(handleX, zoomBarY - 2, handleX + 8, zoomBarY + screen.TIMELINE_ZOOM_BAR_HEIGHT + 2, 0xFF9AD9FF);
        context.drawText(screen.getTextRenderer(), Text.literal(format(screen.state.beatsPerScreen()) + " beats"), zoomBarX - 88, y + 4, 0xCBE8F8, false);
    }

    private void drawTimelineGrid(DrawContext context, int contentX, int contentY, int contentWidth, int contentHeight) {
        double gridStep = timelineGridStep();
        double startBeat = Math.floor(screen.state.visibleStartBeat() / gridStep) * gridStep;
        double endBeat = screen.state.visibleEndBeat() + gridStep;
        for (double beat = startBeat; beat <= endBeat; beat += gridStep) {
            int lineX = screen.beatToScreen(contentX, contentWidth, beat);
            int color = isMeasureBeat(beat) ? 0x48FFFFFF : isWholeBeat(beat) ? 0x245D7687 : 0x16303D47;
            context.fill(lineX, contentY, lineX + 1, contentY + contentHeight, color);
        }
        for (BpmPoint bpm : screen.state.level().meta().bpms()) {
            int markerX = screen.beatToScreen(contentX, contentWidth, bpm.beat());
            context.fill(markerX, contentY, markerX + 1, contentY + contentHeight, 0x7F5DE1FF);
        }
    }

    private double timelineGridStep() {
        if (screen.state.beatsPerScreen() <= 2.0) {
            return 1.0 / 16.0;
        }
        if (screen.state.beatsPerScreen() <= 4.0) {
            return 1.0 / 8.0;
        }
        if (screen.state.beatsPerScreen() <= 8.0) {
            return 1.0 / 4.0;
        }
        if (screen.state.beatsPerScreen() <= 16.0) {
            return 1.0 / 2.0;
        }
        if (screen.state.beatsPerScreen() <= 32.0) {
            return 1.0;
        }
        if (screen.state.beatsPerScreen() <= 64.0) {
            return 2.0;
        }
        return 4.0;
    }

    double trackGridStep(TrackData track) {
        int division = track == null ? 16 : track.beatDivision();
        return Math.max(1.0 / 64.0, 4.0 / Math.max(1, division));
    }

    double snapHoldLength(double length, TrackData track) {
        double step = trackGridStep(track);
        return Math.max(step, Math.round(Math.max(0.0, length) / step) * step);
    }

    private boolean isWholeBeat(double beat) {
        return Math.abs(beat - Math.rint(beat)) < 1.0E-6;
    }

    private boolean isMeasureBeat(double beat) {
        return isWholeBeat(beat) && Math.floorMod((int) Math.round(beat), 4) == 0;
    }

    private String rulerBeatLabel(double beat) {
        int wholeBeat = (int) Math.round(beat);
        int measure = Math.floorDiv(wholeBeat, 4);
        int beatInMeasure = Math.floorMod(wholeBeat, 4) + 1;
        return measure + "." + beatInMeasure;
    }

    String beatNumberLabel(double beat) {
        int wholeBeat = (int) Math.round(beat);
        return Integer.toString(wholeBeat);
    }

    String subdivisionLabel(double beat, TrackData track) {
        int beatIndex = (int) Math.floor(beat);
        double fraction = beat - beatIndex;
        int division = track == null ? 16 : track.beatDivision();
        int partsPerBeat = Math.max(1, division / 4);
        int part = clamp((int) Math.round(fraction * partsPerBeat), 0, partsPerBeat - 1) + 1;
        return "B" + beatIndex + " " + part + "/" + partsPerBeat;
    }

    private void drawWaveform(DrawContext context, int x, int y, int width, int height) {
        if (!screen.state.audioAnalysis().hasWaveform()) {
            return;
        }

        float[] waveform = screen.state.audioAnalysis().waveform();
        long lengthMillis = screen.state.audioAnalysis().lengthMillis();
        int centerY = y + height / 2;
        double visibleStart = screen.state.visibleStartBeat();
        double visibleEnd = screen.state.visibleEndBeat();

        for (int px = 0; px < width; px++) {
            double beat = screen.screenToBeat(x, width, x + px);
            if (beat < visibleStart - 0.25 || beat > visibleEnd + 0.25) {
                continue;
            }
            long timeMillis = screen.state.timing().beatToMillis(beat);
            if (timeMillis < 0 || timeMillis > lengthMillis || lengthMillis <= 0) {
                continue;
            }
            double t = timeMillis / (double) lengthMillis;
            int idx = (int) Math.round(t * (waveform.length - 1));
            idx = Math.max(0, Math.min(waveform.length - 1, idx));
            int amplitude = Math.max(1, Math.round(waveform[idx] * (height / 2.6f)));
            context.fill(x + px, centerY - amplitude, x + px + 1, centerY + amplitude, 0x3355CC88);
        }
    }

    private void drawAudioStrip(DrawContext context, int x, int y, int width, int height, int contentX, int contentWidth) {
        context.fill(x, y, contentX, y + height, 0xE019232C);
        context.drawText(screen.getTextRenderer(), Text.literal("Audio"), x + 10, y + 8, screen.UI_TEXT, false);
        screen.chrome.drawTrimmedText(context, screen.propertyPanel.audioSummary(), x + 10, y + 22,
                Math.max(24, contentX - x - 18), screen.UI_MUTED);
        context.fill(contentX, y, contentX + contentWidth, y + height, 0xD3131A20);
        context.enableScissor(contentX + 1, y + 1, contentX + contentWidth - 1, y + height - 1);
        drawTimelineGrid(context, contentX, y, contentWidth, height);
        drawWaveform(context, contentX, y + 3, contentWidth, height - 6);
        context.disableScissor();
    }

    void drawTrackBar(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, screen.UI_PANEL_STRONG);
        screen.chrome.drawOutline(context, x, y, width, height, screen.UI_BORDER);

        int toggleX = timelineToggleX(x);
        int toggleColor = screen.timelineFocused() ? 0xAA2F5F9D : 0x77303A44;
        context.fill(toggleX, y + 4, toggleX + TIMELINE_TOGGLE_WIDTH, y + height - 4, toggleColor);
        context.drawText(screen.getTextRenderer(),
                Text.literal(screen.timelineFocused() ? "Timeline -" : "Timeline +"),
                toggleX + 6, y + 10, 0xFFFFFFFF, false);

        int addWidth = 70;
        int addX = x + width - addWidth - 6;
        int cursorX = trackTabsX(x);
        for (TrackData track : screen.state.visibleTracks()) {
            int tabWidth = 88;
            if (cursorX + tabWidth > addX - 4) {
                break;
            }
            boolean selected = screen.state.selectedTrack() == track;
            int fill = selected ? 0xCC2F5F9D : 0x66303030;
            context.fill(cursorX, y + 4, cursorX + tabWidth, y + height - 4, fill);
            context.fill(cursorX + 4, y + 6, cursorX + 16, y + 18, 0x55000000);
            context.drawText(screen.getTextRenderer(), Text.literal(screen.state.isTrackExpanded(track) ? "-" : "+"), cursorX + 8, y + 10, 0xFFFFFFFF, false);
            context.drawText(screen.getTextRenderer(), Text.literal("TrackID " + track.id()), cursorX + 20, y + 10, 0xFFFFFFFF, false);
            cursorX += tabWidth + 4;
        }

        context.fill(addX, y + 4, x + width - 6, y + height - 4, 0x66557733);
        context.drawText(screen.getTextRenderer(), Text.literal("+ Track"), addX + 14, y + 10, 0xFFFFFFFF, false);
    }

    int timelineToggleX(int trackBarX) {
        return trackBarX + 6;
    }

    int trackTabsX(int trackBarX) {
        return timelineToggleX(trackBarX) + TIMELINE_TOGGLE_WIDTH + 4;
    }

    int countTrackEvents(TrackData track) {
        return track.speedEvents().size()
                + track.xTransformEvents().size() + track.yTransformEvents().size() + track.zTransformEvents().size()
                + track.xRotateEvents().size() + track.yRotateEvents().size() + track.zRotateEvents().size()
                + track.xScaleEvents().size() + track.yScaleEvents().size() + track.zScaleEvents().size();
    }

    private List<TimelineLane> buildTimelineLanes() {
        List<TimelineLane> lanes = new ArrayList<>();
        lanes.add(new TimelineLane(LaneType.BPM, null, null, null, null, null));
        for (TrackData track : screen.state.visibleTracks()) {
            lanes.add(new TimelineLane(LaneType.TRACK_HEADER, track, null, null, null, null));
            if (!screen.state.isTrackExpanded(track)) {
                continue;
            }
            lanes.add(new TimelineLane(LaneType.NOTES, track, null, null, null, null));
            for (String group : List.of("Speed", "Position", "Rotation", "Scale")) {
                lanes.add(new TimelineLane(LaneType.EVENT_GROUP_HEADER, track, null, null, null, group));
                if (screen.state.isTrackEventGroupExpanded(track, group)) {
                    for (EventLaneType eventType : eventTypesInGroup(group)) {
                        lanes.add(new TimelineLane(LaneType.EVENTS, track, null, eventType, null, null));
                    }
                }
            }
        }
        lanes.add(new TimelineLane(LaneType.FX_TRACK_HEADER, null, null, null, null, null));
        if (screen.state.isFxTrackExpanded()) {
            for (EffectData effect : screen.state.level().effects()) {
                lanes.add(new TimelineLane(LaneType.FX_EFFECT_CLIP, null, effect, null, null, null));
            }
        }
        return lanes;
    }

    private List<EventLaneType> eventTypesInGroup(String group) {
        return switch (group) {
            case "Speed" -> List.of(EventLaneType.SPEED);
            case "Position" -> List.of(EventLaneType.MOVE_X, EventLaneType.MOVE_Y, EventLaneType.MOVE_Z);
            case "Rotation" -> List.of(EventLaneType.ROT_X, EventLaneType.ROT_Y, EventLaneType.ROT_Z);
            case "Scale" -> List.of(EventLaneType.SCALE_X, EventLaneType.SCALE_Y, EventLaneType.SCALE_Z);
            default -> List.of();
        };
    }

    private double scrollbarDragAnchorRatio = 0.0;

    void drawTimelineScrollbar(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, screen.UI_PANEL_ALT);
        screen.chrome.drawOutline(context, x, y, width, height, screen.UI_BORDER);
        int padX = 4;
        int padY = 2;
        int barX = x + padX;
        int barY = y + padY;
        int barWidth = width - padX * 2;
        int barHeight = height - padY * 2;
        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0x55303A44);

        double totalBeats = totalTimelineBeats();
        double viewStart = screen.state.visibleStartBeat();
        double viewEnd = screen.state.visibleEndBeat();
        double startRatio = EditorUtils.clamp(viewStart / totalBeats, 0.0, 1.0);
        double endRatio = EditorUtils.clamp(viewEnd / totalBeats, 0.0, 1.0);
        int thumbX = barX + (int) Math.round(startRatio * barWidth);
        int thumbRight = barX + (int) Math.round(endRatio * barWidth);
        int thumbWidth = Math.max(6, thumbRight - thumbX);
        context.fill(thumbX, barY, thumbX + thumbWidth, barY + barHeight, 0xFF9AD9FF);
        context.drawText(screen.getTextRenderer(), Text.literal(EditorUtils.format(viewStart) + " - " + EditorUtils.format(viewEnd)), barX + 6, barY + 1, screen.UI_MUTED, false);
    }

    void startTimelineScrollbarDrag(double mouseX) {
        EditorLayout layout = screen.editorLayout();
        int x = layout.centerX();
        int width = layout.centerWidth();
        int barX = x + 4;
        int barWidth = width - 8;
        double totalBeats = totalTimelineBeats();
        double viewStart = screen.state.visibleStartBeat();
        double startRatio = EditorUtils.clamp(viewStart / totalBeats, 0.0, 1.0);
        int thumbX = barX + (int) Math.round(startRatio * barWidth);
        scrollbarDragAnchorRatio = (mouseX - thumbX) / (double) barWidth;
        screen.dragMode = DragMode.TIMELINE_SCROLL;
    }

    void handleTimelineScrollbarDrag(double mouseX) {
        EditorLayout layout = screen.editorLayout();
        int x = layout.centerX();
        int width = layout.centerWidth();
        int barX = x + 4;
        int barWidth = Math.max(1, width - 8);
        double totalBeats = totalTimelineBeats();
        double targetRatio = (mouseX - barX) / (double) barWidth - scrollbarDragAnchorRatio;
        targetRatio = EditorUtils.clamp(targetRatio, 0.0, 1.0);
        screen.state.setVisibleStartBeat(targetRatio * totalBeats);
    }

    List<TimelineLaneLayout> buildTimelineLaneLayouts(int timelineY) {
        List<TimelineLaneLayout> layouts = new ArrayList<>();
        int laneTop = timelineY - screen.trackScrollY;
        for (TimelineLane lane : buildTimelineLanes()) {
            int laneHeight = laneHeight(lane);
            layouts.add(new TimelineLaneLayout(lane, laneTop, laneHeight));
            laneTop += laneHeight;
        }
        return layouts;
    }

    int totalTimelineLaneHeight() {
        int total = 0;
        for (TimelineLane lane : buildTimelineLanes()) {
            total += laneHeight(lane);
        }
        return total;
    }

    double totalTimelineBeats() {
        long lengthMillis = screen.state.audioLengthMillis();
        if (lengthMillis > 0) {
            return Math.max(0.0, screen.state.timing().calcBeat(lengthMillis));
        }
        double max = screen.state.hasPlaybackEndBeat() ? screen.state.playbackEndBeat() : 0.0;
        for (TrackData track : screen.state.level().tracks()) {
            for (NoteData note : track.notes()) {
                max = Math.max(max, note.beat());
            }
        }
        for (EffectData effect : screen.state.level().effects()) {
            max = Math.max(max, effect.beat());
        }
        return Math.max(16.0, max + 4.0);
    }

    void scrollTimelineLanes(int direction) {
        EditorLayout layout = screen.editorLayout();
        if (!layout.timelineVisible()) {
            screen.setTimelineFocused(true);
            layout = screen.editorLayout();
        }
        int contentHeight = layout.timelineHeight() - screen.TIMELINE_RULER_HEIGHT - screen.TIMELINE_AUDIO_STRIP_HEIGHT;
        int page = Math.max(24, contentHeight - 24);
        int maxScroll = Math.max(0, totalTimelineLaneHeight() - contentHeight);
        screen.trackScrollY = EditorUtils.clamp(screen.trackScrollY + direction * page, 0, maxScroll);
    }

    void clampTimelineLaneScroll() {
        EditorLayout layout = screen.editorLayout();
        if (!layout.timelineVisible()) {
            screen.trackScrollY = Math.max(0, screen.trackScrollY);
            return;
        }
        int contentHeight = Math.max(1,
                layout.timelineHeight() - screen.TIMELINE_RULER_HEIGHT - screen.TIMELINE_AUDIO_STRIP_HEIGHT);
        int maxScroll = Math.max(0, totalTimelineLaneHeight() - contentHeight);
        screen.trackScrollY = EditorUtils.clamp(screen.trackScrollY, 0, maxScroll);
    }

    int zoomBarWidth(int contentWidth) {
        return Math.max(24, Math.min(96, contentWidth - 24));
    }

    int zoomBarX(int contentX, int contentWidth) {
        return contentX + contentWidth - zoomBarWidth(contentWidth) - 8;
    }

    TimelineLaneLayout timelineLaneAt(List<TimelineLaneLayout> layouts, double mouseY) {
        for (TimelineLaneLayout layout : layouts) {
            if (mouseY >= layout.top() && mouseY < layout.top() + layout.height()) {
                return layout;
            }
        }
        return null;
    }

    private int laneHeight(TimelineLane lane) {
        return switch (lane.type) {
            case BPM, EVENT_GROUP_HEADER -> screen.BASE_ROW_HEIGHT;
            case FX_TRACK_HEADER -> screen.FX_TRACK_HEADER_HEIGHT;
            case FX_EFFECT_CLIP -> screen.FX_EFFECT_CLIP_HEIGHT;
            case TRACK_HEADER -> screen.TRACK_HEADER_HEIGHT;
            case NOTES -> screen.NOTE_LANE_HEIGHT;
            case EVENTS -> screen.EVENT_LANE_HEIGHT;
        };
    }

    private void drawTimelineLane(DrawContext context, TimelineLaneLayout layout, int x, int contentX, int contentWidth) {
        TimelineLane lane = layout.lane();
        switch (lane.type) {
            case BPM -> drawBpmLane(context, x, contentX, layout.top(), layout.height(), contentWidth);
            case FX_TRACK_HEADER -> drawFxTrackHeaderLane(context, x, contentX, layout.top(), layout.height(), contentWidth);
            case FX_EFFECT_CLIP -> drawFxEffectClipLane(context, lane.effect, x, contentX, layout.top(), layout.height(), contentWidth);
            case TRACK_HEADER -> drawTrackHeaderLane(context, lane.track, x, contentX, layout.top(), layout.height(), contentWidth);
            case NOTES -> drawTrackNotesLane(context, lane.track, x, contentX, layout.top(), layout.height(), contentWidth);
            case EVENT_GROUP_HEADER -> drawEventGroupHeaderLane(context, lane.track, lane.eventGroup, x, contentX, layout.top(), layout.height(), contentWidth);
            case EVENTS -> drawTrackEventLane(context, lane.track, lane.eventType, x, contentX, layout.top(), layout.height(), contentWidth);
        }
    }

    private void drawEventGroupHeaderLane(DrawContext context, TrackData track, String group, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null || group == null) return;
        boolean expanded = screen.state.isTrackEventGroupExpanded(track, group);
        int color = switch (group) {
            case "Speed" -> 0xFFE8B15B;
            case "Position" -> 0xFF7BC8FF;
            case "Rotation" -> 0xFFFF8F8F;
            case "Scale" -> 0xFFD2A8FF;
            default -> screen.UI_MUTED;
        };
        context.drawText(screen.getTextRenderer(), Text.literal((expanded ? "- " : "+ ") + group), labelX + 8, rowTop + laneHeight / 2 - 4, color, false);
        context.fill(labelX, rowTop + laneHeight - 1, contentX + width, rowTop + laneHeight, 0x15354048);
    }

    private void drawBpmLane(DrawContext context, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        context.drawText(screen.getTextRenderer(), Text.literal("BPM"), labelX + 8, rowTop + laneHeight / 2 - 4, 0x90D0FF, false);
        if (screen.state.estimatedBpmReference() > 0.0) {
            context.drawText(screen.getTextRenderer(), Text.literal("Ref " + format(screen.state.estimatedBpmReference())), labelX + 42, rowTop + laneHeight / 2 - 4, 0x66CCFF, false);
        }
        context.enableScissor(contentX + 1, rowTop, contentX + width - 1, rowTop + laneHeight);
        for (BpmPoint bpm : screen.state.level().meta().bpms()) {
            int markerX = screen.beatToScreen(contentX, width, bpm.beat());
            int markerY = rowTop + laneHeight / 2;
            context.fill(markerX - 3, markerY - 3, markerX + 3, markerY + 3, bpm == screen.state.selection().bpm() ? 0xFF88CCFF : 0xFF4477AA);
            context.drawText(screen.getTextRenderer(), Text.literal(format(bpm.bpm())), markerX + 4, markerY - 4, 0xFF88CCFF, false);
        }
        context.disableScissor();
    }

    private void drawFxTrackHeaderLane(DrawContext context, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        int accent = 0xAA6E4A1E;
        context.fill(labelX, rowTop, contentX + width, rowTop + laneHeight - 1, accent);
        context.fill(labelX + 4, rowTop + 5, labelX + 18, rowTop + 19, 0x55000000);
        context.drawText(screen.getTextRenderer(), Text.literal(screen.state.isFxTrackExpanded() ? "-" : "+"), labelX + 9, rowTop + 8, 0xFFFFFFFF, false);
        context.drawText(screen.getTextRenderer(), Text.literal("FX Track"), labelX + 24, rowTop + 8, 0xFFFFD090, false);
        context.drawText(screen.getTextRenderer(), Text.literal(screen.state.level().effects().size() + " clip(s)"), contentX + 8, rowTop + 8, 0xFFD7E8F4, false);
    }

    private void drawFxEffectClipLane(DrawContext context, EffectData effect, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (effect == null) {
            return;
        }
        boolean selected = screen.selectionManager.isEffectSelected(effect);
        String info = effectInfoLabel(effect);
        context.drawText(screen.getTextRenderer(), Text.literal(info), labelX + 8, rowTop + laneHeight / 2 - 4, selected ? 0xFFFFFFFF : 0xFFFFD090, false);
        context.enableScissor(contentX + 1, rowTop, contentX + width - 1, rowTop + laneHeight);
        double endBeat = effectEndBeat(effect);
        int startX = screen.beatToScreen(contentX, width, effect.beat());
        int endX = screen.beatToScreen(contentX, width, endBeat);
        int clipLeft = Math.min(startX, endX);
        int clipRight = Math.max(startX, endX);
        int clipTop = rowTop + 6;
        int clipBottom = rowTop + laneHeight - 6;
        int fillColor = selected ? 0x66FFD77A : 0x55AA7722;
        int edgeColor = selected ? 0xFFFFD77A : 0xFFAA7722;
        if (clipRight > clipLeft + 1) {
            context.fill(clipLeft, clipTop, clipRight, clipBottom, fillColor);
            context.fill(clipLeft, clipTop, clipLeft + 2, clipBottom, edgeColor);
            context.fill(clipRight - 2, clipTop, clipRight, clipBottom, edgeColor);
            context.fill(clipLeft, clipTop, clipRight, clipTop + 1, edgeColor);
            context.fill(clipLeft, clipBottom - 1, clipRight, clipBottom, edgeColor);
            String clipLabel = screen.propertyPanel.shortEffectLabel(effect.effectType()) + " " + effectClipText(effect);
            int labelWidth = screen.getTextRenderer().getWidth(clipLabel);
            if (clipRight - clipLeft > labelWidth + 8) {
                context.drawText(screen.getTextRenderer(), Text.literal(clipLabel), clipLeft + 4, rowTop + laneHeight / 2 - 4, 0xFFEBC48C, false);
            }
        } else {
            context.fill(startX - 2, clipTop, startX + 2, clipBottom, edgeColor);
        }
        Set<String> groups = effectGroupNames(effect);
        if (!groups.isEmpty()) {
            int groupColor = groupNameColor(groups.iterator().next());
            context.fill(clipLeft, clipTop - 2, clipRight, clipTop, groupColor);
            String groupLabel = screen.chrome.trimToWidth(groups.iterator().next(), Math.max(20, clipRight - clipLeft - 4));
            if (clipRight - clipLeft > screen.getTextRenderer().getWidth(groupLabel) + 4) {
                context.drawText(screen.getTextRenderer(), Text.literal(groupLabel), clipLeft + 2, clipTop - 10, groupColor, false);
            }
        }
        context.disableScissor();
    }

    double effectEndBeat(EffectData effect) {
        if (effect == null || effect.properties() == null) {
            return effect == null ? 0.0 : effect.beat();
        }
        long durationMs = effect.properties().has("duration") ? effect.properties().get("duration").getAsLong() : 0L;
        if (durationMs > 0L) {
            double startMs = screen.state.timing().beatToMillis(effect.beat());
            return screen.state.timing().calcBeat((long) (startMs + durationMs));
        }
        return effect.beat() + 1.0;
    }

    private String effectInfoLabel(EffectData effect) {
        return screen.propertyPanel.shortEffectLabel(effect.effectType()) + " @ " + format(effect.beat());
    }

    private Set<String> effectGroupNames(EffectData effect) {
        return screen.state.editorDraft().effectGroupNamesContaining(effect);
    }

    private Set<String> noteGroupNames(NoteData note) {
        for (TrackData track : screen.state.tracks()) {
            if (track.notes().contains(note)) {
                EditorDraft.NoteGroupEntry entry = new EditorDraft.NoteGroupEntry(track.id(), note.beat(), note.noteType());
                Set<String> names = screen.state.editorDraft().noteGroupNamesContaining(entry);
                if (!names.isEmpty()) {
                    return names;
                }
            }
        }
        return Set.of();
    }

    private int groupNameColor(String name) {
        int hash = name.hashCode();
        float hue = (Math.abs(hash) % 360) / 360.0f;
        return hsbToRgb(hue, 0.75f, 0.9f);
    }

    private int hsbToRgb(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - saturation * (1.0f - f));
            switch ((int) h) {
                case 0 -> { r = (int) (brightness * 255.0f + 0.5f); g = (int) (t * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); }
                case 1 -> { r = (int) (q * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); }
                case 2 -> { r = (int) (p * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (t * 255.0f + 0.5f); }
                case 3 -> { r = (int) (p * 255.0f + 0.5f); g = (int) (q * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); }
                case 4 -> { r = (int) (t * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); }
                case 5 -> { r = (int) (brightness * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (q * 255.0f + 0.5f); }
            }
        }
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private String effectClipText(EffectData effect) {
        JsonObject properties = effect.properties() == null ? new JsonObject() : effect.properties();
        String text = firstProperty(properties, "text", "content", "title", "message", "value", "id");
        if (text == null || text.isBlank()) {
            text = propertyString(properties, "id", "-");
        }
        return screen.chrome.trimToWidth(text, Math.max(40, screen.TIMELINE_LABEL_WIDTH - 24));
    }

    private String firstProperty(JsonObject properties, String... keys) {
        for (String key : keys) {
            if (properties.has(key) && !properties.get(key).isJsonNull()) {
                String value = properties.get(key).getAsString();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private String propertyString(JsonObject properties, String key, String fallback) {
        if (properties.has(key) && !properties.get(key).isJsonNull()) {
            String value = properties.get(key).getAsString();
            if (!value.isBlank()) {
                return value;
            }
        }
        return fallback;
    }

    private void drawTrackHeaderLane(DrawContext context, TrackData track, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null) {
            return;
        }
        int accent = screen.state.selectedTrack() == track ? 0xAA3A6EA5 : 0x66405060;
        context.fill(labelX, rowTop, contentX + width, rowTop + laneHeight - 1, accent);
        context.fill(labelX + 4, rowTop + 5, labelX + 18, rowTop + 19, 0x55000000);
        context.drawText(screen.getTextRenderer(), Text.literal(screen.state.isTrackExpanded(track) ? "-" : "+"), labelX + 9, rowTop + 8, 0xFFFFFFFF, false);
        context.drawText(screen.getTextRenderer(), Text.literal("TrackID " + track.id()), labelX + 24, rowTop + 5, 0xFFFFFFFF, false);
        context.drawText(screen.getTextRenderer(), Text.literal("Notes " + track.notes().size() + "  |  Grid 1/" + track.beatDivision() + "  |  Event Lanes " + screen.countVisibleEventLanes(track)), contentX + 8, rowTop + 5, 0xFFD7E8F4, false);
    }

    private void drawTrackNotesLane(DrawContext context, TrackData track, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null) {
            return;
        }
        double range = EditorUtils.noteLaneProjectionRange(track);
        int centerY = rowTop + laneHeight / 2;
        context.drawText(screen.getTextRenderer(), Text.literal("Notes"), labelX + 8, rowTop + 8, 0xFF9CB1C2, false);
        context.drawText(screen.getTextRenderer(), Text.literal(track.notes().size() + "  1/" + track.beatDivision() + "  |  range " + format(range)), labelX + 8, rowTop + 20, 0x81B8CF, false);
        context.enableScissor(contentX + 1, rowTop, contentX + width - 1, rowTop + laneHeight);
        context.fill(contentX, centerY, contentX + width, centerY + 1, 0x18A5D6F6);
        drawTrackDivisionGrid(context, track, contentX, rowTop, width, laneHeight);
        for (NoteData note : track.notes()) {
            int noteX = screen.beatToScreen(contentX, width, note.beat());
            double projectionValue = EditorUtils.noteProjectionValue(note);
            int noteY = EditorUtils.noteProjectionToScreenY(rowTop, laneHeight, range, projectionValue);
            boolean selected = screen.selectionManager.isNoteSelected(track, note);
            int color = selected ? 0xFFFFFFFF : EditorUtils.noteColor(note.noteType());
            if (note.noteType() == NoteType.HOLD) {
                int holdEndX = screen.beatToScreen(contentX, width, note.beat() + note.holdLengthBeats());
                context.fill(Math.min(noteX, holdEndX), noteY - 3, Math.max(noteX + 2, holdEndX), noteY + 3, selected ? 0xAAFFFFFF : 0xAA26A69A);
                context.fill(holdEndX - 2, noteY - 7, holdEndX + 2, noteY + 7, selected ? 0xFFFFFFFF : 0xFF26A69A);
            }
            int halfWidth = selected ? 8 : 6;
            int halfHeight = selected ? 5 : 4;
            context.fill(noteX - halfWidth, noteY - halfHeight, noteX + halfWidth, noteY + halfHeight, color);
            if (selected) {
                context.fill(noteX - 1, rowTop + 4, noteX + 1, rowTop + laneHeight - 4, 0x88FFFFFF);
            }
            Set<String> groups = noteGroupNames(note);
            if (!groups.isEmpty()) {
                int groupColor = groupNameColor(groups.iterator().next());
                context.fill(noteX - halfWidth - 1, noteY - halfHeight - 1, noteX + halfWidth + 1, noteY - halfHeight, groupColor);
                context.fill(noteX - halfWidth - 1, noteY + halfHeight, noteX + halfWidth + 1, noteY + halfHeight + 1, groupColor);
                context.fill(noteX - halfWidth - 1, noteY - halfHeight, noteX - halfWidth, noteY + halfHeight, groupColor);
                context.fill(noteX + halfWidth, noteY - halfHeight, noteX + halfWidth + 1, noteY + halfHeight, groupColor);
            }
        }
        context.disableScissor();
    }

    private void drawTrackEventLane(DrawContext context, TrackData track, EventLaneType eventType, int labelX, int contentX, int rowTop, int laneHeight, int width) {
        if (track == null || eventType == null) {
            return;
        }
        context.drawText(screen.getTextRenderer(), Text.literal(eventType.label), labelX + 8, rowTop + laneHeight / 2 - 4, eventType.color, false);
        List<NumEventData> events = screen.eventsForLane(track, eventType);
        if (!events.isEmpty()) {
            NumEventData event = events.getFirst();
            context.drawText(screen.getTextRenderer(), Text.literal(EditorUtils.shortEasingLabel(event.easingType().name()) + " " + format(event.startValue()) + ">" + format(event.endValue())), labelX + 62, rowTop + laneHeight / 2 - 4, 0xBFCFDF, false);
        }
        context.enableScissor(contentX + 1, rowTop, contentX + width - 1, rowTop + laneHeight);
        drawTrackDivisionGrid(context, track, contentX, rowTop, width, laneHeight);
        double[] range = eventValueRange(events);
        for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
            NumEventData event = events.get(eventIndex);
            int startX = screen.beatToScreen(contentX, width, event.startBeat());
            int endX = screen.beatToScreen(contentX, width, event.endBeat());
            if (endX <= startX) {
                endX = startX + 2;
            }
            boolean clipSelected = screen.isEventClipSelected(track, eventType, eventIndex);
            context.fill(startX, rowTop + 4, endX, rowTop + laneHeight - 4, eventType.fillColor);
            if (clipSelected) {
                context.fill(startX, rowTop + 4, endX, rowTop + 6, 0xB0FFFFFF);
                context.fill(startX, rowTop + laneHeight - 6, endX, rowTop + laneHeight - 4, 0xB0FFFFFF);
                context.fill(startX, rowTop + 4, startX + 2, rowTop + laneHeight - 4, 0xB0FFFFFF);
                context.fill(endX - 2, rowTop + 4, endX, rowTop + laneHeight - 4, 0xB0FFFFFF);
            }
            drawEventCurve(context, contentX, width, rowTop, laneHeight, event, eventType, range[0], range[1]);
            SelectedEventHandle currentHandle = screen.selectedEventHandle;
            boolean selected = currentHandle != null && currentHandle.track == track && currentHandle.eventType == eventType && currentHandle.eventIndex == eventIndex;
            int startY = valueToLaneY(rowTop, laneHeight, event.startValue(), range[0], range[1]);
            int endY = valueToLaneY(rowTop, laneHeight, event.endValue(), range[0], range[1]);
            int handleColor = selected ? 0xFFFFFFFF : eventType.color;
            int edgeHandleColor = clipSelected ? 0xFFFFFFFF : 0xCC000000;
            context.fill(startX - 2, rowTop + 4, startX + 2, rowTop + laneHeight - 4, edgeHandleColor);
            context.fill(endX - 2, rowTop + 4, endX + 2, rowTop + laneHeight - 4, edgeHandleColor);
            context.fill(startX - 2, startY - 2, startX + 2, startY + 2, handleColor);
            context.fill(endX - 2, endY - 2, endX + 2, endY + 2, handleColor);
        }
        context.disableScissor();
    }

    private void drawTrackDivisionGrid(DrawContext context, TrackData track, int contentX, int rowTop, int width, int laneHeight) {
        double step = trackGridStep(track);
        double startBeat = Math.floor(screen.state.visibleStartBeat() / step) * step;
        double endBeat = screen.state.visibleEndBeat() + step;
        int previousX = Integer.MIN_VALUE;
        for (double beat = startBeat; beat <= endBeat; beat += step) {
            int lineX = screen.beatToScreen(contentX, width, beat);
            if (lineX - previousX < 5) {
                continue;
            }
            previousX = lineX;
            boolean whole = isWholeBeat(beat);
            int color = whole ? 0x335FBCD3 : 0x1E5FBCD3;
            context.fill(lineX, rowTop + 4, lineX + 1, rowTop + laneHeight - 4, color);
        }
    }

    private void drawEventCurve(DrawContext context, int x, int width, int rowTop, int laneHeight, NumEventData event, EventLaneType eventType, double minValue, double maxValue) {
        int previousX = screen.beatToScreen(x, width, event.startBeat());
        int previousY = valueToLaneY(rowTop, laneHeight, event.startValue(), minValue, maxValue);
        int samples = Math.max(8, Math.min(48, screen.beatToScreen(x, width, event.endBeat()) - previousX));
        for (int step = 1; step <= samples; step++) {
            double progress = step / (double) samples;
            double beat = event.startBeat() + (event.endBeat() - event.startBeat()) * progress;
            double value = EasingFunctions.getEase(event.startValue(), event.endValue(), progress, event.easingType());
            int nextX = screen.beatToScreen(x, width, beat);
            int nextY = valueToLaneY(rowTop, laneHeight, value, minValue, maxValue);
            int fromX = Math.min(previousX, nextX);
            int toX = Math.max(previousX, nextX) + 1;
            int fromY = Math.min(previousY, nextY);
            int toY = Math.max(previousY, nextY) + 1;
            context.fill(fromX, fromY, toX, toY, eventType.color);
            previousX = nextX;
            previousY = nextY;
        }
    }

    double[] eventValueRange(List<NumEventData> events) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (NumEventData event : events) {
            min = Math.min(min, Math.min(event.startValue(), event.endValue()));
            max = Math.max(max, Math.max(event.startValue(), event.endValue()));
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            return new double[]{-1.0, 1.0};
        }
        if (Math.abs(max - min) < 1.0E-6) {
            min -= 1.0;
            max += 1.0;
        }
        double padding = (max - min) * 0.15;
        return new double[]{min - padding, max + padding};
    }

    private int valueToLaneY(int rowTop, int laneHeight, double value, double minValue, double maxValue) {
        double progress = (value - minValue) / (maxValue - minValue);
        progress = Math.max(0.0, Math.min(1.0, progress));
        return rowTop + laneHeight - 5 - (int) Math.round(progress * (laneHeight - 10));
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

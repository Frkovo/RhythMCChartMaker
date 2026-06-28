package cn.frkovo.rhythmcv2.rmcChart.client.editor.audio;

import cn.frkovo.rhythmcv2.rmcChart.chart.core.TimingTimeline;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.LevelData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side autoplay hit sound scheduler for the in-world server preview.
 *
 * <p>Mimics {@code RhythMC-Reborn} {@code SoundGuidance} behavior:
 * <ul>
 *   <li>TAP / LOOK: note-block bass drum.</li>
 *   <li>HOLD: pressure-plate click-on for start/solo, click-off for end; middle blocks are silent.</li>
 *   <li>DODGE: silent.</li>
 * </ul>
 */
public final class PreviewAutoSoundScheduler {

    private final List<PreviewNote> queue = new ArrayList<>();
    private int nextIndex = 0;

    public void reset(ChartEditorState state, double startBeat) {
        queue.clear();
        nextIndex = 0;
        if (state == null) {
            return;
        }

        LevelData level = state.level();
        TimingTimeline timing = state.timing();

        Map<Integer, List<NoteData>> holdGroups = new HashMap<>();
        for (TrackData track : level.tracks()) {
            for (NoteData note : track.notes()) {
                if (note.noteType() == NoteType.HOLD && note.holdGroup() >= 0) {
                    holdGroups.computeIfAbsent(note.holdGroup(), k -> new ArrayList<>()).add(note);
                }
            }
        }

        Map<NoteData, HoldPosition> positions = new IdentityHashMap<>();
        for (List<NoteData> group : holdGroups.values()) {
            group.sort(Comparator.comparingDouble(NoteData::beat));
            if (group.size() == 1) {
                positions.put(group.get(0), HoldPosition.SOLO);
            } else {
                positions.put(group.get(0), HoldPosition.START);
                positions.put(group.get(group.size() - 1), HoldPosition.END);
                for (int i = 1; i < group.size() - 1; i++) {
                    positions.put(group.get(i), HoldPosition.MIDDLE);
                }
            }
        }

        List<PreviewNote> nextQueue = new ArrayList<>();
        for (TrackData track : level.tracks()) {
            for (NoteData note : track.notes()) {
                NoteType type = note.noteType();
                if (type == NoteType.DODGE) {
                    continue;
                }
                long millis = timing.beatToMillis(note.beat());
                if (type == NoteType.HOLD) {
                    HoldPosition pos = note.holdGroup() < 0
                            ? HoldPosition.SOLO
                            : positions.getOrDefault(note, HoldPosition.NONE);
                    nextQueue.add(new PreviewNote(note, millis, SoundType.HOLD, pos));
                } else {
                    nextQueue.add(new PreviewNote(note, millis, SoundType.TAP_LOOK, HoldPosition.NONE));
                }
            }
        }
        nextQueue.sort(Comparator.comparingLong(PreviewNote::millis));
        queue.addAll(nextQueue);

        long startMillis = timing.beatToMillis(startBeat);
        while (nextIndex < queue.size() && queue.get(nextIndex).millis() < startMillis) {
            nextIndex++;
        }
    }

    public void tick(ChartEditorState state) {
        if (!state.serverPreviewRunning() || !state.playing()) {
            return;
        }
        long now = state.playheadMillis();
        while (nextIndex < queue.size() && queue.get(nextIndex).millis() <= now) {
            play(queue.get(nextIndex));
            nextIndex++;
        }
    }

    private void play(PreviewNote note) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        var soundManager = client.getSoundManager();
        switch (note.soundType()) {
            case TAP_LOOK -> soundManager.play(
                    PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), 1.0f));
            case HOLD -> {
                switch (note.holdPosition()) {
                    case START, SOLO -> soundManager.play(
                            PositionedSoundInstance.ui(SoundEvents.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON, 1.0f));
                    case END -> soundManager.play(
                            PositionedSoundInstance.ui(SoundEvents.BLOCK_STONE_PRESSURE_PLATE_CLICK_OFF, 1.0f));
                    default -> {
                    }
                }
            }
        }
    }

    private record PreviewNote(NoteData note, long millis, SoundType soundType, HoldPosition holdPosition) {
    }

    private enum SoundType {
        TAP_LOOK,
        HOLD
    }

    private enum HoldPosition {
        NONE,
        START,
        MIDDLE,
        END,
        SOLO
    }
}

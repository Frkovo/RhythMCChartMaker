package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.LevelData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class EditorLevelSorter {
    private EditorLevelSorter() {
    }

    static void sortCurrentLevel(LevelData level, Set<Integer> expandedTrackIds, Map<Integer, Set<String>> expandedTrackEventGroups) {
        level.tracks().sort(Comparator.comparingInt(TrackData::id));
        if (expandedTrackIds != null) {
            expandedTrackIds.retainAll(level.tracks().stream().map(TrackData::id).collect(Collectors.toSet()));
        }
        if (expandedTrackEventGroups != null) {
            expandedTrackEventGroups.keySet().retainAll(level.tracks().stream().map(TrackData::id).collect(Collectors.toSet()));
        }
        for (TrackData track : level.tracks()) {
            track.ensureDefaultEvents();
            normalizeSingleTrackEvent(track.speedEvents());
            normalizeSingleTrackEvent(track.xTransformEvents());
            normalizeSingleTrackEvent(track.yTransformEvents());
            normalizeSingleTrackEvent(track.zTransformEvents());
            normalizeSingleTrackEvent(track.xRotateEvents());
            normalizeSingleTrackEvent(track.yRotateEvents());
            normalizeSingleTrackEvent(track.zRotateEvents());
            normalizeSingleTrackEvent(track.xScaleEvents());
            normalizeSingleTrackEvent(track.yScaleEvents());
            normalizeSingleTrackEvent(track.zScaleEvents());
            track.notes().sort(Comparator.comparingDouble(NoteData::beat));
        }
        level.effects().sort(Comparator.comparingDouble(EffectData::beat));
        level.meta().bpms().sort(Comparator.comparingDouble(BpmPoint::beat));
    }

    private static void sortEvents(List<NumEventData> events) {
        events.sort(Comparator.comparingDouble(NumEventData::startBeat));
    }

    private static void normalizeSingleTrackEvent(List<NumEventData> events) {
        sortEvents(events);
        if (events.size() <= 1) {
            return;
        }
        NumEventData first = events.getFirst();
        events.clear();
        events.add(first);
    }
}

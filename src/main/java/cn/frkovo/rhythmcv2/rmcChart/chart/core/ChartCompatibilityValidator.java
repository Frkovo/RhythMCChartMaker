package cn.frkovo.rhythmcv2.rmcChart.chart.core;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.LevelData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChartCompatibilityValidator {
    private ChartCompatibilityValidator() {
    }

    public static Report validate(LevelData level) {
        List<Issue> issues = new ArrayList<>();
        if (level == null) {
            issues.add(new Issue(Severity.ERROR, "Level", "No active level is loaded"));
            return new Report(issues);
        }
        validateBpms(level, issues);
        for (TrackData track : level.tracks()) {
            validateTrack(track, issues);
        }
        for (int i = 0; i < level.effects().size(); i++) {
            validateEffect(i, level.effects().get(i), issues);
        }
        return new Report(issues);
    }

    private static void validateBpms(LevelData level, List<Issue> issues) {
        if (level.meta().bpms().isEmpty()) {
            issues.add(new Issue(Severity.ERROR, "BPM", "At least one BPM point is required"));
            return;
        }
        double previousBeat = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < level.meta().bpms().size(); i++) {
            BpmPoint bpm = level.meta().bpms().get(i);
            String location = "BPM " + i;
            if (!isFinite(bpm.beat()) || !isFinite(bpm.bpm()) || bpm.bpm() <= 0.0) {
                issues.add(new Issue(Severity.ERROR, location, "BPM beat and value must be finite, and BPM must be positive"));
            }
            if (bpm.beat() < previousBeat) {
                issues.add(new Issue(Severity.WARNING, location, "BPM points should be sorted by beat"));
            }
            previousBeat = bpm.beat();
        }
    }

    private static void validateTrack(TrackData track, List<Issue> issues) {
        String prefix = "Track " + track.id();
        validateSingleEventLane(prefix, "Speed", track.speedEvents(), issues);
        validateSingleEventLane(prefix, "X Transform", track.xTransformEvents(), issues);
        validateSingleEventLane(prefix, "Y Transform", track.yTransformEvents(), issues);
        validateSingleEventLane(prefix, "Z Transform", track.zTransformEvents(), issues);
        validateSingleEventLane(prefix, "X Rotation", track.xRotateEvents(), issues);
        validateSingleEventLane(prefix, "Y Rotation", track.yRotateEvents(), issues);
        validateSingleEventLane(prefix, "Z Rotation", track.zRotateEvents(), issues);
        validateSingleEventLane(prefix, "X Scale", track.xScaleEvents(), issues);
        validateSingleEventLane(prefix, "Y Scale", track.yScaleEvents(), issues);
        validateSingleEventLane(prefix, "Z Scale", track.zScaleEvents(), issues);
        validateNotes(track, issues);
    }

    private static void validateSingleEventLane(String trackPrefix, String lane, List<NumEventData> events, List<Issue> issues) {
        String location = trackPrefix + " / " + lane;
        if (events.size() > 1) {
            issues.add(new Issue(Severity.ERROR, location, "Reborn treats this property as a single event lane; keep only one event"));
        }
        for (int i = 0; i < events.size(); i++) {
            NumEventData event = events.get(i);
            if (!isFinite(event.startBeat()) || !isFinite(event.endBeat()) || event.endBeat() <= event.startBeat()) {
                issues.add(new Issue(Severity.ERROR, location + " event " + i, "Event end beat must be greater than start beat"));
            }
            if (!isFinite(event.startValue()) || !isFinite(event.endValue())) {
                issues.add(new Issue(Severity.ERROR, location + " event " + i, "Event values must be finite"));
            }
            if (event.easingType() == null) {
                issues.add(new Issue(Severity.ERROR, location + " event " + i, "Event easing is missing"));
            }
        }
    }

    private static void validateNotes(TrackData track, List<Issue> issues) {
        Map<Integer, Integer> holdGroupCounts = new HashMap<>();
        for (NoteData note : track.notes()) {
            if (note.noteType() == NoteType.HOLD && note.holdGroup() >= 0) {
                holdGroupCounts.merge(note.holdGroup(), 1, Integer::sum);
            }
        }

        for (int i = 0; i < track.notes().size(); i++) {
            NoteData note = track.notes().get(i);
            String location = "Track " + track.id() + " / Note " + i;
            if (!isFinite(note.beat())) {
                issues.add(new Issue(Severity.ERROR, location, "Note beat must be finite"));
            }
            if (!isFinite(note.pos().x()) || !isFinite(note.pos().y()) || !isFinite(note.pos().z())) {
                issues.add(new Issue(Severity.ERROR, location, "Note position values must be finite"));
            }
            if (!isFinite(note.scale().x()) || !isFinite(note.scale().y()) || !isFinite(note.scale().z())) {
                issues.add(new Issue(Severity.ERROR, location, "Note scale values must be finite"));
            }
            if (!isFinite(note.rotation().x()) || !isFinite(note.rotation().y()) || !isFinite(note.rotation().z())) {
                issues.add(new Issue(Severity.ERROR, location, "Note rotation values must be finite"));
            }
            if (note.noteType() == NoteType.HOLD) {
                if (note.holdLengthBeats() <= 0.0 && note.holdGroup() >= 0 && holdGroupCounts.getOrDefault(note.holdGroup(), 0) < 2) {
                    issues.add(new Issue(Severity.WARNING, location, "Hold group has fewer than two HOLD notes"));
                }
            }
        }
    }

    private static void validateEffect(int index, EffectData effect, List<Issue> issues) {
        String location = "Effect " + index + " / " + effect.effectType();
        JsonObject properties = effect.properties();
        if (!isFinite(effect.beat())) {
            issues.add(new Issue(Severity.ERROR, location, "Effect beat must be finite"));
        }
        if (properties == null) {
            issues.add(new Issue(Severity.ERROR, location, "Effect properties must not be null"));
            return;
        }
        switch (effect.effectType()) {
            case TEXT_DISPLAY, TEXT_DISPLAY_EFFECT, TEXT_DISPLAY_SYNC_TRACK, TEXT_DISPLAY_DESYNC_TRACK, TEXT_DISPLAY_REMOVE -> {
                if (!hasString(properties, "id")) {
                    issues.add(new Issue(Severity.WARNING, location, "Text display effects should include an id"));
                }
            }
            case HIDE_NOTES -> {
                if (!properties.has("noteTypes") && !properties.has("types") && !properties.has("tracks") && !properties.has("trackIds")
                        && !properties.has("noteType") && !properties.has("trackId")) {
                    issues.add(new Issue(Severity.WARNING, location, "HIDE_NOTES should target note types or track ids"));
                }
            }
            case GLOW_COLOR -> {
                if (!hasString(properties, "color")) {
                    issues.add(new Issue(Severity.WARNING, location, "GLOW_COLOR should include a color"));
                }
            }
            default -> {
            }
        }
    }

    private static boolean hasString(JsonObject properties, String key) {
        return properties.has(key) && properties.get(key).isJsonPrimitive() && !properties.get(key).getAsString().isBlank();
    }

    private static boolean isFinite(double value) {
        return Double.isFinite(value);
    }

    public enum Severity {
        ERROR,
        WARNING
    }

    public record Issue(Severity severity, String location, String message) {
    }

    public record Report(List<Issue> issues) {
        public int errorCount() {
            int count = 0;
            for (Issue issue : issues) {
                if (issue.severity() == Severity.ERROR) {
                    count++;
                }
            }
            return count;
        }

        public int warningCount() {
            int count = 0;
            for (Issue issue : issues) {
                if (issue.severity() == Severity.WARNING) {
                    count++;
                }
            }
            return count;
        }

        public boolean ok() {
            return issues.isEmpty();
        }

        public String summary() {
            if (ok()) {
                return "Compatibility check passed";
            }
            return "Compatibility check: " + errorCount() + " error(s), " + warningCount() + " warning(s)";
        }
    }
}

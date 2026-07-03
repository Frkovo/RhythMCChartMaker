package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.Vec3Data;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.NoteAxis;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EditorUtils {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private EditorUtils() {
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static double lerp(double left, double right, double t) {
        return left + (right - left) * t;
    }

    static Vec3Data lerpVec(Vec3Data left, Vec3Data right, double t) {
        return new Vec3Data(lerp(left.x(), right.x(), t), lerp(left.y(), right.y(), t), lerp(left.z(), right.z(), t));
    }

    static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    static String formatMillis(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        long ms = Math.max(0L, millis % 1000L);
        return String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, ms);
    }

    static int parseIntOrDefault(String value, int fallback) {
        try {
            return parseInt(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    static int parseInt(String value) {
        return Integer.parseInt(value.trim());
    }

    static double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    static long parseLong(String value) {
        return Long.parseLong(value.trim());
    }

    static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    static boolean rectanglesIntersect(int leftA, int topA, int rightA, int bottomA, int leftB, int topB, int rightB, int bottomB) {
        return rightA >= leftB && rightB >= leftA && bottomA >= topB && bottomB >= topA;
    }

    static double squaredDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    static int hsbToRgb(float hue, float saturation, float brightness) {
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
                case 0 -> {
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                }
                case 1 -> {
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                }
                case 2 -> {
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                }
                case 3 -> {
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                }
                case 4 -> {
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                }
                case 5 -> {
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                }
            }
        }
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    static void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color, int thickness) {
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

    static void drawTrimmedText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int maxWidth, int color) {
        String display = trimToWidth(textRenderer, text == null ? "" : text, maxWidth);
        context.drawText(textRenderer, Text.literal(display), x, y, color, false);
    }

    static String trimToWidth(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = textRenderer.getWidth(suffix);
        int length = text.length();
        while (length > 0 && textRenderer.getWidth(text.substring(0, length)) + suffixWidth > maxWidth) {
            length--;
        }
        return text.substring(0, Math.max(0, length)) + suffix;
    }

    static String joinCsv(List<String> values) {
        return String.join(",", values);
    }

    static List<String> splitCsv(String text) {
        List<String> values = new ArrayList<>();
        if (text.isBlank()) {
            return values;
        }
        for (String token : text.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    static String serializeVec(Vec3Data vec) {
        return format(vec.x()) + "," + format(vec.y()) + "," + format(vec.z());
    }

    static Vec3Data parseVec(String value) {
        String[] parts = value.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expected x,y,z");
        }
        return new Vec3Data(parseDouble(parts[0]), parseDouble(parts[1]), parseDouble(parts[2]));
    }

    static Vec3Data parseOptionalVec(String value, Vec3Data fallback) {
        if (value == null || value.isBlank()) {
            return fallback.copy();
        }
        return parseVec(value);
    }

    static String serializeTriple(double x, double y, double z) {
        return format(x) + "," + format(y) + "," + format(z);
    }

    static String serializeNumEvents(List<NumEventData> events) {
        JsonArray array = new JsonArray();
        for (NumEventData event : events) {
            JsonObject object = new JsonObject();
            object.addProperty("startBeat", event.startBeat());
            object.addProperty("endBeat", event.endBeat());
            object.addProperty("startValue", event.startValue());
            object.addProperty("endValue", event.endValue());
            object.addProperty("easingType", event.easingType().id());
            array.add(object);
        }
        return GSON.toJson(array);
    }

    static String serializeEventList(List<NumEventData> events) {
        List<String> rows = new ArrayList<>();
        for (NumEventData event : events) {
            rows.add(format(event.startBeat()) + "," + format(event.endBeat()) + "," + format(event.startValue()) + "," + format(event.endValue()) + "," + event.easingType().id());
        }
        return String.join(" | ", rows);
    }

    static List<NumEventData> parseEventList(String text) {
        List<NumEventData> events = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return events;
        }
        String[] rows = text.split("\\|");
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(",");
            if (parts.length < 5) {
                throw new IllegalArgumentException("Event entry requires: start,end,startValue,endValue,easingId");
            }
            events.add(new NumEventData(
                    parseDouble(parts[0]),
                    parseDouble(parts[1]),
                    parseDouble(parts[2]),
                    parseDouble(parts[3]),
                    EasingType.fromId(parseInt(parts[4]))
            ));
        }
        return events;
    }

    static String serializeMapEntries(List<Map<String, Object>> entries) {
        List<String> rows = new ArrayList<>();
        for (Map<String, Object> map : entries) {
            List<String> pairs = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                pairs.add(entry.getKey() + "=" + entry.getValue());
            }
            rows.add(String.join(",", pairs));
        }
        return String.join(" | ", rows);
    }

    static List<Map<String, Object>> parseMapEntries(String text) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return list;
        }
        String[] rows = text.split("\\|");
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            for (String pair : trimmed.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
            if (!map.isEmpty()) {
                list.add(map);
            }
        }
        return list;
    }

    static String serializeExtraProperties(JsonObject object) {
        if (object == null || object.entrySet().isEmpty()) {
            return "";
        }
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String value = entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : entry.getValue().toString();
            pairs.add(entry.getKey() + "=" + value);
        }
        return String.join(",", pairs);
    }

    static Map<String, String> parseKeyValuePairs(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return map;
        }
        for (String pair : text.split(",")) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] kv = trimmed.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    static List<NumEventData> parseNumEvents(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonArray()) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        List<NumEventData> events = new ArrayList<>();
        for (JsonElement element : parsed.getAsJsonArray()) {
            JsonObject object = element.getAsJsonObject();
            events.add(new NumEventData(
                    object.get("startBeat").getAsDouble(),
                    object.get("endBeat").getAsDouble(),
                    object.get("startValue").getAsDouble(),
                    object.get("endValue").getAsDouble(),
                    EasingType.fromId(object.get("easingType").getAsInt())
            ));
        }
        return events;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> parseMapList(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonArray()) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        List<Map<String, Object>> list = GSON.fromJson(parsed, List.class);
        return list == null ? List.of() : list;
    }

    static void replaceStrings(List<String> target, List<String> source) {
        target.clear();
        target.addAll(source);
    }

    static void replaceMaps(List<Map<String, Object>> target, List<Map<String, Object>> source) {
        target.clear();
        target.addAll(source);
    }

    static void replaceNumEvents(List<NumEventData> target, List<NumEventData> source) {
        target.clear();
        target.addAll(source);
    }

    static void putIfNotBlank(JsonObject object, String key, String value) {
        if (value != null && !value.isBlank()) {
            object.addProperty(key, value.trim());
        }
    }

    static void putIntIfNotBlank(JsonObject object, String key, String value) {
        if (value != null && !value.isBlank()) {
            object.addProperty(key, parseInt(value));
        }
    }

    static String propertyString(JsonObject properties, String key, String fallback) {
        if (properties != null && properties.has(key) && properties.get(key).isJsonPrimitive()) {
            return properties.get(key).getAsString();
        }
        return fallback;
    }

    static String firstProperty(JsonObject properties, String... keys) {
        for (String key : keys) {
            if (properties != null && properties.has(key) && properties.get(key).isJsonPrimitive()) {
                return properties.get(key).getAsString();
            }
        }
        return "";
    }

    static double getDouble(JsonObject properties, String key, double fallback) {
        if (properties != null && properties.has(key) && properties.get(key).isJsonPrimitive()) {
            try {
                return properties.get(key).getAsDouble();
            } catch (RuntimeException ignored) {
            }
        }
        return fallback;
    }

    static String shortNoteLabel(NoteType noteType) {
        return switch (noteType) {
            case TAP -> "TAP";
            case LOOK -> "LOOK";
            case HOLD -> "HOLD";
            case DODGE -> "DODGE";
        };
    }

    static NoteType nextNoteType(NoteType noteType) {
        NoteType[] values = NoteType.values();
        int index = noteType == null ? 0 : noteType.ordinal() + 1;
        return values[index % values.length];
    }

    static int noteColor(NoteType noteType) {
        return switch (noteType) {
            case TAP -> 0xFF4FC3F7;
            case LOOK -> 0xFFFFF176;
            case HOLD -> 0xFF26A69A;
            case DODGE -> 0xFFEF5350;
        };
    }

    static String shortEffectLabel(cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectType effectType) {
        return switch (effectType) {
            case TEXT_DISPLAY -> "TXT";
            case TEXT_DISPLAY_EFFECT -> "TXT+";
            case TEXT_DISPLAY_REMOVE -> "TXT-";
            case TEXT_DISPLAY_SYNC_TRACK -> "SYNC";
            default -> effectType.name().substring(0, Math.min(3, effectType.name().length()));
        };
    }

    static String shortEasingLabel(String easingName) {
        return easingName.length() <= 8 ? easingName : easingName.substring(0, 8);
    }

    static String trackEaseDisplayLabel(EasingType easingType) {
        String name = easingType.name().replace("IN_OUT_", "InOut ")
                .replace("IN_", "In ")
                .replace("OUT_", "Out ")
                .replace('_', ' ')
                .toLowerCase(Locale.ROOT);
        String[] parts = name.split(" ");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return String.join(" ", words);
    }

    static String shortTrackEaseLabel(EasingType easingType) {
        String name = easingType.name().replace('_', ' ');
        return name.length() <= 9 ? name : name.substring(0, 9);
    }

    static String shortTrackEasePopupLabel(EasingType easingType) {
        return easingType.name().replace('_', ' ');
    }

    static String trackEaseGroup(EasingType easingType) {
        String name = easingType.name();
        if (name.equals("LINEAR")) {
            return "Basic";
        }
        if (name.startsWith("IN_OUT_")) {
            return "Ease In/Out";
        }
        if (name.startsWith("IN_")) {
            return "Ease In";
        }
        if (name.startsWith("OUT_")) {
            return "Ease Out";
        }
        return "Basic";
    }

    static EasingType parseEasing(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return EasingType.LINEAR;
        }
        try {
            return EasingType.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EasingType.fromId(parseInt(trimmed));
        }
    }

    static String formatBeat(double beat) {
        return String.format(Locale.ROOT, "%.3f", beat);
    }

    static String formatDuration(long millis) {
        long safe = Math.max(0L, millis);
        long totalSeconds = safe / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        long hundredths = (safe % 1000L) / 10L;
        return String.format(Locale.ROOT, "%02d:%02d.%02d", minutes, seconds, hundredths);
    }

    static void keepPlayheadVisible(ChartEditorState state) {
        double playheadBeat = state.playheadBeat();
        double visibleStartBeat = state.visibleStartBeat();
        double beatsPerScreen = state.beatsPerScreen();
        if (playheadBeat < visibleStartBeat + 1.0) {
            visibleStartBeat = playheadBeat - 1.0;
        } else if (playheadBeat > visibleStartBeat + beatsPerScreen - 1.0) {
            visibleStartBeat = playheadBeat - beatsPerScreen + 1.0;
        }
        visibleStartBeat = Math.max(-64.0, visibleStartBeat);
        if (visibleStartBeat != state.visibleStartBeat()) {
            state.scrollWindow(visibleStartBeat - state.visibleStartBeat());
        }
    }

    static double clampPlaybackBeat(double beat) {
        if (!Double.isFinite(beat)) {
            return 0.0;
        }
        return Math.max(-64.0, beat);
    }

    static String trackEventSection(cn.frkovo.rhythmcv2.rmcChart.client.editor.model.EventLaneType eventType) {
        return switch (eventType) {
            case SPEED -> "Speed";
            case MOVE_X, MOVE_Y, MOVE_Z -> "Position";
            case ROT_X, ROT_Y, ROT_Z -> "Rotation";
            case SCALE_X, SCALE_Y, SCALE_Z -> "Scale";
        };
    }

    static double noteAxisValue(NoteData note, NoteAxis noteAxis) {
        return switch (noteAxis) {
            case X -> note.pos().x();
            case Y -> note.pos().y();
            case Z -> note.pos().z();
        };
    }

    static void setNoteAxisValue(NoteData note, NoteAxis noteAxis, double value) {
        switch (noteAxis) {
            case X -> note.pos().set(value, note.pos().y(), note.pos().z());
            case Y -> note.pos().set(note.pos().x(), value, note.pos().z());
            case Z -> note.pos().set(note.pos().x(), note.pos().y(), note.noteType() == NoteType.HOLD ? -1.0 : value);
        }
    }

    static double noteProjectionValue(NoteData note) {
        return Math.atan2(note.pos().y(), note.pos().x()) / Math.PI;
    }

    static double noteLaneProjectionRange(TrackData track) {
        double maxAbs = 1.0;
        if (track != null) {
            for (NoteData note : track.notes()) {
                maxAbs = Math.max(maxAbs, Math.abs(noteProjectionValue(note)) + 0.05);
            }
        }
        return maxAbs;
    }

    static int noteProjectionToScreenY(int rowTop, int laneHeight, double range, double projectionValue) {
        double progress = (projectionValue + range) / Math.max(0.0001, range * 2.0);
        progress = Math.max(0.0, Math.min(1.0, progress));
        return rowTop + laneHeight - 6 - (int) Math.round(progress * (laneHeight - 12));
    }

    static double noteProjectionScreenToValue(int rowTop, int laneHeight, double range, double mouseY) {
        double progress = 1.0 - ((mouseY - (rowTop + 6.0)) / Math.max(1.0, laneHeight - 12.0));
        progress = Math.max(0.0, Math.min(1.0, progress));
        return (progress * 2.0 - 1.0) * range;
    }

    static double noteLaneRange(TrackData track, NoteAxis noteAxis) {
        double maxAbs = ChartEditorScreen.NOTE_LANE_DEFAULT_RANGE;
        if (track != null && noteAxis != null) {
            for (NoteData note : track.notes()) {
                maxAbs = Math.max(maxAbs, Math.abs(noteAxisValue(note, noteAxis)) + 0.5);
            }
        }
        return maxAbs;
    }

    static int noteLaneValueToScreen(int rowTop, int laneHeight, double range, double axisValue) {
        double progress = (axisValue + range) / Math.max(0.0001, range * 2.0);
        progress = Math.max(0.0, Math.min(1.0, progress));
        return rowTop + laneHeight - 6 - (int) Math.round(progress * (laneHeight - 12));
    }

    static double noteLaneScreenToValue(int rowTop, int laneHeight, double range, double mouseY) {
        double progress = 1.0 - ((mouseY - (rowTop + 6.0)) / Math.max(1.0, laneHeight - 12.0));
        progress = Math.max(0.0, Math.min(1.0, progress));
        return (progress * 2.0 - 1.0) * range;
    }
}

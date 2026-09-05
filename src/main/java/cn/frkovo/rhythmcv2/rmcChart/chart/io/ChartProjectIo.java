package cn.frkovo.rhythmcv2.rmcChart.chart.io;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartProject;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.LevelData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.MetaData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.SongManifestData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.Vec3Data;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChartProjectIo {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Yaml YAML = createYaml();
    private static final String GENERATED_HOLD_NOTE_KEY = "chartMakerGeneratedHold";

    private ChartProjectIo() {
    }

    public static ChartProject load(Path projectPath) throws IOException {
        EnumMap<ChartDifficulty, LevelData> levels = new EnumMap<>(ChartDifficulty.class);
        SongManifestData manifest = loadManifest(projectPath.resolve("manifest.yml"));
        for (ChartDifficulty difficulty : ChartDifficulty.values()) {
            Path levelPath = projectPath.resolve(difficulty.fileName());
            levels.put(difficulty, Files.exists(levelPath) ? loadLevel(levelPath, difficulty) : LevelData.createDefault(difficulty));
        }
        return new ChartProject(projectPath, manifest, levels);
    }

    public static void save(ChartProject project) throws IOException {
        if (project.projectPath() == null) {
            throw new IOException("Project path is not set");
        }
        Files.createDirectories(project.projectPath());
        saveManifest(project.projectPath().resolve("manifest.yml"), project.manifest());
        for (ChartDifficulty difficulty : ChartDifficulty.values()) {
            saveLevel(project.projectPath().resolve(difficulty.fileName()), project.level(difficulty));
        }
    }

    public static String toManifestJson(SongManifestData manifest) {
        return GSON.toJson(manifest.toOrderedMap());
    }

    /**
     * Preview-upload variant: same as {@link #toManifestJson(SongManifestData)} plus the
     * per-upload {@code activeTrackId} inside the Editor-only {@code editor} block.
     * Never used for file saves (the active track is session state, not project data).
     */
    @SuppressWarnings("unchecked")
    public static String toManifestJson(SongManifestData manifest, int activeTrackId) {
        Map<String, Object> map = manifest.toOrderedMap();
        Object editor = map.get("editor");
        Map<String, Object> editorMap = editor instanceof Map
                ? (Map<String, Object>) editor
                : new LinkedHashMap<>();
        editorMap.put("activeTrackId", activeTrackId);
        map.put("editor", editorMap);
        return GSON.toJson(map);
    }

    public static String toLevelJson(LevelData level) {
        return GSON.toJson(toLevelJsonObject(level));
    }

    private static SongManifestData loadManifest(Path path) throws IOException {
        if (!Files.exists(path)) {
            return SongManifestData.createDefault();
        }
        SongManifestData manifest = SongManifestData.createDefault();
        try (Reader reader = Files.newBufferedReader(path)) {
            Object loaded = YAML.load(reader);
            if (!(loaded instanceof Map<?, ?> rawMap)) {
                return manifest;
            }
            manifest.setName(getString(rawMap, "name", manifest.name()));
            manifest.setComposer(getString(rawMap, "composer", manifest.composer()));
            manifest.setIcon(getString(rawMap, "icon", manifest.icon()));
            manifest.setAlias(getString(rawMap, "alias", manifest.alias()));
            manifest.setLength(getInt(rawMap, "length", manifest.length()));
            manifest.setRespackSha1(getString(rawMap, "respack_sha1", manifest.respackSha1()));
            manifest.setKey(getString(rawMap, "key", manifest.key()));
            manifest.setDescription(getString(rawMap, "description", manifest.description()));
            manifest.setSongId(getInt(rawMap, "song_id", manifest.songId()));
            manifest.setVersion(getString(rawMap, "version", manifest.version()));
            replaceStringList(manifest.comments(), getStringList(rawMap.get("comments")));
            replaceStringList(manifest.playerAlias(), getStringList(rawMap.get("player-alias")));
            replaceStringList(manifest.tags(), getStringList(rawMap.get("tags")));
            replaceMapList(manifest.unlockSong(), getMapList(rawMap.get("unlockSong")));
            replaceMapList(manifest.unlockWorld(), getMapList(rawMap.get("unlockWorld")));
            replaceMapList(manifest.unlockNether(), getMapList(rawMap.get("unlockNether")));
            replaceMapList(manifest.unlockVoid(), getMapList(rawMap.get("unlockVoid")));
            Object editor = rawMap.get("editor");
            if (editor instanceof Map<?, ?> editorMap) {
                manifest.setBeatsPerBar(getInt(editorMap, "beatsPerBar", manifest.beatsPerBar()));
            }
        }
        return manifest;
    }

    private static void saveManifest(Path path, SongManifestData manifest) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            YAML.dump(manifest.toOrderedMap(), writer);
        }
    }

    private static LevelData loadLevel(Path path, ChartDifficulty difficulty) throws IOException {
        if (!Files.exists(path)) {
            return LevelData.createDefault(difficulty);
        }
        JsonObject root = JsonParser.parseReader(Files.newBufferedReader(path)).getAsJsonObject();
        return parseLevel(root, difficulty);
    }

    public static LevelData parseLevel(JsonObject root, ChartDifficulty difficulty) {
        LevelData level = new LevelData(loadMeta(root.getAsJsonObject("meta"), difficulty));
        JsonArray tracks = root.has("tracks") && root.get("tracks").isJsonArray() ? root.getAsJsonArray("tracks") : new JsonArray();
        for (JsonElement element : tracks) {
            if (element.isJsonObject()) {
                level.tracks().add(loadTrack(element.getAsJsonObject()));
            }
        }
        if (level.tracks().isEmpty()) {
            level.tracks().add(TrackData.createDefault(0));
        }
        JsonArray effects = root.has("effects") && root.get("effects").isJsonArray() ? root.getAsJsonArray("effects") : new JsonArray();
        for (JsonElement element : effects) {
            if (element.isJsonObject()) {
                level.effects().add(loadEffect(element.getAsJsonObject()));
            }
        }
        return level;
    }

    private static void saveLevel(Path path, LevelData level) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(toLevelJsonObject(level), writer);
        }
    }

    private static JsonObject toLevelJsonObject(LevelData level) {
        JsonObject root = new JsonObject();
        root.add("meta", saveMeta(level.meta()));
        JsonArray tracks = new JsonArray();
        for (TrackData track : level.tracks()) {
            tracks.add(saveTrack(track));
        }
        root.add("tracks", tracks);
        JsonArray effects = new JsonArray();
        for (EffectData effect : level.effects()) {
            effects.add(saveEffect(effect));
        }
        root.add("effects", effects);
        return root;
    }

    private static MetaData loadMeta(JsonObject metaObject, ChartDifficulty difficulty) {
        if (metaObject == null) {
            return MetaData.createDefault(difficulty.defaultLevelId());
        }
        MetaData meta = new MetaData(
                getInt(metaObject, "uid", difficulty.defaultLevelId()),
                getString(metaObject, "initialArena", "default"),
                getLong(metaObject, "offset", 0L),
                getDouble(metaObject, "level", 1.0)
        );
        replaceStringList(meta.charters(), getStringList(metaObject.get("charters")));
        replaceStringList(meta.comments(), getStringList(metaObject.get("comments")));
        JsonArray bpms = metaObject.has("bpms") && metaObject.get("bpms").isJsonArray() ? metaObject.getAsJsonArray("bpms") : new JsonArray();
        if (bpms.isEmpty()) {
            meta.bpms().add(new BpmPoint(0.0, 120.0));
        } else {
            for (JsonElement element : bpms) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject bpmObject = element.getAsJsonObject();
                meta.bpms().add(new BpmPoint(getDouble(bpmObject, "beat", 0.0), getDouble(bpmObject, "bpm", 120.0)));
            }
        }
        return meta;
    }

    private static JsonObject saveMeta(MetaData meta) {
        JsonObject object = new JsonObject();
        object.addProperty("uid", meta.uid());
        object.addProperty("initialArena", meta.initialArena());
        object.addProperty("offset", meta.offset());
        object.addProperty("level", meta.level());
        object.add("charters", toStringArray(meta.charters()));
        object.add("comments", toStringArray(meta.comments()));
        JsonArray bpms = new JsonArray();
        for (BpmPoint bpm : meta.bpms()) {
            JsonObject bpmObject = new JsonObject();
            bpmObject.addProperty("beat", bpm.beat());
            bpmObject.addProperty("bpm", bpm.bpm());
            bpms.add(bpmObject);
        }
        object.add("bpms", bpms);
        return object;
    }

    private static TrackData loadTrack(JsonObject trackObject) {
        TrackData track = new TrackData(getInt(trackObject, "id", 0));
        track.setBeatDivision(getInt(trackObject, "beatDivision", 16));
        loadNumEvents(track.speedEvents(), trackObject.get("speedEvents"));
        loadNumEvents(track.xTransformEvents(), trackObject.get("xTransformEvents"));
        loadNumEvents(track.yTransformEvents(), trackObject.get("yTransformEvents"));
        loadNumEvents(track.zTransformEvents(), trackObject.get("zTransformEvents"));
        loadNumEvents(track.xRotateEvents(), trackObject.get("xRotateEvents"));
        loadNumEvents(track.yRotateEvents(), trackObject.get("yRotateEvents"));
        loadNumEvents(track.zRotateEvents(), trackObject.get("zRotateEvents"));
        loadNumEvents(track.xScaleEvents(), trackObject.get("xScaleEvents"));
        loadNumEvents(track.yScaleEvents(), trackObject.get("yScaleEvents"));
        loadNumEvents(track.zScaleEvents(), trackObject.get("zScaleEvents"));
        track.ensureDefaultEvents();
        JsonArray notes = trackObject.has("notes") && trackObject.get("notes").isJsonArray() ? trackObject.getAsJsonArray("notes") : new JsonArray();
        for (JsonElement element : notes) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject noteObject = element.getAsJsonObject();
            if (getBoolean(noteObject, GENERATED_HOLD_NOTE_KEY, false)) {
                continue;
            }
            track.notes().add(new NoteData(
                    NoteType.fromId(getInt(noteObject, "noteType", 0)),
                    getDouble(noteObject, "beat", 0.0),
                    getVec3(noteObject, "pos", Vec3Data.zero()),
                    getVec3(noteObject, "scale", Vec3Data.one()),
                    getVec3(noteObject, "rotation", Vec3Data.zero()),
                    getInt(noteObject, "holdGroup", -1),
                    getDouble(noteObject, "holdLengthBeats", 0.0)
            ));
        }
        return track;
    }

    private static JsonObject saveTrack(TrackData track) {
        JsonObject object = new JsonObject();
        object.addProperty("id", track.id());
        object.addProperty("beatDivision", track.beatDivision());
        object.add("speedEvents", toNumEventArray(track.speedEvents()));
        object.add("xTransformEvents", toNumEventArray(track.xTransformEvents()));
        object.add("yTransformEvents", toNumEventArray(track.yTransformEvents()));
        object.add("zTransformEvents", toNumEventArray(track.zTransformEvents()));
        object.add("xRotateEvents", toNumEventArray(track.xRotateEvents()));
        object.add("yRotateEvents", toNumEventArray(track.yRotateEvents()));
        object.add("zRotateEvents", toNumEventArray(track.zRotateEvents()));
        object.add("xScaleEvents", toNumEventArray(track.xScaleEvents()));
        object.add("yScaleEvents", toNumEventArray(track.yScaleEvents()));
        object.add("zScaleEvents", toNumEventArray(track.zScaleEvents()));
        JsonArray notes = new JsonArray();
        for (JsonObject noteObject : materializeNotesForSave(track)) {
            notes.add(noteObject);
        }
        object.add("notes", notes);
        return object;
    }

    private static List<JsonObject> materializeNotesForSave(TrackData track) {
        List<JsonObject> notes = new ArrayList<>();
        Map<Integer, Integer> holdGroupCounts = countHoldGroups(track);
        int nextHoldGroup = nextHoldGroupId(track);
        for (NoteData note : track.notes()) {
            int holdGroup = note.holdGroup();
            boolean shouldGenerateHoldChain = note.noteType() == NoteType.HOLD
                    && note.holdLengthBeats() > 0.0
                    && (holdGroup < 0 || holdGroupCounts.getOrDefault(holdGroup, 0) <= 1);
            if (shouldGenerateHoldChain && holdGroup < 0) {
                holdGroup = nextHoldGroup++;
            }
            notes.add(toNoteObject(note, holdGroup, false));
            if (shouldGenerateHoldChain) {
                addGeneratedHoldTailNotes(notes, track, note, holdGroup);
            }
        }
        notes.sort((left, right) -> Double.compare(getDouble(left, "beat", 0.0), getDouble(right, "beat", 0.0)));
        return notes;
    }

    private static Map<Integer, Integer> countHoldGroups(TrackData track) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (NoteData note : track.notes()) {
            if (note.noteType() == NoteType.HOLD && note.holdGroup() >= 0) {
                counts.merge(note.holdGroup(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static int nextHoldGroupId(TrackData track) {
        int max = -1;
        for (NoteData note : track.notes()) {
            max = Math.max(max, note.holdGroup());
        }
        return max + 1;
    }

    private static void addGeneratedHoldTailNotes(List<JsonObject> notes, TrackData track, NoteData source, int holdGroup) {
        double step = trackGridStep(track);
        double endBeat = source.beat() + source.holdLengthBeats();
        double beat = source.beat() + step;
        while (beat < endBeat - 1.0E-6) {
            notes.add(toGeneratedHoldNoteObject(source, holdGroup, beat));
            beat += step;
        }
        notes.add(toGeneratedHoldNoteObject(source, holdGroup, endBeat));
    }

    private static JsonObject toGeneratedHoldNoteObject(NoteData source, int holdGroup, double beat) {
        JsonObject object = toNoteObject(source, holdGroup, true);
        object.addProperty("beat", beat);
        object.addProperty("holdLengthBeats", 0.0);
        return object;
    }

    private static JsonObject toNoteObject(NoteData note, int holdGroup, boolean generatedHoldTail) {
        JsonObject noteObject = new JsonObject();
        noteObject.addProperty("noteType", note.noteType().id());
        noteObject.addProperty("beat", note.beat());
        noteObject.add("pos", toVec3Array(note.pos()));
        noteObject.add("scale", toVec3Array(note.scale()));
        noteObject.add("rotation", toVec3Array(note.rotation()));
        noteObject.addProperty("holdGroup", holdGroup);
        noteObject.addProperty("holdLengthBeats", generatedHoldTail ? 0.0 : note.holdLengthBeats());
        if (generatedHoldTail) {
            noteObject.addProperty(GENERATED_HOLD_NOTE_KEY, true);
        }
        return noteObject;
    }

    private static double trackGridStep(TrackData track) {
        int division = track == null ? 16 : track.beatDivision();
        return Math.max(1.0 / 64.0, 4.0 / Math.max(1, division));
    }

    private static EffectData loadEffect(JsonObject effectObject) {
        JsonObject properties = effectObject.has("properties") && effectObject.get("properties").isJsonObject()
                ? effectObject.getAsJsonObject("properties").deepCopy() : new JsonObject();
        return new EffectData(
                EffectType.fromName(getString(effectObject, "effectType", "TEXT_DISPLAY")),
                getDouble(effectObject, "beat", 0.0),
                properties
        );
    }

    private static JsonObject saveEffect(EffectData effect) {
        JsonObject object = new JsonObject();
        object.addProperty("effectType", effect.effectType().name());
        object.addProperty("beat", effect.beat());
        object.add("properties", effect.properties() == null ? new JsonObject() : effect.properties().deepCopy());
        return object;
    }

    private static void loadNumEvents(List<NumEventData> target, JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return;
        }
        for (JsonElement child : element.getAsJsonArray()) {
            if (!child.isJsonObject()) {
                continue;
            }
            JsonObject object = child.getAsJsonObject();
            target.add(new NumEventData(
                    getDouble(object, "startBeat", 0.0),
                    getDouble(object, "endBeat", 0.0),
                    getDouble(object, "startValue", 0.0),
                    getDouble(object, "endValue", 0.0),
                    EasingType.fromId(getInt(object, "easingType", 0))
            ));
        }
    }

    private static JsonArray toNumEventArray(List<NumEventData> events) {
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
        return array;
    }

    private static JsonArray toVec3Array(Vec3Data vec3) {
        JsonArray array = new JsonArray();
        array.add(vec3.x());
        array.add(vec3.y());
        array.add(vec3.z());
        return array;
    }

    private static Vec3Data getVec3(JsonObject object, String key, Vec3Data fallback) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return fallback.copy();
        }
        JsonArray array = object.getAsJsonArray(key);
        if (array.size() < 3) {
            return fallback.copy();
        }
        return new Vec3Data(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    private static JsonArray toStringArray(List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static String getString(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static int getInt(Map<?, ?> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static List<String> getStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry != null) {
                result.add(String.valueOf(entry));
            }
        }
        return result;
    }

    private static List<Map<String, Object>> getMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                for (Map.Entry<?, ?> mapEntry : map.entrySet()) {
                    copy.put(String.valueOf(mapEntry.getKey()), mapEntry.getValue());
                }
                result.add(copy);
            }
        }
        return result;
    }

    private static void replaceStringList(List<String> target, List<String> source) {
        target.clear();
        target.addAll(source);
    }

    private static void replaceMapList(List<Map<String, Object>> target, List<Map<String, Object>> source) {
        target.clear();
        target.addAll(source);
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static long getLong(JsonObject object, String key, long fallback) {
        return object.has(key) ? object.get(key).getAsLong() : fallback;
    }

    private static double getDouble(JsonObject object, String key, double fallback) {
        return object.has(key) ? object.get(key).getAsDouble() : fallback;
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        return object.has(key) ? object.get(key).getAsBoolean() : fallback;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        return object.has(key) ? object.get(key).getAsString() : fallback;
    }

    private static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        return new Yaml(options);
    }
}

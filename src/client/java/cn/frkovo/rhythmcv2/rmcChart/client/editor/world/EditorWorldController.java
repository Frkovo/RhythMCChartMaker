package cn.frkovo.rhythmcv2.rmcChart.client.editor.world;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorState;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.EditorSelection;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.server.integrated.IntegratedServer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EditorWorldController {
    private static final String TAG_DYNAMIC = "rmc_chart_dynamic";
    private static final String TAG_NOTE = "rmc_chart_note";
    private static final String TAG_EFFECT = "rmc_chart_effect";
    private static final String TAG_STAGE = "rmc_chart_stage";
    private static final String TAG_GIZMO = "rmc_chart_gizmo";

    private static final double NOTE_CENTER_X = 0.5;
    private static final double NOTE_BASE_Y = 65.8;
    private static final double NOTE_MIN_Z = -30.0;
    private static final double NOTE_MAX_Z = 30.0;
    private static final double LANE_SPACING = 5.0;

    private static final double STAGE_CENTER_X = 23.5;
    private static final double STAGE_CENTER_Y = 67.0;
    private static final double STAGE_CENTER_Z = 0.5;

    private final ChartEditorState state;
    private Manipulator activeManipulator = Manipulator.MOVE;

    private int lastRevision = -1;
    private double lastVisibleStartBeat = Double.NaN;
    private double lastBeatsPerScreen = Double.NaN;
    private ChartDifficulty lastDifficulty = null;
    private Object lastSelectionRef = null;

    public EditorWorldController(ChartEditorState state) {
        this.state = state;
    }

    public void tick(MinecraftClient client) {
        if (!isEditorWorldReady(client)) {
            return;
        }

        Object selectionRef = currentSelectionRef();
        boolean changed = lastRevision != state.revision()
                || Double.compare(lastVisibleStartBeat, state.visibleStartBeat()) != 0
                || Double.compare(lastBeatsPerScreen, state.beatsPerScreen()) != 0
                || lastDifficulty != state.activeDifficulty()
                || lastSelectionRef != selectionRef;

        if (changed) {
            sync(client.getServer());
        }
    }

    public void syncNow(MinecraftClient client) {
        if (!isEditorWorldReady(client)) {
            return;
        }
        sync(client.getServer());
    }

    public boolean pickLookTarget(MinecraftClient client) {
        if (!isEditorWorldReady(client)) {
            return false;
        }

        Entity entity = client.targetedEntity;
        if (entity == null) {
            state.setStatus("No display entity under crosshair");
            return false;
        }

        Set<String> tags = entity.getCommandTags();
        for (String tag : tags) {
            if (tag.startsWith("rmc_gizmo_")) {
                activeManipulator = Manipulator.fromTag(tag);
            }
            if (tag.startsWith("rmc_note_t")) {
                if (selectNoteFromTag(tag)) {
                    state.setStatus("Selected note from world preview [" + activeManipulator.label + "]");
                    sync(client.getServer());
                    return true;
                }
            }
            if (tag.startsWith("rmc_fx_i")) {
                if (selectEffectFromTag(tag)) {
                    state.setStatus("Selected effect from world preview [" + activeManipulator.label + "]");
                    sync(client.getServer());
                    return true;
                }
            }
        }

        state.setStatus("Target is not a RhythMC editor entity");
        return false;
    }

    public boolean dragSelection(MinecraftClient client, double deltaX, double deltaY, int button, boolean shiftDown) {
        if (!isEditorWorldReady(client)) {
            return false;
        }

        EditorSelection selection = state.selection();
        if (selection.kind() == EditorSelection.Kind.NOTE && selection.note() != null && selection.track() != null) {
            dragNote(selection.note(), button, deltaX, deltaY, shiftDown);
            state.markDirty();
            sync(client.getServer());
            return true;
        }
        if (selection.kind() == EditorSelection.Kind.EFFECT && selection.effect() != null) {
            dragEffect(selection.effect(), button, deltaX, deltaY, shiftDown);
            state.markDirty();
            sync(client.getServer());
            return true;
        }
        return false;
    }

    private void dragNote(NoteData note, int button, double deltaX, double deltaY, boolean shiftDown) {
        switch (activeManipulator) {
            case MOVE -> {
                if (button == 0) {
                    note.pos().set(
                            note.pos().x() + deltaX * 0.03,
                            note.pos().y() - deltaY * 0.03,
                            note.pos().z()
                    );
                } else if (shiftDown) {
                    note.setBeat(Math.max(0.0, note.beat() + deltaX * state.beatsPerScreen() / 900.0));
                } else {
                    note.pos().set(note.pos().x(), note.pos().y(), note.pos().z() - deltaY * 0.03);
                }
            }
            case SCALE -> {
                if (shiftDown || button != 0) {
                    note.scale().set(note.scale().x(), note.scale().y(), Math.max(0.1, note.scale().z() - deltaY * 0.01));
                } else {
                    note.scale().set(
                            Math.max(0.1, note.scale().x() + deltaX * 0.01),
                            Math.max(0.1, note.scale().y() - deltaY * 0.01),
                            note.scale().z()
                    );
                }
            }
            case ROTATE -> {
                if (shiftDown || button != 0) {
                    note.rotation().set(note.rotation().x() - deltaY * 0.4, note.rotation().y() + deltaX * 0.4, note.rotation().z());
                } else {
                    note.rotation().set(note.rotation().x(), note.rotation().y(), note.rotation().z() + deltaX * 0.4);
                }
            }
        }
    }

    private void dragEffect(EffectData effect, int button, double deltaX, double deltaY, boolean shiftDown) {
        JsonObject properties = effect.properties();
        if (properties == null) {
            properties = new JsonObject();
            effect.setProperties(properties);
        }

        double x = getDouble(properties, "x", 0.0);
        double y = getDouble(properties, "y", 0.0);
        double z = getDouble(properties, "z", 0.0);

        switch (activeManipulator) {
            case MOVE -> {
                if (button == 0) {
                    properties.addProperty("x", x + deltaX * 0.03);
                    properties.addProperty("y", y - deltaY * 0.03);
                } else if (shiftDown) {
                    effect.setBeat(Math.max(0.0, effect.beat() + deltaX * state.beatsPerScreen() / 900.0));
                } else {
                    properties.addProperty("z", z - deltaY * 0.03);
                }
            }
            case SCALE -> {
                double sx = getDouble(properties, "scaleX", 1.0);
                double sy = getDouble(properties, "scaleY", 1.0);
                double sz = getDouble(properties, "scaleZ", 1.0);
                if (shiftDown || button != 0) {
                    properties.addProperty("scaleZ", Math.max(0.1, sz - deltaY * 0.01));
                } else {
                    properties.addProperty("scaleX", Math.max(0.1, sx + deltaX * 0.01));
                    properties.addProperty("scaleY", Math.max(0.1, sy - deltaY * 0.01));
                }
            }
            case ROTATE -> {
                double rx = getDouble(properties, "rotationX", 0.0);
                double ry = getDouble(properties, "rotationY", 0.0);
                double rz = getDouble(properties, "rotationZ", 0.0);
                if (shiftDown || button != 0) {
                    properties.addProperty("rotationX", rx - deltaY * 0.4);
                    properties.addProperty("rotationY", ry + deltaX * 0.4);
                } else {
                    properties.addProperty("rotationZ", rz + deltaX * 0.4);
                }
            }
        }
    }

    private void sync(IntegratedServer server) {
        if (server == null) {
            return;
        }

        server.execute(() -> {
            run(server, "kill @e[tag=" + TAG_DYNAMIC + "]");
            spawnNotes(server);
            spawnEffectTimeline(server);
            spawnActiveStageEffects(server);
            spawnGizmos(server);
            spawnSelectionHint(server);
        });

        lastRevision = state.revision();
        lastVisibleStartBeat = state.visibleStartBeat();
        lastBeatsPerScreen = state.beatsPerScreen();
        lastDifficulty = state.activeDifficulty();
        lastSelectionRef = currentSelectionRef();
    }

    private void spawnNotes(IntegratedServer server) {
        List<TrackData> tracks = state.tracks();
        for (TrackData track : tracks) {
            for (int noteIndex = 0; noteIndex < track.notes().size(); noteIndex++) {
                NoteData note = track.notes().get(noteIndex);
                if (!isNoteVisible(note, track == state.selection().track() && note == state.selection().note())) {
                    continue;
                }

                Vec3 worldPos = noteWorldPosition(track, note);
                String block = noteBlockName(note, track == state.selection().track() && note == state.selection().note());
                String tag = noteTag(track, noteIndex);
                double scaleX = Math.max(0.35, Math.abs(note.scale().x()) * 0.8);
                double scaleZ = Math.max(0.35, Math.abs(note.scale().y()) * 0.8);
                String rotation = rotationTag(note.rotation().z());

                run(server, String.format(Locale.ROOT,
                        "summon minecraft:block_display %.3f %.3f %.3f {Tags:[\"%s\",\"%s\",\"%s\"],block_state:{Name:\"minecraft:%s\",Properties:{axis:\"%s\"}},transformation:{scale:[%.3ff,0.180f,%.3ff]},brightness:{block:15,sky:15}}",
                        worldPos.x,
                        worldPos.y,
                        worldPos.z,
                        TAG_DYNAMIC,
                        TAG_NOTE,
                        tag,
                        block,
                        rotation,
                        scaleX,
                        scaleZ));
            }
        }
    }

    private void spawnEffectTimeline(IntegratedServer server) {
        for (int effectIndex = 0; effectIndex < state.level().effects().size(); effectIndex++) {
            EffectData effect = state.level().effects().get(effectIndex);
            boolean selected = effect == state.selection().effect();
            if (isEffectVisible(effect) || selected) {
                Vec3 timelinePos = effectTimelineWorldPosition(effect);
                run(server, summonTextDisplay(
                        timelinePos,
                        List.of(TAG_DYNAMIC, TAG_EFFECT, effectTag(effectIndex)),
                        shortEffectLabel(effect),
                        selected ? "gold" : "white",
                        true
                ));
            }
        }
    }

    private void spawnActiveStageEffects(IntegratedServer server) {
        StagePreviewState preview = buildStagePreviewState();

        for (int effectIndex = 0; effectIndex < state.level().effects().size(); effectIndex++) {
            EffectData effect = state.level().effects().get(effectIndex);
            if (effect == state.selection().effect() && !preview.displayStates.containsKey(effectId(effect))) {
                preview.displayStates.put(effectId(effect), StageDisplayState.fromEffect(effect));
            }
        }

        for (Map.Entry<String, StageDisplayState> entry : preview.displayStates.entrySet()) {
            StageDisplayState display = entry.getValue();
            Vec3 stagePos = new Vec3(STAGE_CENTER_X + display.x, STAGE_CENTER_Y + display.y, STAGE_CENTER_Z - display.z);
            run(server, summonTextDisplay(
                    stagePos,
                    List.of(TAG_DYNAMIC, TAG_EFFECT, TAG_STAGE, effectTag(display.effectIndex)),
                    display.text,
                    display.color,
                    false,
                    display.scaleText()
            ));
        }

        for (int index = 0; index < preview.statusLines.size(); index++) {
            run(server, summonTextDisplay(
                    new Vec3(STAGE_CENTER_X, 72.0 - index * 0.8, -18.0),
                    List.of(TAG_DYNAMIC, TAG_EFFECT, "rmc_stage_status"),
                    preview.statusLines.get(index),
                    "yellow",
                    true,
                    0.75
            ));
        }
    }

    private void spawnGizmos(IntegratedServer server) {
        EditorSelection selection = state.selection();
        if (selection.kind() == EditorSelection.Kind.NOTE && selection.note() != null && selection.track() != null) {
            Vec3 center = noteWorldPosition(selection.track(), selection.note());
            spawnGizmoSet(server, center, noteTag(selection.track(), selection.track().notes().indexOf(selection.note())));
        } else if (selection.kind() == EditorSelection.Kind.EFFECT && selection.effect() != null) {
            int effectIndex = state.level().effects().indexOf(selection.effect());
            if (effectIndex >= 0) {
                spawnGizmoSet(server, stageWorldPosition(selection.effect()), effectTag(effectIndex));
            }
        }
    }

    private void spawnGizmoSet(IntegratedServer server, Vec3 center, String ownerTag) {
        spawnGizmo(server, new Vec3(center.x + 1.4, center.y + 0.8, center.z), ownerTag, Manipulator.MOVE, "MOVE", "green");
        spawnGizmo(server, new Vec3(center.x, center.y + 1.6, center.z), ownerTag, Manipulator.ROTATE, "ROT", "light_purple");
        spawnGizmo(server, new Vec3(center.x - 1.4, center.y + 0.8, center.z), ownerTag, Manipulator.SCALE, "SCALE", "aqua");
    }

    private void spawnGizmo(IntegratedServer server, Vec3 pos, String ownerTag, Manipulator manipulator, String text, String color) {
        run(server, summonTextDisplay(
                pos,
                List.of(TAG_DYNAMIC, TAG_GIZMO, ownerTag, manipulator.tag),
                text,
                manipulator == activeManipulator ? "gold" : color,
                true,
                0.7
        ));
    }

    private void spawnSelectionHint(IntegratedServer server) {
        EditorSelection selection = state.selection();
        String hint;
        if (selection.kind() == EditorSelection.Kind.NOTE && selection.note() != null) {
            hint = "Selected note [" + activeManipulator.label + "]: click entity/gizmo to pick, drag in preview";
        } else if (selection.kind() == EditorSelection.Kind.EFFECT && selection.effect() != null) {
            hint = "Selected effect [" + activeManipulator.label + "]: click entity/gizmo to pick, drag in preview";
        } else {
            hint = "Click a note, text, or gizmo in the world preview to start editing.";
        }

        run(server, summonTextDisplay(
                new Vec3(0.5, 70.8, -30.5),
                List.of(TAG_DYNAMIC, "rmc_chart_hint"),
                hint,
                "yellow",
                true
        ));
    }

    private boolean selectNoteFromTag(String tag) {
        String[] pieces = tag.substring("rmc_note_t".length()).split("_i");
        if (pieces.length != 2) {
            return false;
        }
        int trackId = Integer.parseInt(pieces[0]);
        int noteIndex = Integer.parseInt(pieces[1]);
        for (TrackData track : state.tracks()) {
            if (track.id() == trackId && noteIndex >= 0 && noteIndex < track.notes().size()) {
                state.setSelection(EditorSelection.note(track, track.notes().get(noteIndex)));
                return true;
            }
        }
        return false;
    }

    private boolean selectEffectFromTag(String tag) {
        int index = Integer.parseInt(tag.substring("rmc_fx_i".length()));
        if (index >= 0 && index < state.level().effects().size()) {
            state.setSelection(EditorSelection.effect(state.level().effects().get(index)));
            return true;
        }
        return false;
    }

    private boolean isEditorWorldReady(MinecraftClient client) {
        return client != null && client.player != null && client.world != null && client.isIntegratedServerRunning();
    }

    private boolean isNoteVisible(NoteData note, boolean selected) {
        if (selected) {
            return true;
        }
        double margin = Math.max(1.0, state.beatsPerScreen() * 0.05);
        return note.beat() >= state.visibleStartBeat() - margin && note.beat() <= state.visibleEndBeat() + margin;
    }

    private boolean isEffectVisible(EffectData effect) {
        double margin = Math.max(1.0, state.beatsPerScreen() * 0.05);
        return effect.beat() >= state.visibleStartBeat() - margin && effect.beat() <= state.visibleEndBeat() + margin;
    }

    private Vec3 noteWorldPosition(TrackData track, NoteData note) {
        double lane = trackLaneCenter(track);
        double normalized = normalizeBeat(note.beat());
        double z = NOTE_MAX_Z - normalized * (NOTE_MAX_Z - NOTE_MIN_Z);
        return new Vec3(lane + note.pos().x(), NOTE_BASE_Y + note.pos().y(), z);
    }

    private Vec3 effectTimelineWorldPosition(EffectData effect) {
        double normalized = normalizeBeat(effect.beat());
        double z = NOTE_MAX_Z - normalized * (NOTE_MAX_Z - NOTE_MIN_Z);
        return new Vec3(17.5, 66.4, z);
    }

    private Vec3 stageWorldPosition(EffectData effect) {
        JsonObject properties = effect.properties();
        double x = getDouble(properties, "x", 0.0);
        double y = getDouble(properties, "y", 0.0);
        double z = getDouble(properties, "z", 0.0);
        return new Vec3(STAGE_CENTER_X + x, STAGE_CENTER_Y + y, STAGE_CENTER_Z - z);
    }

    private double trackLaneCenter(TrackData track) {
        int index = state.trackIndex(track);
        int count = Math.max(1, state.tracks().size());
        double half = (count - 1) * LANE_SPACING / 2.0;
        return NOTE_CENTER_X - half + index * LANE_SPACING;
    }

    private double normalizeBeat(double beat) {
        double span = Math.max(1.0, state.beatsPerScreen());
        double clamped = Math.max(state.visibleStartBeat(), Math.min(state.visibleEndBeat(), beat));
        return (clamped - state.visibleStartBeat()) / span;
    }

    private String noteBlockName(NoteData note, boolean selected) {
        if (selected) {
            return "gold_block";
        }
        return switch (note.noteType()) {
            case TAP -> "light_blue_stained_glass";
            case LOOK -> "yellow_stained_glass";
            case HOLD -> "lime_stained_glass";
            case DODGE -> "red_stained_glass";
        };
    }

    private String noteTag(TrackData track, int noteIndex) {
        return "rmc_note_t" + track.id() + "_i" + noteIndex;
    }

    private String effectTag(int effectIndex) {
        return "rmc_fx_i" + effectIndex;
    }

    private String shortEffectLabel(EffectData effect) {
        return effect.effectType().name() + " @ " + String.format(Locale.ROOT, "%.2f", effect.beat());
    }

    private StagePreviewState buildStagePreviewState() {
        StagePreviewState preview = new StagePreviewState();
        for (int effectIndex = 0; effectIndex < state.level().effects().size(); effectIndex++) {
            EffectData effect = state.level().effects().get(effectIndex);
            if (effect.beat() > state.playheadBeat()) {
                continue;
            }
            JsonObject properties = effect.properties();
            final int currentEffectIndex = effectIndex;
            switch (effect.effectType()) {
                case TEXT_DISPLAY -> preview.displayStates.put(effectId(effect), StageDisplayState.fromEffect(effect, currentEffectIndex));
                case TEXT_DISPLAY_EFFECT -> {
                    StageDisplayState current = preview.displayStates.computeIfAbsent(effectId(effect), key -> StageDisplayState.fromEffect(effect, currentEffectIndex));
                    current.apply(properties, currentEffectIndex);
                }
                case TEXT_DISPLAY_SYNC_TRACK -> preview.statusLines.add("SYNC " + effectId(effect) + " -> track " + propertyString(properties, "trackId", "?"));
                case TEXT_DISPLAY_DESYNC_TRACK -> preview.statusLines.add("DESYNC " + effectId(effect));
                case TEXT_DISPLAY_REMOVE -> preview.displayStates.remove(effectId(effect));
                case TITLE -> preview.statusLines.add("TITLE: " + propertyString(properties, "text", propertyString(properties, "title", "Title")));
                case MESSAGE -> preview.statusLines.add("MESSAGE: " + propertyString(properties, "text", propertyString(properties, "message", "Message")));
                case GLOW_COLOR -> preview.statusLines.add("GLOW_COLOR: " + propertyString(properties, "color", "white"));
                case HIDE_NOTES -> preview.statusLines.add("HIDE_NOTES: " + propertyString(properties, "state", "enabled"));
                case ARENA, WEATHER, TIME, EFFECT, CLEAR_EFFECT, FIREWORK, HOLOGRAM, REMOVE_HOLOGRAM ->
                        preview.statusLines.add(effect.effectType().name() + ": " + effectSummary(properties));
                default -> {
                }
            }
        }
        return preview;
    }

    private String effectId(EffectData effect) {
        JsonObject properties = effect.properties();
        return propertyString(properties, "id", propertyString(properties, "textId", "display"));
    }

    private String effectSummary(JsonObject properties) {
        if (properties == null || properties.entrySet().isEmpty()) {
            return "(no properties)";
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, com.google.gson.JsonElement> entry : properties.entrySet()) {
            if (count++ > 0) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : entry.getValue());
            if (count >= 3) {
                break;
            }
        }
        return builder.toString();
    }

    private String propertyString(JsonObject properties, String key, String fallback) {
        if (properties != null && properties.has(key) && properties.get(key).isJsonPrimitive()) {
            return properties.get(key).getAsString();
        }
        return fallback;
    }

    private double getDouble(JsonObject properties, String key, double fallback) {
        if (properties != null && properties.has(key) && properties.get(key).isJsonPrimitive()) {
            try {
                return properties.get(key).getAsDouble();
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private String summonTextDisplay(Vec3 pos, List<String> tags, String text, String color, boolean through, double scale) {
        StringBuilder tagBuilder = new StringBuilder();
        for (int index = 0; index < tags.size(); index++) {
            if (index > 0) {
                tagBuilder.append(',');
            }
            tagBuilder.append('"').append(tags.get(index)).append('"');
        }

        String jsonText = "{\"text\":\"" + escapeText(text) + "\",\"color\":\"" + color + "\"}";
        return String.format(Locale.ROOT,
                "summon minecraft:text_display %.3f %.3f %.3f {Tags:[%s],billboard:\"center\",background:0,see_through:%sb,text:'%s',transformation:{scale:[%.3ff,%.3ff,%.3ff]},brightness:{block:15,sky:15}}",
                pos.x,
                pos.y,
                pos.z,
                tagBuilder,
                through ? "1" : "0",
                jsonText,
                scale,
                scale,
                scale);
    }

    private String summonTextDisplay(Vec3 pos, List<String> tags, String text, String color, boolean through) {
        return summonTextDisplay(pos, tags, text, color, through, 1.0);
    }

    private String escapeText(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\u0027")
                .replace("\n", " ");
    }

    private Object currentSelectionRef() {
        EditorSelection selection = state.selection();
        return switch (selection.kind()) {
            case NOTE -> selection.note();
            case EFFECT -> selection.effect();
            case TRACK -> selection.track();
            case BPM -> selection.bpm();
            default -> selection.kind();
        };
    }

    private void run(IntegratedServer server, String command) {
        server.getCommandManager().parseAndExecute(server.getCommandSource(), command);
    }

    private record Vec3(double x, double y, double z) {
    }

    private record StagePreviewState(Map<String, StageDisplayState> displayStates, List<String> statusLines) {
        private StagePreviewState() {
            this(new LinkedHashMap<>(), new java.util.ArrayList<>());
        }
    }

    private static final class StageDisplayState {
        private final String id;
        private int effectIndex;
        private String text;
        private double x;
        private double y;
        private double z;
        private double scaleX;
        private double scaleY;
        private double scaleZ;
        private String color;

        private StageDisplayState(String id, int effectIndex, String text, double x, double y, double z, double scaleX, double scaleY, double scaleZ, String color) {
            this.id = id;
            this.effectIndex = effectIndex;
            this.text = text;
            this.x = x;
            this.y = y;
            this.z = z;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.color = color;
        }

        private static StageDisplayState fromEffect(EffectData effect) {
            return fromEffect(effect, -1);
        }

        private static StageDisplayState fromEffect(EffectData effect, int effectIndex) {
            JsonObject properties = effect.properties();
            return new StageDisplayState(
                    properties != null && properties.has("id") ? properties.get("id").getAsString() : "display",
                    effectIndex,
                    properties != null && properties.has("text") ? properties.get("text").getAsString() : properties != null && properties.has("content") ? properties.get("content").getAsString() : "Text Display",
                    properties != null && properties.has("x") ? properties.get("x").getAsDouble() : 0.0,
                    properties != null && properties.has("y") ? properties.get("y").getAsDouble() : 0.0,
                    properties != null && properties.has("z") ? properties.get("z").getAsDouble() : 0.0,
                    properties != null && properties.has("scaleX") ? properties.get("scaleX").getAsDouble() : 1.0,
                    properties != null && properties.has("scaleY") ? properties.get("scaleY").getAsDouble() : 1.0,
                    properties != null && properties.has("scaleZ") ? properties.get("scaleZ").getAsDouble() : 1.0,
                    properties != null && properties.has("color") ? properties.get("color").getAsString() : "aqua"
            );
        }

        private void apply(JsonObject properties, int effectIndex) {
            this.effectIndex = effectIndex;
            if (properties == null) {
                return;
            }
            if (properties.has("text")) {
                this.text = properties.get("text").getAsString();
            }
            if (properties.has("content")) {
                this.text = properties.get("content").getAsString();
            }
            if (properties.has("x")) {
                this.x = properties.get("x").getAsDouble();
            }
            if (properties.has("y")) {
                this.y = properties.get("y").getAsDouble();
            }
            if (properties.has("z")) {
                this.z = properties.get("z").getAsDouble();
            }
            if (properties.has("scaleX")) {
                this.scaleX = properties.get("scaleX").getAsDouble();
            }
            if (properties.has("scaleY")) {
                this.scaleY = properties.get("scaleY").getAsDouble();
            }
            if (properties.has("scaleZ")) {
                this.scaleZ = properties.get("scaleZ").getAsDouble();
            }
            if (properties.has("color")) {
                this.color = properties.get("color").getAsString();
            }
        }

        private double scaleText() {
            return Math.max(0.45, (scaleX + scaleY + scaleZ) / 3.0);
        }
    }

    private enum Manipulator {
        MOVE("rmc_gizmo_move", "move"),
        ROTATE("rmc_gizmo_rotate", "rotate"),
        SCALE("rmc_gizmo_scale", "scale");

        private final String tag;
        private final String label;

        Manipulator(String tag, String label) {
            this.tag = tag;
            this.label = label;
        }

        private static Manipulator fromTag(String tag) {
            for (Manipulator value : values()) {
                if (value.tag.equals(tag)) {
                    return value;
                }
            }
            return MOVE;
        }
    }

    private String rotationTag(double rotationZ) {
        double normalized = Math.abs(rotationZ % 180.0);
        if (normalized > 135.0 || normalized < 45.0) {
            return "y";
        }
        return "x";
    }
}

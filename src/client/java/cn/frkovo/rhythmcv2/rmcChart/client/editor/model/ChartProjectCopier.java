package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.*;
import com.google.gson.JsonObject;

import java.util.*;

public final class ChartProjectCopier {
    private ChartProjectCopier() {
    }

    public static ChartProject copyProject(ChartProject source) {
        EnumMap<ChartDifficulty, LevelData> levels = new EnumMap<>(ChartDifficulty.class);
        for (Map.Entry<ChartDifficulty, LevelData> entry : source.levels().entrySet()) {
            levels.put(entry.getKey(), copyLevel(entry.getValue()));
        }
        return new ChartProject(source.projectPath(), copyManifest(source.manifest()), levels);
    }

    public static SongManifestData copyManifest(SongManifestData source) {
        SongManifestData manifest = SongManifestData.createDefault();
        manifest.setName(source.name());
        manifest.setComposer(source.composer());
        manifest.setIcon(source.icon());
        manifest.setAlias(source.alias());
        manifest.setLength(source.length());
        manifest.setRespackSha1(source.respackSha1());
        manifest.setKey(source.key());
        manifest.setDescription(source.description());
        manifest.setSongId(source.songId());
        manifest.setVersion(source.version());
        manifest.comments().addAll(source.comments());
        manifest.playerAlias().addAll(source.playerAlias());
        manifest.tags().addAll(source.tags());
        copyMapList(source.unlockSong(), manifest.unlockSong());
        copyMapList(source.unlockWorld(), manifest.unlockWorld());
        copyMapList(source.unlockNether(), manifest.unlockNether());
        copyMapList(source.unlockVoid(), manifest.unlockVoid());
        return manifest;
    }

    private static void copyMapList(List<Map<String, Object>> source, List<Map<String, Object>> target) {
        target.clear();
        for (Map<String, Object> map : source) {
            target.add(new LinkedHashMap<>(map));
        }
    }

    public static LevelData copyLevel(LevelData source) {
        LevelData level = new LevelData(copyMeta(source.meta()));
        for (TrackData track : source.tracks()) {
            level.tracks().add(copyTrack(track));
        }
        for (EffectData effect : source.effects()) {
            level.effects().add(copyEffect(effect));
        }
        return level;
    }

    public static MetaData copyMeta(MetaData source) {
        MetaData meta = new MetaData(source.uid(), source.initialArena(), source.offset(), source.level());
        meta.charters().addAll(source.charters());
        meta.comments().addAll(source.comments());
        for (BpmPoint bpm : source.bpms()) {
            meta.bpms().add(copyBpm(bpm));
        }
        return meta;
    }

    public static TrackData copyTrack(TrackData source) {
        TrackData track = new TrackData(source.id());
        track.setBeatDivision(source.beatDivision());
        copyEvents(source.speedEvents(), track.speedEvents());
        copyEvents(source.xTransformEvents(), track.xTransformEvents());
        copyEvents(source.yTransformEvents(), track.yTransformEvents());
        copyEvents(source.zTransformEvents(), track.zTransformEvents());
        copyEvents(source.xRotateEvents(), track.xRotateEvents());
        copyEvents(source.yRotateEvents(), track.yRotateEvents());
        copyEvents(source.zRotateEvents(), track.zRotateEvents());
        copyEvents(source.xScaleEvents(), track.xScaleEvents());
        copyEvents(source.yScaleEvents(), track.yScaleEvents());
        copyEvents(source.zScaleEvents(), track.zScaleEvents());
        for (NoteData note : source.notes()) {
            track.notes().add(copyNote(note));
        }
        track.ensureDefaultEvents();
        return track;
    }

    private static void copyEvents(List<NumEventData> source, List<NumEventData> target) {
        target.clear();
        for (NumEventData event : source) {
            target.add(copyEvent(event));
        }
    }

    private static NumEventData copyEvent(NumEventData source) {
        return new NumEventData(source.startBeat(), source.endBeat(), source.startValue(), source.endValue(), source.easingType());
    }

    private static NoteData copyNote(NoteData source) {
        return new NoteData(source.noteType(), source.beat(), source.pos().copy(), source.scale().copy(), source.rotation().copy(), source.holdGroup(), source.holdLengthBeats());
    }

    private static EffectData copyEffect(EffectData source) {
        JsonObject properties = source.properties() == null ? new JsonObject() : source.properties().deepCopy();
        return new EffectData(source.effectType(), source.beat(), properties);
    }

    private static BpmPoint copyBpm(BpmPoint source) {
        return new BpmPoint(source.beat(), source.bpm());
    }
}

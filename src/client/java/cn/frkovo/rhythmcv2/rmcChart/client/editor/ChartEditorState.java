package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.core.TimingTimeline;
import cn.frkovo.rhythmcv2.rmcChart.chart.io.ChartProjectIo;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartProject;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.LevelData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.audio.AudioAnalysis;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.audio.AudioAnalysisService;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.audio.SongAudioPlayer;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.audio.SongAudioResolver;
import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ChartEditorState {
    private ChartProject project;
    private ChartDifficulty activeDifficulty = ChartDifficulty.WORLD;
    private EditorSelection selection = EditorSelection.song();
    private double visibleStartBeat = 0.0;
    private double beatsPerScreen = 16.0;
    private double playheadBeat = 0.0;
    private long playheadMillis = 0L;
    private boolean playing = false;
    private long lastTickNanos = System.nanoTime();
    private String statusMessage = "Ready";
    private final SongAudioPlayer audioPlayer = new SongAudioPlayer();
    private Path audioPath;
    private String audioStatus = "No audio";
    private AudioAnalysis audioAnalysis = AudioAnalysis.empty();
    private int revision = 0;
    private final Set<Integer> expandedTrackIds = new HashSet<>();

    public ChartEditorState() {
        Path defaultPath = ProjectStorage.projectPath("default-project");
        this.project = ChartProject.createEmpty(defaultPath);
        this.playheadMillis = timing().beatToMillis(playheadBeat);
        reloadAudio();
    }

    public ChartProject project() {
        return project;
    }

    public ChartDifficulty activeDifficulty() {
        return activeDifficulty;
    }

    public void setActiveDifficulty(ChartDifficulty activeDifficulty) {
        this.activeDifficulty = activeDifficulty;
        ensureSelectionValid();
        this.playheadMillis = timing().beatToMillis(playheadBeat);
    }

    public EditorSelection selection() {
        return selection;
    }

    public void setSelection(EditorSelection selection) {
        this.selection = selection;
    }

    public double visibleStartBeat() {
        return visibleStartBeat;
    }

    public double beatsPerScreen() {
        return beatsPerScreen;
    }

    public double visibleEndBeat() {
        return visibleStartBeat + beatsPerScreen;
    }

    public double playheadBeat() {
        return playheadBeat;
    }

    public boolean playing() {
        return playing;
    }

    public String statusMessage() {
        return statusMessage;
    }

    public Path audioPath() {
        return audioPath;
    }

    public String audioStatus() {
        return audioStatus;
    }

    public boolean hasAudio() {
        return audioPlayer.isLoaded();
    }

    public long audioLengthMillis() {
        return audioPlayer.lengthMillis();
    }

    public AudioAnalysis audioAnalysis() {
        return audioAnalysis;
    }

    public double estimatedBpmReference() {
        return audioAnalysis.estimatedBpm();
    }

    public LevelData level() {
        return project.level(activeDifficulty);
    }

    public int revision() {
        return revision;
    }

    public TimingTimeline timing() {
        return new TimingTimeline(level());
    }

    public void tick() {
        long now = System.nanoTime();
        if (playing) {
            long deltaMillis = Math.round((now - lastTickNanos) / 1_000_000.0);
            playheadMillis += Math.max(0L, deltaMillis);
            syncAudio(false);
            playheadBeat = timing().calcBeat(playheadMillis);
            keepPlayheadVisible();
        }
        lastTickNanos = now;
    }

    public void togglePlayback() {
        playing = !playing;
        if (playing) {
            playheadMillis = timing().beatToMillis(playheadBeat);
            syncAudio(true);
        } else {
            audioPlayer.pause();
        }
        lastTickNanos = System.nanoTime();
        setStatus(playing ? "Playback started" : "Playback paused");
    }

    public void stopPlayback() {
        playing = false;
        playheadMillis = timing().beatToMillis(playheadBeat);
        audioPlayer.pause();
        syncAudio(true);
        setStatus("Playback stopped");
    }

    public void seekToBeat(double beat) {
        playheadBeat = Math.max(-64.0, beat);
        playheadMillis = timing().beatToMillis(playheadBeat);
        syncAudio(true);
        keepPlayheadVisible();
    }

    public void scrollWindow(double deltaBeat) {
        visibleStartBeat = Math.max(-64.0, visibleStartBeat + deltaBeat);
    }

    public void zoom(double factor) {
        double next = beatsPerScreen * factor;
        beatsPerScreen = Math.max(4.0, Math.min(128.0, next));
        keepPlayheadVisible();
    }

    public void loadProject(Path path) {
        try {
            project = ChartProjectIo.load(path);
            project.setProjectPath(path);
            activeDifficulty = ChartDifficulty.WORLD;
            selection = EditorSelection.song();
            playheadBeat = 0.0;
            playheadMillis = timing().beatToMillis(playheadBeat);
            visibleStartBeat = 0.0;
            reloadAudio();
            resetExpandedTracks();
            markDirty();
            setStatus("Loaded project: " + path);
        } catch (IOException exception) {
            setStatus("Load failed: " + exception.getMessage());
        }
    }

    public void saveProject(Path path) {
        try {
            project.setProjectPath(path);
            sortCurrentLevel();
            ChartProjectIo.save(project);
            setStatus("Saved project: " + path);
        } catch (IOException exception) {
            setStatus("Save failed: " + exception.getMessage());
        }
    }

    public void newProject(Path path) {
        project = ChartProject.createEmpty(path);
        activeDifficulty = ChartDifficulty.WORLD;
        selection = EditorSelection.song();
        playheadBeat = 0.0;
        playheadMillis = timing().beatToMillis(playheadBeat);
        visibleStartBeat = 0.0;
        reloadAudio();
        resetExpandedTracks();
        markDirty();
        setStatus("Created new project: " + path);
    }

    public void markDirty() {
        revision++;
    }

    public void reloadAudio() {
        audioPlayer.close();
        audioPath = null;
        audioStatus = "No audio";
        audioAnalysis = AudioAnalysis.empty();

        Optional<Path> resolved = SongAudioResolver.resolve(project.projectPath());
        if (resolved.isEmpty()) {
            return;
        }

        try {
            audioPlayer.load(resolved.get());
            audioPath = resolved.get();
            audioAnalysis = AudioAnalysisService.analyze(resolved.get());
            audioStatus = audioAnalysis.hasEstimatedBpm()
                    ? "Loaded audio | BPM ref " + formatBeat(audioAnalysis.estimatedBpm())
                    : "Loaded audio";
            syncAudio(true);
        } catch (Exception exception) {
            String detail = exception.getMessage();
            audioStatus = detail == null || detail.isBlank()
                    ? "Audio load failed: " + exception.getClass().getSimpleName()
                    : "Audio load failed: " + exception.getClass().getSimpleName() + " - " + detail;
            audioAnalysis = AudioAnalysis.empty();
        }
    }

    public boolean applyEstimatedBpmReferenceIfDefault() {
        if (!audioAnalysis.hasEstimatedBpm()) {
            return false;
        }
        if (level().meta().bpms().size() != 1) {
            return false;
        }
        BpmPoint bpm = level().meta().bpms().getFirst();
        if (Math.abs(bpm.beat()) > 1.0E-6 || Math.abs(bpm.bpm() - 120.0) > 1.0E-6) {
            return false;
        }
        bpm.setBpm(audioAnalysis.estimatedBpm());
        markDirty();
        setStatus("Applied BPM reference: " + formatBeat(audioAnalysis.estimatedBpm()));
        return true;
    }

    public TrackData addTrack() {
        int nextId = level().tracks().stream().map(TrackData::id).max(Integer::compareTo).orElse(-1) + 1;
        TrackData track = TrackData.createDefault(nextId);
        level().tracks().add(track);
        expandedTrackIds.add(track.id());
        sortCurrentLevel();
        selection = EditorSelection.track(track);
        markDirty();
        setStatus("Added track " + track.id());
        return track;
    }

    public NoteData addNote() {
        TrackData track = selectedTrack();
        if (track == null) {
            track = addTrack();
        }
        return addNote(track, playheadBeat);
    }

    public NoteData addNote(TrackData track, double beat) {
        if (track == null) {
            track = addTrack();
        }
        NoteData note = NoteData.createDefault(beat);
        track.notes().add(note);
        sortCurrentLevel();
        selection = EditorSelection.note(track, note);
        markDirty();
        setStatus("Added note at beat " + formatBeat(note.beat()));
        return note;
    }

    public EffectData addEffect() {
        EffectData effect = EffectData.createDefault(playheadBeat);
        level().effects().add(effect);
        sortCurrentLevel();
        selection = EditorSelection.effect(effect);
        markDirty();
        setStatus("Added effect at beat " + formatBeat(effect.beat()));
        return effect;
    }

    public BpmPoint addBpm() {
        BpmPoint bpm = new BpmPoint(playheadBeat, 120.0);
        level().meta().bpms().add(bpm);
        sortCurrentLevel();
        selection = EditorSelection.bpm(bpm);
        markDirty();
        setStatus("Added BPM point at beat " + formatBeat(bpm.beat()));
        return bpm;
    }

    public void deleteSelection() {
        switch (selection.kind()) {
            case TRACK -> {
                if (selection.track() != null && level().tracks().size() > 1) {
                    level().tracks().remove(selection.track());
                    selection = EditorSelection.meta();
                    setStatus("Deleted track");
                }
            }
            case NOTE -> {
                if (selection.track() != null && selection.note() != null) {
                    selection.track().notes().remove(selection.note());
                    selection = EditorSelection.track(selection.track());
                    setStatus("Deleted note");
                }
            }
            case EFFECT -> {
                if (selection.effect() != null) {
                    level().effects().remove(selection.effect());
                    selection = EditorSelection.meta();
                    setStatus("Deleted effect");
                }
            }
            case BPM -> {
                if (selection.bpm() != null && level().meta().bpms().size() > 1) {
                    level().meta().bpms().remove(selection.bpm());
                    selection = EditorSelection.meta();
                    setStatus("Deleted BPM point");
                }
            }
            default -> {
            }
        }
        markDirty();
        ensureSelectionValid();
    }

    public void moveSelectedNote(double beat, TrackData newTrack) {
        NoteData note = selection.note();
        TrackData oldTrack = selection.track();
        if (note == null || oldTrack == null || newTrack == null) {
            return;
        }
        note.setBeat(beat);
        if (oldTrack != newTrack) {
            oldTrack.notes().remove(note);
            newTrack.notes().add(note);
            selection = EditorSelection.note(newTrack, note);
        }
        sortCurrentLevel();
        markDirty();
    }

    public void moveSelectedEffect(double beat) {
        if (selection.effect() == null) {
            return;
        }
        selection.effect().setBeat(beat);
        sortCurrentLevel();
        markDirty();
    }

    public void moveSelectedBpm(double beat) {
        if (selection.bpm() == null) {
            return;
        }
        selection.bpm().setBeat(beat);
        sortCurrentLevel();
        playheadMillis = timing().beatToMillis(playheadBeat);
        markDirty();
    }

    public TrackData selectedTrack() {
        if (selection.kind() == EditorSelection.Kind.TRACK || selection.kind() == EditorSelection.Kind.NOTE) {
            return selection.track();
        }
        return level().tracks().isEmpty() ? null : level().tracks().getFirst();
    }

    public boolean isTrackExpanded(TrackData track) {
        return track != null && expandedTrackIds.contains(track.id());
    }

    public void toggleTrackExpanded(TrackData track) {
        if (track == null) {
            return;
        }
        if (!expandedTrackIds.add(track.id())) {
            expandedTrackIds.remove(track.id());
        }
    }

    public void setTrackExpanded(TrackData track, boolean expanded) {
        if (track == null) {
            return;
        }
        if (expanded) {
            expandedTrackIds.add(track.id());
        } else {
            expandedTrackIds.remove(track.id());
        }
    }

    public int trackIndex(TrackData track) {
        return level().tracks().indexOf(track);
    }

    public List<TrackData> tracks() {
        return level().tracks();
    }

    public List<EffectData> triggeredEffects() {
        List<EffectData> triggered = new ArrayList<>();
        for (EffectData effect : level().effects()) {
            if (effect.beat() <= playheadBeat) {
                triggered.add(effect);
            }
        }
        return triggered;
    }

    public void setStatus(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void sortCurrentLevel() {
        level().tracks().sort(Comparator.comparingInt(TrackData::id));
        expandedTrackIds.retainAll(level().tracks().stream().map(TrackData::id).collect(java.util.stream.Collectors.toSet()));
        for (TrackData track : level().tracks()) {
            sortEvents(track.speedEvents());
            sortEvents(track.xTransformEvents());
            sortEvents(track.yTransformEvents());
            sortEvents(track.zTransformEvents());
            sortEvents(track.xRotateEvents());
            sortEvents(track.yRotateEvents());
            sortEvents(track.zRotateEvents());
            sortEvents(track.xScaleEvents());
            sortEvents(track.yScaleEvents());
            sortEvents(track.zScaleEvents());
            track.notes().sort(Comparator.comparingDouble(NoteData::beat));
        }
        level().effects().sort(Comparator.comparingDouble(EffectData::beat));
        level().meta().bpms().sort(Comparator.comparingDouble(BpmPoint::beat));
    }

    private void sortEvents(List<NumEventData> events) {
        events.sort(Comparator.comparingDouble(NumEventData::startBeat));
    }

    private void ensureSelectionValid() {
        if (selection.kind() == EditorSelection.Kind.TRACK && !level().tracks().contains(selection.track())) {
            selection = EditorSelection.meta();
        }
        if (selection.kind() == EditorSelection.Kind.NOTE) {
            TrackData track = selection.track();
            NoteData note = selection.note();
            if (track == null || note == null || !level().tracks().contains(track) || !track.notes().contains(note)) {
                selection = EditorSelection.meta();
            } else {
                expandedTrackIds.add(track.id());
            }
        }
        if (selection.kind() == EditorSelection.Kind.TRACK && selection.track() != null) {
            expandedTrackIds.add(selection.track().id());
        }
        if (selection.kind() == EditorSelection.Kind.EFFECT && !level().effects().contains(selection.effect())) {
            selection = EditorSelection.meta();
        }
        if (selection.kind() == EditorSelection.Kind.BPM && !level().meta().bpms().contains(selection.bpm())) {
            selection = EditorSelection.meta();
        }
    }

    private void keepPlayheadVisible() {
        if (playheadBeat < visibleStartBeat + 1.0) {
            visibleStartBeat = playheadBeat - 1.0;
        } else if (playheadBeat > visibleStartBeat + beatsPerScreen - 1.0) {
            visibleStartBeat = playheadBeat - beatsPerScreen + 1.0;
        }
        visibleStartBeat = Math.max(-64.0, visibleStartBeat);
    }

    private String formatBeat(double beat) {
        return String.format(java.util.Locale.ROOT, "%.3f", beat);
    }

    private void resetExpandedTracks() {
        expandedTrackIds.clear();
        for (TrackData track : level().tracks()) {
            expandedTrackIds.add(track.id());
        }
    }

    private void syncAudio(boolean forceSeek) {
        if (!audioPlayer.isLoaded()) {
            return;
        }

        long targetMillis = Math.max(0L, Math.min(audioPlayer.lengthMillis(), playheadMillis));

        if (!playing) {
            audioPlayer.pause();
            audioPlayer.seekMillis(targetMillis);
            return;
        }

        if (playheadMillis < 0L) {
            audioPlayer.pause();
            audioPlayer.seekMillis(0L);
            return;
        }

        if (playheadMillis > audioPlayer.lengthMillis()) {
            audioPlayer.pause();
            audioPlayer.seekMillis(audioPlayer.lengthMillis());
            return;
        }

        long drift = Math.abs(audioPlayer.positionMillis() - targetMillis);
        if (forceSeek || drift > 120L) {
            audioPlayer.seekMillis(targetMillis);
        }
        if (!audioPlayer.isPlaying()) {
            audioPlayer.play();
        }
    }
}

package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import com.google.gson.JsonObject;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.TimingTimeline;
import cn.frkovo.rhythmcv2.rmcChart.chart.io.ChartProjectIo;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartProject;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.LevelData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.MetaData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.SongManifestData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.TrackData;
import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.audio.AudioAnalysis;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.audio.AudioAnalysisService;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.audio.PreviewAutoSoundScheduler;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.audio.SongAudioPlayer;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.audio.SongAudioResolver;
import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ChartEditorState {
    public record SelectionSnapshot(EditorSelection.Kind kind, Integer trackId, Integer noteIndex, Integer effectIndex, Integer bpmIndex) {
    }

    public record EditorSnapshot(ChartProject project, ChartDifficulty activeDifficulty, double visibleStartBeat, double beatsPerScreen,
                                  double playheadBeat, SelectionSnapshot selection) {
    }

    public record SerializedPreviewChart(String manifestJson, String levelJson) {
    }

    private ChartProject project;
    private ChartDifficulty activeDifficulty = ChartDifficulty.WORLD;
    private EditorSelection selection = EditorSelection.song();
    private double visibleStartBeat = 0.0;
    private double beatsPerScreen = 16.0;
    private double playheadBeat = 0.0;
    private long playheadMillis = 0L;
    private boolean playing = false;
    private double playbackStartBeat = 0.0;
    private double playbackEndBeat = Double.NaN;
    private boolean showOnlySelectedTrack = false;
    private long lastTickNanos = System.nanoTime();
    private String statusMessage = "Ready";
    private final SongAudioPlayer audioPlayer = new SongAudioPlayer();
    private Path audioPath;
    private String audioStatus = "No audio";
    private AudioAnalysis audioAnalysis = AudioAnalysis.empty();
    private int revision = 0;
    private final Set<Integer> expandedTrackIds = new HashSet<>();
    private final Map<Integer, Set<String>> expandedTrackEventGroups = new HashMap<>();
    private boolean fxTrackExpanded = true;
    private boolean projectDirty;
    private boolean previewChartDirty = true;
    private boolean previewChartUploaded;
    private boolean previewUploading;
    private boolean serverPreviewRunning;
    private boolean previewAudioUploaded;
    private boolean previewSchematicUploaded;
    private boolean previewAutoStarted;
    private final PreviewAutoSoundScheduler autoSoundScheduler = new PreviewAutoSoundScheduler();
    private final EditorDraft editorDraft = new EditorDraft();


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

    public boolean selectTrackById(int trackId) {
        TrackData track = trackById(trackId);
        if (track == null) {
            return false;
        }
        setSelection(EditorSelection.track(track));
        return true;
    }

    public boolean selectNoteByTrackAndIndex(int trackId, int noteIndex) {
        TrackData track = trackById(trackId);
        if (track == null || noteIndex < 0 || noteIndex >= track.notes().size()) {
            return false;
        }
        setSelection(EditorSelection.note(track, track.notes().get(noteIndex)));
        return true;
    }

    public boolean selectEffectByIndex(int effectIndex) {
        if (effectIndex < 0 || effectIndex >= level().effects().size()) {
            return false;
        }
        setSelection(EditorSelection.effect(level().effects().get(effectIndex)));
        return true;
    }

    public boolean selectBpmByIndex(int bpmIndex) {
        if (bpmIndex < 0 || bpmIndex >= level().meta().bpms().size()) {
            return false;
        }
        setSelection(EditorSelection.bpm(level().meta().bpms().get(bpmIndex)));
        return true;
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

    public long playheadMillis() {
        return playheadMillis;
    }

    public boolean playing() {
        return playing;
    }

    public double playbackStartBeat() {
        return playbackStartBeat;
    }

    public double playbackEndBeat() {
        return playbackEndBeat;
    }

    public boolean hasPlaybackEndBeat() {
        return Double.isFinite(playbackEndBeat);
    }

    public boolean showOnlySelectedTrack() {
        return showOnlySelectedTrack;
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

    public long totalDurationMillis() {
        if (audioPlayer.isLoaded() && audioPlayer.lengthMillis() > 0L) {
            return audioPlayer.lengthMillis();
        }
        long manifestMillis = Math.max(0L, project.manifest().length()) * 1000L;
        if (manifestMillis > 0L) {
            return manifestMillis;
        }
        double maxBeat = Math.max(playheadBeat, playbackEndBeat);
        for (TrackData track : level().tracks()) {
            for (NoteData note : track.notes()) {
                maxBeat = Math.max(maxBeat, note.beat() + note.holdLengthBeats());
            }
        }
        return Math.max(manifestMillis, timing().beatToMillis(maxBeat));
    }

    public double currentBpm() {
        double bpm = 120.0;
        for (BpmPoint point : level().meta().bpms()) {
            if (point.beat() <= playheadBeat) {
                bpm = point.bpm();
            } else {
                break;
            }
        }
        return bpm;
    }

    public long playheadTick() {
        return Math.max(0L, playheadMillis / 50L);
    }

    public String progressText() {
        return String.format(java.util.Locale.ROOT, "%.2f BPM at %.3f beat | TICK %d | %s / %s",
                currentBpm(),
                playheadBeat,
                playheadTick(),
                formatDuration(playheadMillis),
                formatDuration(totalDurationMillis()));
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

    public boolean projectDirty() {
        return projectDirty;
    }

    public boolean previewChartDirty() {
        return previewChartDirty;
    }

    public boolean previewChartUploaded() {
        return previewChartUploaded;
    }

    public boolean previewUploading() {
        return previewUploading;
    }

    public boolean serverPreviewRunning() {
        return serverPreviewRunning;
    }

    public boolean previewAudioUploaded() {
        return previewAudioUploaded;
    }

    public boolean previewSchematicUploaded() {
        return previewSchematicUploaded;
    }

    public boolean previewAutoStarted() {
        return previewAutoStarted;
    }

    public void markPreviewAutoStarted() {
        previewAutoStarted = true;
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
            if (hasPlaybackEndBeat() && playheadBeat >= playbackEndBeat) {
                playheadBeat = playbackEndBeat;
                playheadMillis = timing().beatToMillis(playheadBeat);
                playing = false;
                audioPlayer.pause();
                syncAudio(true);
                setStatus("Playback reached Out @ " + formatBeat(playbackEndBeat));
            }
            keepPlayheadVisible();
        }
        autoSoundScheduler.tick(this);
        lastTickNanos = now;
    }

    public void togglePlayback() {
        if (playing) {
            pausePlayback("Playback paused");
            return;
        }
        if (hasPlaybackEndBeat() && playheadBeat >= playbackEndBeat) {
            seekToBeat(playbackStartBeat);
        }
        startPlaybackAt(playheadBeat, "Playback started");
    }

    public void playPreviewRange() {
        startPlaybackAt(playbackStartBeat, "Playing In/Out range");
    }

    public void stopPlayback() {
        pausePlayback("Playback stopped");
        playheadMillis = timing().beatToMillis(playheadBeat);
        syncAudio(true);
    }

    public void markPlaybackStartAtPlayhead() {
        setPlaybackStartBeat(playheadBeat);
    }

    public void markPlaybackEndAtPlayhead() {
        setPlaybackEndBeat(playheadBeat);
    }

    public void setPlaybackStartBeat(double beat) {
        playbackStartBeat = clampPlaybackBeat(beat);
        if (hasPlaybackEndBeat() && playbackEndBeat <= playbackStartBeat) {
            playbackEndBeat = Double.NaN;
        }
        setStatus("Playback In @ " + formatBeat(playbackStartBeat));
    }

    public void setPlaybackEndBeat(double beat) {
        double endBeat = clampPlaybackBeat(beat);
        if (endBeat <= playbackStartBeat) {
            playbackStartBeat = Math.max(-64.0, endBeat - 4.0);
        }
        playbackEndBeat = endBeat;
        setStatus("Playback Out @ " + formatBeat(playbackEndBeat));
    }

    public void clearPlaybackEndBeat() {
        playbackEndBeat = Double.NaN;
        setStatus("Playback Out cleared");
    }

    public void toggleShowOnlySelectedTrack() {
        showOnlySelectedTrack = !showOnlySelectedTrack;
        TrackData track = selectedTrack();
        setStatus(showOnlySelectedTrack && track != null ? "Showing only Track " + track.id() : "Showing all tracks");
    }

    public void startPlaybackAt(double beat, String status) {
        playheadBeat = clampPlaybackBeat(beat);
        playheadMillis = timing().beatToMillis(playheadBeat);
        playing = true;
        lastTickNanos = System.nanoTime();
        syncAudio(true);
        setStatus(status);
    }

    public void pausePlayback(String status) {
        playing = false;
        audioPlayer.pause();
        lastTickNanos = System.nanoTime();
        setStatus(status);
    }

    public void seekToBeat(double beat) {
        playheadBeat = clampPlaybackBeat(beat);
        playheadMillis = timing().beatToMillis(playheadBeat);
        syncAudio(true);
        keepPlayheadVisible();
    }

    public void seekByMillis(long deltaMillis) {
        long totalMillis = totalDurationMillis();
        long nextMillis = playheadMillis + deltaMillis;
        if (totalMillis > 0L) {
            nextMillis = Math.max(0L, Math.min(totalMillis, nextMillis));
        } else {
            nextMillis = Math.max(0L, nextMillis);
        }
        seekToBeat(timing().calcBeat(nextMillis));
    }

    public void scrollWindow(double deltaBeat) {
        visibleStartBeat = Math.max(-64.0, visibleStartBeat + deltaBeat);
    }

    public void zoom(double factor) {
        double next = beatsPerScreen * factor;
        setBeatsPerScreen(next);
    }

    public void setBeatsPerScreen(double beatsPerScreen) {
        this.beatsPerScreen = Math.max(4.0, Math.min(128.0, beatsPerScreen));
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
            editorDraft.clear();
            projectDirty = false;
            resetPreviewSyncState();
            ProjectStorage.recordRecentProject(path);
            revision++;
            RmcChartClient.beginEditorSession(true);
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
            projectDirty = false;
            ProjectStorage.recordRecentProject(path);
            RmcChartClient.markEditorSessionSaved();
            setStatus("Saved project: " + path);
        } catch (IOException exception) {
            setStatus("Save failed: " + exception.getMessage());
        }
    }

    public SerializedPreviewChart serializeCurrentChart() {
        sortCurrentLevel();
        return new SerializedPreviewChart(
                ChartProjectIo.toManifestJson(project.manifest()),
                ChartProjectIo.toLevelJson(level())
        );
    }

    public void applyLevelJson(JsonObject levelJson) {
        LevelData newLevel = ChartProjectIo.parseLevel(levelJson, activeDifficulty);
        project.setLevel(activeDifficulty, newLevel);
        selection = EditorSelection.song();
        playheadBeat = 0.0;
        playheadMillis = timing().beatToMillis(playheadBeat);
        resetExpandedTracks();
        projectDirty = true;
        previewChartDirty = true;
        previewChartUploaded = false;
        previewUploading = false;
        serverPreviewRunning = false;
        revision++;
        setStatus("Applied raw level JSON");
    }

    public void startServerPreviewAudio(double beat) {
        serverPreviewRunning = true;
        startPlaybackAt(beat, "Server preview playing @ " + formatBeat(beat));
        autoSoundScheduler.reset(this, beat);
    }

    public void stopServerPreviewAudio(String status) {
        serverPreviewRunning = false;
        autoSoundScheduler.reset(null, 0.0);
        pausePlayback(status);
    }

    public void resetTickClock() {
        lastTickNanos = System.nanoTime();
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
        editorDraft.clear();
        RmcChartClient.beginEditorSession(false);
        markDirty();
        setStatus("Created new project: " + path);
    }

    public void markDirty() {
        revision++;
        projectDirty = true;
        RmcChartClient.markEditorSessionDirty();
        previewChartDirty = true;
        previewChartUploaded = false;
        previewUploading = false;
        serverPreviewRunning = false;
    }

    public void markPreviewUploadStarted() {
        previewUploading = true;
        previewChartUploaded = false;
        serverPreviewRunning = false;
    }

    public void markPreviewChartUploaded() {
        previewUploading = false;
        previewChartDirty = false;
        previewChartUploaded = true;
    }

    public void markPreviewUploadFailed() {
        previewUploading = false;
        serverPreviewRunning = false;
    }

    public void markPreviewAssetUploaded(String fileType) {
        previewUploading = false;
        if (cn.frkovo.rhythmcv2.rmcChart.client.net.ChartPreviewChannel.FILE_TYPE_AUDIO.equalsIgnoreCase(fileType)) {
            previewAudioUploaded = true;
        }
        if (cn.frkovo.rhythmcv2.rmcChart.client.net.ChartPreviewChannel.FILE_TYPE_SCHEMATIC.equalsIgnoreCase(fileType)) {
            previewSchematicUploaded = true;
        }
    }

    public void resetPreviewSyncState() {
        previewChartDirty = true;
        previewChartUploaded = false;
        previewUploading = false;
        serverPreviewRunning = false;
        previewAudioUploaded = false;
        previewSchematicUploaded = false;
        previewAutoStarted = false;
    }

    public void reloadAudio() {
        audioPlayer.close();
        audioPath = null;
        audioStatus = "No audio";
        audioAnalysis = AudioAnalysis.empty();
        previewAudioUploaded = false;

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

    public boolean isFxTrackExpanded() {
        return fxTrackExpanded;
    }

    public void toggleFxTrackExpanded() {
        fxTrackExpanded = !fxTrackExpanded;
    }

    public void setFxTrackExpanded(boolean expanded) {
        fxTrackExpanded = expanded;
    }

    public EditorDraft editorDraft() {
        return editorDraft;
    }

    public boolean isTrackEventGroupExpanded(TrackData track, String group) {
        if (track == null || group == null) return true;
        return expandedTrackEventGroups.computeIfAbsent(track.id(), k ->
            new HashSet<>(List.of("Speed", "Position", "Rotation", "Scale"))
        ).contains(group);
    }

    public void toggleTrackEventGroupExpanded(TrackData track, String group) {
        if (track == null || group == null) return;
        Set<String> groups = expandedTrackEventGroups.computeIfAbsent(track.id(), k ->
            new HashSet<>(List.of("Speed", "Position", "Rotation", "Scale"))
        );
        if (!groups.add(group)) {
            groups.remove(group);
        }
    }

    public int trackIndex(TrackData track) {
        return level().tracks().indexOf(track);
    }

    public List<TrackData> tracks() {
        return level().tracks();
    }

    public List<TrackData> visibleTracks() {
        TrackData selectedTrack = selectedTrack();
        if (showOnlySelectedTrack && selectedTrack != null) {
            return List.of(selectedTrack);
        }
        return tracks();
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

    public EditorSnapshot snapshot() {
        return new EditorSnapshot(
                copyProject(project),
                activeDifficulty,
                visibleStartBeat,
                beatsPerScreen,
                playheadBeat,
                captureSelection()
        );
    }

    public void restoreSnapshot(EditorSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        playing = false;
        audioPlayer.pause();
        project = copyProject(snapshot.project());
        activeDifficulty = snapshot.activeDifficulty();
        visibleStartBeat = snapshot.visibleStartBeat();
        beatsPerScreen = snapshot.beatsPerScreen();
            playheadBeat = snapshot.playheadBeat();
            selection = restoreSelection(snapshot.selection());
            playheadMillis = timing().beatToMillis(playheadBeat);
            editorDraft.clear();
            ensureSelectionValid();
        syncAudio(true);
        revision++;
        projectDirty = true;
        previewChartDirty = true;
        previewChartUploaded = false;
        previewUploading = false;
        serverPreviewRunning = false;
    }

    public void sortCurrentLevel() {
        level().tracks().sort(Comparator.comparingInt(TrackData::id));
        expandedTrackIds.retainAll(level().tracks().stream().map(TrackData::id).collect(java.util.stream.Collectors.toSet()));
        expandedTrackEventGroups.keySet().retainAll(level().tracks().stream().map(TrackData::id).collect(java.util.stream.Collectors.toSet()));
        for (TrackData track : level().tracks()) {
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
        level().effects().sort(Comparator.comparingDouble(EffectData::beat));
        level().meta().bpms().sort(Comparator.comparingDouble(BpmPoint::beat));
    }

    private void sortEvents(List<NumEventData> events) {
        events.sort(Comparator.comparingDouble(NumEventData::startBeat));
    }

    private void normalizeSingleTrackEvent(List<NumEventData> events) {
        sortEvents(events);
        if (events.size() <= 1) {
            return;
        }
        NumEventData first = events.getFirst();
        events.clear();
        events.add(first);
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

    private SelectionSnapshot captureSelection() {
        return switch (selection.kind()) {
            case TRACK -> new SelectionSnapshot(selection.kind(), selection.track() == null ? null : selection.track().id(), null, null, null);
            case NOTE -> new SelectionSnapshot(
                    selection.kind(),
                    selection.track() == null ? null : selection.track().id(),
                    selection.track() == null || selection.note() == null ? null : selection.track().notes().indexOf(selection.note()),
                    null,
                    null
            );
            case EFFECT -> new SelectionSnapshot(selection.kind(), null, null, level().effects().indexOf(selection.effect()), null);
            case BPM -> new SelectionSnapshot(selection.kind(), null, null, null, level().meta().bpms().indexOf(selection.bpm()));
            default -> new SelectionSnapshot(selection.kind(), null, null, null, null);
        };
    }

    private EditorSelection restoreSelection(SelectionSnapshot selectionSnapshot) {
        if (selectionSnapshot == null) {
            return EditorSelection.song();
        }
        return switch (selectionSnapshot.kind()) {
            case SONG -> EditorSelection.song();
            case META -> EditorSelection.meta();
            case TRACK -> {
                TrackData track = trackById(selectionSnapshot.trackId());
                yield track == null ? EditorSelection.meta() : EditorSelection.track(track);
            }
            case NOTE -> {
                TrackData track = trackById(selectionSnapshot.trackId());
                if (track == null || selectionSnapshot.noteIndex() == null || selectionSnapshot.noteIndex() < 0 || selectionSnapshot.noteIndex() >= track.notes().size()) {
                    yield EditorSelection.meta();
                }
                yield EditorSelection.note(track, track.notes().get(selectionSnapshot.noteIndex()));
            }
            case EFFECT -> {
                if (selectionSnapshot.effectIndex() == null || selectionSnapshot.effectIndex() < 0 || selectionSnapshot.effectIndex() >= level().effects().size()) {
                    yield EditorSelection.meta();
                }
                yield EditorSelection.effect(level().effects().get(selectionSnapshot.effectIndex()));
            }
            case BPM -> {
                if (selectionSnapshot.bpmIndex() == null || selectionSnapshot.bpmIndex() < 0 || selectionSnapshot.bpmIndex() >= level().meta().bpms().size()) {
                    yield EditorSelection.meta();
                }
                yield EditorSelection.bpm(level().meta().bpms().get(selectionSnapshot.bpmIndex()));
            }
        };
    }

    private TrackData trackById(Integer trackId) {
        if (trackId == null) {
            return null;
        }
        for (TrackData track : level().tracks()) {
            if (track.id() == trackId) {
                return track;
            }
        }
        return null;
    }

    private ChartProject copyProject(ChartProject source) {
        EnumMap<ChartDifficulty, LevelData> levels = new EnumMap<>(ChartDifficulty.class);
        for (Map.Entry<ChartDifficulty, LevelData> entry : source.levels().entrySet()) {
            levels.put(entry.getKey(), copyLevel(entry.getValue()));
        }
        return new ChartProject(source.projectPath(), copyManifest(source.manifest()), levels);
    }

    private SongManifestData copyManifest(SongManifestData source) {
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

    private void copyMapList(List<Map<String, Object>> source, List<Map<String, Object>> target) {
        target.clear();
        for (Map<String, Object> map : source) {
            target.add(new LinkedHashMap<>(map));
        }
    }

    private LevelData copyLevel(LevelData source) {
        LevelData level = new LevelData(copyMeta(source.meta()));
        for (TrackData track : source.tracks()) {
            level.tracks().add(copyTrack(track));
        }
        for (EffectData effect : source.effects()) {
            level.effects().add(copyEffect(effect));
        }
        return level;
    }

    private MetaData copyMeta(MetaData source) {
        MetaData meta = new MetaData(source.uid(), source.initialArena(), source.offset(), source.level());
        meta.charters().addAll(source.charters());
        meta.comments().addAll(source.comments());
        for (BpmPoint bpm : source.bpms()) {
            meta.bpms().add(copyBpm(bpm));
        }
        return meta;
    }

    private TrackData copyTrack(TrackData source) {
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
        return track;
    }

    private void copyEvents(List<NumEventData> source, List<NumEventData> target) {
        target.clear();
        for (NumEventData event : source) {
            target.add(copyEvent(event));
        }
    }

    private NumEventData copyEvent(NumEventData source) {
        return new NumEventData(source.startBeat(), source.endBeat(), source.startValue(), source.endValue(), source.easingType());
    }

    private NoteData copyNote(NoteData source) {
        return new NoteData(source.noteType(), source.beat(), source.pos().copy(), source.scale().copy(), source.rotation().copy(), source.holdGroup(), source.holdLengthBeats());
    }

    private EffectData copyEffect(EffectData source) {
        JsonObject properties = source.properties() == null ? new JsonObject() : source.properties().deepCopy();
        return new EffectData(source.effectType(), source.beat(), properties);
    }

    private BpmPoint copyBpm(BpmPoint source) {
        return new BpmPoint(source.beat(), source.bpm());
    }

    private void resetExpandedTracks() {
        expandedTrackIds.clear();
        expandedTrackEventGroups.clear();
        for (TrackData track : level().tracks()) {
            expandedTrackIds.add(track.id());
        }
    }

    private double clampPlaybackBeat(double beat) {
        if (!Double.isFinite(beat)) {
            return 0.0;
        }
        return Math.max(-64.0, beat);
    }

    private String formatDuration(long millis) {
        long safe = Math.max(0L, millis);
        long totalSeconds = safe / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        long hundredths = (safe % 1000L) / 10L;
        return String.format(java.util.Locale.ROOT, "%02d:%02d.%02d", minutes, seconds, hundredths);
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

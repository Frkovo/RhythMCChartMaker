package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import cn.frkovo.rhythmcv2.rmcChart.client.net.ChartPreviewChannel;
import cn.frkovo.rhythmcv2.rmcChart.client.net.PreviewClient;
import cn.frkovo.rhythmcv2.rmcChart.client.net.PreviewClient.PreviewEvent;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.PendingPreviewAction;

import java.nio.file.Files;
import java.nio.file.Path;

final class EditorPreviewBridge {
    private final ChartEditorScreen screen;
    PendingPreviewAction pendingPreviewAction = PendingPreviewAction.NONE;
    double pendingServerPreviewBeat = Double.NaN;
    boolean openWorldPreviewOnReady;
    boolean autoPreviewStarted;

    EditorPreviewBridge(ChartEditorScreen screen) {
        this.screen = screen;
    }

    void uploadServerPreview(PendingPreviewAction action) {
        PreviewClient previewClient = RmcChartClient.getPreviewClient();
        if (!previewClient.isReady()) {
            openWorldPreviewOnReady = false;
            screen.state.setStatus("Preview server not ready: " + previewClient.handshakeMessage());
            return;
        }
        if (!screen.propertyPanel.applyFieldsToSelection()) {
            openWorldPreviewOnReady = false;
            return;
        }
        try {
            ChartEditorState.SerializedPreviewChart payload = screen.state.serializeCurrentChart();
            pendingPreviewAction = action;
            pendingServerPreviewBeat = screen.state.playheadBeat();
            screen.state.markPreviewUploadStarted();
            previewClient.sendChartLoad(payload.manifestJson(), payload.levelJson());
            screen.state.setStatus(action == PendingPreviewAction.LOAD_ONLY ? "Uploading chart to preview server" : "Uploading chart and starting server preview");
        } catch (RuntimeException exception) {
            pendingPreviewAction = PendingPreviewAction.NONE;
            pendingServerPreviewBeat = Double.NaN;
            openWorldPreviewOnReady = false;
            screen.state.markPreviewUploadFailed();
            screen.state.setStatus("Preview upload failed: " + exception.getMessage());
        }
    }

    void startWorldPreviewFromEditor(PendingPreviewAction action) {
        openWorldPreviewOnReady = true;
        uploadServerPreview(action);
    }

    void stopServerPreview() {
        PreviewClient previewClient = RmcChartClient.getPreviewClient();
        if (!previewClient.isReady()) {
            screen.state.stopServerPreviewAudio("Preview server not ready: " + previewClient.handshakeMessage());
            return;
        }
        previewClient.sendPreviewStop();
        pendingPreviewAction = PendingPreviewAction.NONE;
        pendingServerPreviewBeat = Double.NaN;
        openWorldPreviewOnReady = false;
        screen.state.stopServerPreviewAudio("Server preview stop requested");
    }

    void handleServerPreviewEvent(PreviewEvent event) {
        switch (event.type()) {
            case HELLO_ACK -> screen.state.setStatus(event.ok()
                    ? "Preview server ready. You can edit now. Default schematic: " + event.defaultSchematicName()
                    : "Preview handshake rejected: " + event.error());
            case CHART_LOAD_ACK -> handleServerPreviewLoadAck(event);
            case PREVIEW_READY -> {
                double beat = Double.isFinite(pendingServerPreviewBeat) ? pendingServerPreviewBeat : screen.state.playheadBeat();
                screen.state.startServerPreviewAudio(beat);
                pendingServerPreviewBeat = Double.NaN;
                screen.state.setStatus("Server preview ready; local audio started @ " + screen.propertyPanel.format(beat));
                if (openWorldPreviewOnReady && screen.getClient() != null && screen.getClient().world != null) {
                    openWorldPreviewOnReady = false;
                    RmcChartClient.enterWorldPreviewFromEditor();
                    screen.getClient().setScreen(null);
                }
            }
            case PREVIEW_STOPPED -> screen.state.stopServerPreviewAudio("Server preview stopped");
            case FILE_UPLOAD_ACK -> {
                if (event.ok()) {
                    screen.state.markPreviewAssetUploaded(event.fileType());
                } else {
                    screen.state.markPreviewUploadFailed();
                }
                screen.state.setStatus((event.ok() ? "Uploaded " : "Upload rejected ")
                        + event.fileType() + ": " + event.error());
            }
            case ERROR -> {
                pendingPreviewAction = PendingPreviewAction.NONE;
                pendingServerPreviewBeat = Double.NaN;
                openWorldPreviewOnReady = false;
                screen.state.markPreviewUploadFailed();
                screen.state.stopServerPreviewAudio("Server preview error: " + event.error());
            }
        }
    }

    void uploadPreviewSchematic() {
        PreviewClient previewClient = RmcChartClient.getPreviewClient();
        if (!previewClient.isReady()) {
            screen.state.setStatus("Preview server not ready: " + previewClient.handshakeMessage());
            return;
        }
        Path schematic = screen.state.project().projectPath().resolve(ChartPreviewChannel.DEFAULT_SCHEMATIC_NAME);
        if (!Files.isRegularFile(schematic)) {
            screen.state.setStatus("No " + ChartPreviewChannel.DEFAULT_SCHEMATIC_NAME + " in project folder; server default will be used.");
            return;
        }
        previewClient.sendFileUpload(schematic, ChartPreviewChannel.FILE_TYPE_SCHEMATIC);
        screen.state.setStatus("Uploading schematic: " + schematic.getFileName());
    }

    void uploadPreviewAudio() {
        PreviewClient previewClient = RmcChartClient.getPreviewClient();
        if (!previewClient.isReady()) {
            screen.state.setStatus("Preview server not ready: " + previewClient.handshakeMessage());
            return;
        }
        Path audio = screen.state.audioPath();
        if (audio == null || !Files.isRegularFile(audio)) {
            screen.state.setStatus("No local audio file loaded for upload");
            return;
        }
        previewClient.sendFileUpload(audio, ChartPreviewChannel.FILE_TYPE_AUDIO);
        screen.state.setStatus("Uploading audio cache: " + audio.getFileName());
    }

    void handleServerPreviewLoadAck(PreviewEvent event) {
        if (!event.ok()) {
            pendingPreviewAction = PendingPreviewAction.NONE;
            pendingServerPreviewBeat = Double.NaN;
            openWorldPreviewOnReady = false;
            screen.state.markPreviewUploadFailed();
            screen.state.setStatus("Preview upload rejected: " + event.error());
            return;
        }
        screen.state.markPreviewChartUploaded();
        double beat = Double.isFinite(pendingServerPreviewBeat) ? pendingServerPreviewBeat : screen.state.playheadBeat();
        if (pendingPreviewAction == PendingPreviewAction.START) {
            RmcChartClient.getPreviewClient().sendPreviewStart(beat);
            screen.state.setStatus("Preview uploaded; starting server visuals @ " + screen.propertyPanel.format(beat));
        } else if (pendingPreviewAction == PendingPreviewAction.RESTART) {
            RmcChartClient.getPreviewClient().sendPreviewRestart(beat);
            screen.state.setStatus("Preview uploaded; restarting server visuals @ " + screen.propertyPanel.format(beat));
        } else {
            pendingServerPreviewBeat = Double.NaN;
            screen.state.setStatus("Preview chart uploaded");
        }
        pendingPreviewAction = PendingPreviewAction.NONE;
    }
}

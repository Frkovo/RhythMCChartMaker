package cn.frkovo.rhythmcv2.rmcChart.client.net;

public final class PreviewEvent {
    public final PreviewEventType type;
    public final boolean ok;
    public final String error;
    public final long serverWallClockMs;
    public final String uploadId;
    public final String fileType;
    public final int protocolVersion;
    public final int maxPayloadBytes;
    public final String defaultSchematicName;
    public final String data;

    public PreviewEvent(PreviewEventType type, boolean ok, String error, long serverWallClockMs,
                        String uploadId, String fileType, int protocolVersion, int maxPayloadBytes,
                        String defaultSchematicName, String data) {
        this.type = type;
        this.ok = ok;
        this.error = error;
        this.serverWallClockMs = serverWallClockMs;
        this.uploadId = uploadId;
        this.fileType = fileType;
        this.protocolVersion = protocolVersion;
        this.maxPayloadBytes = maxPayloadBytes;
        this.defaultSchematicName = defaultSchematicName;
        this.data = data;
    }

    public PreviewEvent(PreviewEventType type, boolean ok, String error, long serverWallClockMs) {
        this(type, ok, error, serverWallClockMs, "", "", 0, 0, "", "");
    }

    public PreviewEventType type() { return type; }
    public boolean ok() { return ok; }
    public String error() { return error; }
    public long serverWallClockMs() { return serverWallClockMs; }
    public String uploadId() { return uploadId; }
    public String fileType() { return fileType; }
    public int protocolVersion() { return protocolVersion; }
    public int maxPayloadBytes() { return maxPayloadBytes; }
    public String defaultSchematicName() { return defaultSchematicName; }
    public String data() { return data; }
}

package cn.frkovo.rhythmcv2.rmcChart.client.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Constants and opcodes for the {@code rhythmc:chart_preview} plugin channel.
 * Must match the server side (RhythMC-Preview {@code ChartPreviewChannel}).
 */
public final class ChartPreviewChannel {
    public static final String CHANNEL = "rhythmc:chart_preview";

    private ChartPreviewChannel() {
    }

    // Client -> Server
    public static final int OP_HELLO = 1;
    public static final int OP_CHART_LOAD = 2;
    public static final int OP_PREVIEW_START = 3;
    public static final int OP_PREVIEW_STOP = 4;
    public static final int OP_PREVIEW_RESTART = 5;
    public static final int OP_CHART_LOAD_CHUNK_START = 6;
    public static final int OP_CHART_LOAD_CHUNK = 7;
    public static final int OP_CHART_LOAD_CHUNK_END = 8;
    public static final int OP_FILE_UPLOAD_START = 9;
    public static final int OP_FILE_UPLOAD_CHUNK = 10;
    public static final int OP_FILE_UPLOAD_END = 11;

    // Server -> Client
    public static final int OP_HELLO_ACK = 101;
    public static final int OP_CHART_LOAD_ACK = 102;
    public static final int OP_PREVIEW_READY = 103;
    public static final int OP_PREVIEW_STOPPED = 104;
    public static final int OP_ERROR = 105;
    public static final int OP_FILE_UPLOAD_ACK = 106;
    public static final int OP_EDITOR_OPEN = 107;
    public static final int OP_EDITOR_SELECT = 108;

    public static final int PROTOCOL_VERSION = 2;
    public static final String FILE_TYPE_SCHEMATIC = "SCHEMATIC";
    public static final String FILE_TYPE_AUDIO = "AUDIO";
    public static final String DEFAULT_SCHEMATIC_NAME = "VILLAGE.schem";

    /** Safe max payload size for a single plugin message (well under the ~32KB Bukkit limit). */
    public static final int SAFE_PAYLOAD_BYTES = 28_000;
}

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
    public static final int OP_CHART_LOAD = 1;
    public static final int OP_PREVIEW_START = 2;
    public static final int OP_PREVIEW_STOP = 3;
    public static final int OP_PREVIEW_RESTART = 4;
    public static final int OP_CHART_LOAD_CHUNK_START = 5;
    public static final int OP_CHART_LOAD_CHUNK = 6;
    public static final int OP_CHART_LOAD_CHUNK_END = 7;

    // Server -> Client
    public static final int OP_CHART_LOAD_ACK = 101;
    public static final int OP_PREVIEW_READY = 102;
    public static final int OP_PREVIEW_STOPPED = 103;
    public static final int OP_ERROR = 104;

    /** Safe max payload size for a single plugin message (well under the ~32KB Bukkit limit). */
    public static final int SAFE_PAYLOAD_BYTES = 28_000;
}

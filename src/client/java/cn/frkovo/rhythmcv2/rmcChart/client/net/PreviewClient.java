package cn.frkovo.rhythmcv2.rmcChart.client.net;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Client-side transport for the {@code rhythmc:chart_preview} plugin channel.
 */
public final class PreviewClient {
    private final ConcurrentHashMap<String, ChunkUpload> activeUploads = new ConcurrentHashMap<>();
    private Consumer<PreviewEvent> eventHandler = null;

    private boolean registered = false;

    /** Register the payload type and global receiver. Call once from client init. */
    public void register() {
        if (registered) return;
        registered = true;
        PayloadTypeRegistry.playC2S().register(ChartPreviewPayload.ID, ChartPreviewPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChartPreviewPayload.ID, ChartPreviewPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ChartPreviewPayload.ID, (payload, context) -> {
            context.client().execute(() -> handleServerPacket(payload.buf()));
        });
    }

    public enum EventType {
        CHART_LOAD_ACK,
        PREVIEW_READY,
        PREVIEW_STOPPED,
        ERROR
    }

    public record PreviewEvent(EventType type, boolean ok, String error, long serverWallClockMs) {
    }

    public void setEventHandler(Consumer<PreviewEvent> handler) {
        this.eventHandler = handler;
    }

    public boolean canSend() {
        return ClientPlayNetworking.canSend(ChartPreviewPayload.ID);
    }

    public void sendChartLoad(String manifestJson, String levelJson) {
        byte[] manifestBytes = manifestJson.getBytes(StandardCharsets.UTF_8);
        byte[] levelBytes = levelJson.getBytes(StandardCharsets.UTF_8);
        int estimated = 4 + 4 + manifestBytes.length + 4 + levelBytes.length;
        if (estimated <= ChartPreviewChannel.SAFE_PAYLOAD_BYTES) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(ChartPreviewChannel.OP_CHART_LOAD);
            buf.writeInt(manifestBytes.length);
            buf.writeBytes(manifestBytes);
            buf.writeInt(levelBytes.length);
            buf.writeBytes(levelBytes);
            ClientPlayNetworking.send(new ChartPreviewPayload(buf));
        } else {
            sendChunkedChartLoad(manifestBytes, levelBytes);
        }
    }

    private void sendChunkedChartLoad(byte[] manifestBytes, byte[] levelBytes) {
        byte[] combined = new byte[manifestBytes.length + 1 + levelBytes.length];
        System.arraycopy(manifestBytes, 0, combined, 0, manifestBytes.length);
        combined[manifestBytes.length] = 0;
        System.arraycopy(levelBytes, 0, combined, manifestBytes.length + 1, levelBytes.length);

        String chunkId = UUID.randomUUID().toString();
        int chunkSize = ChartPreviewChannel.SAFE_PAYLOAD_BYTES - 256;
        int totalChunks = (combined.length + chunkSize - 1) / chunkSize;
        activeUploads.put(chunkId, new ChunkUpload(chunkId, totalChunks));

        PacketByteBuf startBuf = new PacketByteBuf(Unpooled.buffer());
        startBuf.writeInt(ChartPreviewChannel.OP_CHART_LOAD_CHUNK_START);
        startBuf.writeString(chunkId);
        startBuf.writeInt(totalChunks);
        ClientPlayNetworking.send(new ChartPreviewPayload(startBuf));

        for (int i = 0; i < totalChunks; i++) {
            int offset = i * chunkSize;
            int length = Math.min(chunkSize, combined.length - offset);
            PacketByteBuf chunkBuf = new PacketByteBuf(Unpooled.buffer());
            chunkBuf.writeInt(ChartPreviewChannel.OP_CHART_LOAD_CHUNK);
            chunkBuf.writeString(chunkId);
            chunkBuf.writeInt(i);
            chunkBuf.writeInt(length);
            chunkBuf.writeBytes(combined, offset, length);
            ClientPlayNetworking.send(new ChartPreviewPayload(chunkBuf));
        }

        PacketByteBuf endBuf = new PacketByteBuf(Unpooled.buffer());
        endBuf.writeInt(ChartPreviewChannel.OP_CHART_LOAD_CHUNK_END);
        endBuf.writeString(chunkId);
        ClientPlayNetworking.send(new ChartPreviewPayload(endBuf));
    }

    public void sendPreviewStart(double startBeat) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(ChartPreviewChannel.OP_PREVIEW_START);
        buf.writeDouble(startBeat);
        buf.writeLong(System.currentTimeMillis());
        ClientPlayNetworking.send(new ChartPreviewPayload(buf));
    }

    public void sendPreviewStop() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(ChartPreviewChannel.OP_PREVIEW_STOP);
        ClientPlayNetworking.send(new ChartPreviewPayload(buf));
    }

    public void sendPreviewRestart(double newStartBeat) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(ChartPreviewChannel.OP_PREVIEW_RESTART);
        buf.writeDouble(newStartBeat);
        buf.writeLong(System.currentTimeMillis());
        ClientPlayNetworking.send(new ChartPreviewPayload(buf));
    }

    private void handleServerPacket(PacketByteBuf buf) {
        int opcode = buf.readInt();
        switch (opcode) {
            case ChartPreviewChannel.OP_CHART_LOAD_ACK -> {
                boolean ok = buf.readBoolean();
                int len = buf.readInt();
                String error = len > 0 ? buf.readString(len) : "";
                emit(new PreviewEvent(EventType.CHART_LOAD_ACK, ok, error, 0L));
            }
            case ChartPreviewChannel.OP_PREVIEW_READY -> {
                long serverWallClock = buf.readLong();
                emit(new PreviewEvent(EventType.PREVIEW_READY, true, null, serverWallClock));
            }
            case ChartPreviewChannel.OP_PREVIEW_STOPPED -> {
                emit(new PreviewEvent(EventType.PREVIEW_STOPPED, true, null, 0L));
            }
            case ChartPreviewChannel.OP_ERROR -> {
                int len = buf.readInt();
                String message = len > 0 ? buf.readString(len) : "";
                emit(new PreviewEvent(EventType.ERROR, false, message, 0L));
            }
            default -> {
            }
        }
    }

    private void emit(PreviewEvent event) {
        Consumer<PreviewEvent> handler = eventHandler;
        if (handler != null) {
            handler.accept(event);
        }
    }

    private static final class ChunkUpload {
        final String chunkId;
        final int totalChunks;

        ChunkUpload(String chunkId, int totalChunks) {
            this.chunkId = chunkId;
            this.totalChunks = totalChunks;
        }
    }
}

package cn.frkovo.rhythmcv2.rmcChart.client.net;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Client-side transport for the {@code rhythmc:chart_preview} plugin channel.
 */
public final class PreviewClient {
    private final ConcurrentHashMap<String, ChunkUpload> activeUploads = new ConcurrentHashMap<>();
    private Consumer<PreviewEvent> eventHandler = null;
    private Consumer<PreviewEvent> globalEventHandler = null;
    private boolean handshakeReady = false;
    private String handshakeMessage = "Not connected";

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
        HELLO_ACK,
        CHART_LOAD_ACK,
        PREVIEW_READY,
        PREVIEW_STOPPED,
        FILE_UPLOAD_ACK,
        EDITOR_OPEN,
        EDITOR_SELECT,
        ERROR
    }

    public record PreviewEvent(EventType type, boolean ok, String error, long serverWallClockMs,
                               String uploadId, String fileType, int protocolVersion, int maxPayloadBytes,
                               String defaultSchematicName, String data) {
        public PreviewEvent(EventType type, boolean ok, String error, long serverWallClockMs) {
            this(type, ok, error, serverWallClockMs, "", "", 0, 0, "", "");
        }
    }

    public void setEventHandler(Consumer<PreviewEvent> handler) {
        this.eventHandler = handler;
    }

    public void setGlobalEventHandler(Consumer<PreviewEvent> handler) {
        this.globalEventHandler = handler;
    }

    public boolean canSend() {
        return ClientPlayNetworking.canSend(ChartPreviewPayload.ID);
    }

    public boolean isReady() {
        return handshakeReady && canSend();
    }

    public String handshakeMessage() {
        return handshakeMessage;
    }

    public void resetHandshake(String message) {
        handshakeReady = false;
        handshakeMessage = message == null ? "Not connected" : message;
    }

    public void sendHello(String modVersion) {
        if (!ensureCanSend()) {
            resetHandshake("Preview channel unavailable");
            return;
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(ChartPreviewChannel.OP_HELLO);
        buf.writeInt(ChartPreviewChannel.PROTOCOL_VERSION);
        writeUtf8String(buf, modVersion == null ? "RhythMCChartMaker" : modVersion);
        ClientPlayNetworking.send(new ChartPreviewPayload(buf));
    }

    public void sendChartLoad(String manifestJson, String levelJson) {
        if (!ensureReady()) {
            return;
        }
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
        writeUtf8String(startBuf, chunkId);
        startBuf.writeInt(totalChunks);
        ClientPlayNetworking.send(new ChartPreviewPayload(startBuf));

        for (int i = 0; i < totalChunks; i++) {
            int offset = i * chunkSize;
            int length = Math.min(chunkSize, combined.length - offset);
            PacketByteBuf chunkBuf = new PacketByteBuf(Unpooled.buffer());
            chunkBuf.writeInt(ChartPreviewChannel.OP_CHART_LOAD_CHUNK);
            writeUtf8String(chunkBuf, chunkId);
            chunkBuf.writeInt(i);
            chunkBuf.writeInt(length);
            chunkBuf.writeBytes(combined, offset, length);
            ClientPlayNetworking.send(new ChartPreviewPayload(chunkBuf));
        }

        PacketByteBuf endBuf = new PacketByteBuf(Unpooled.buffer());
        endBuf.writeInt(ChartPreviewChannel.OP_CHART_LOAD_CHUNK_END);
        writeUtf8String(endBuf, chunkId);
        ClientPlayNetworking.send(new ChartPreviewPayload(endBuf));
    }

    public void sendPreviewStart(double startBeat, byte mode) {
        if (!ensureReady()) {
            return;
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(ChartPreviewChannel.OP_PREVIEW_START);
        buf.writeDouble(startBeat);
        buf.writeLong(System.currentTimeMillis());
        buf.writeByte(mode);
        ClientPlayNetworking.send(new ChartPreviewPayload(buf));
    }

    public boolean sendPreviewStop() {
        if (!ensureReady()) {
            return false;
        }
        try {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(ChartPreviewChannel.OP_PREVIEW_STOP);
            ClientPlayNetworking.send(new ChartPreviewPayload(buf));
            return true;
        } catch (RuntimeException exception) {
            emit(new PreviewEvent(EventType.ERROR, false,
                    "Preview stop could not be sent: " + exception.getMessage(), 0L));
            return false;
        }
    }

    public void sendPreviewRestart(double newStartBeat) {
        if (!ensureReady()) {
            return;
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(ChartPreviewChannel.OP_PREVIEW_RESTART);
        buf.writeDouble(newStartBeat);
        buf.writeLong(System.currentTimeMillis());
        ClientPlayNetworking.send(new ChartPreviewPayload(buf));
    }

    public void sendFileUpload(Path path, String fileType) {
        if (!ensureReady()) {
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            String fileName = path.getFileName().toString();
            String uploadId = UUID.randomUUID().toString();
            String sha256 = sha256(bytes);
            int chunkSize = ChartPreviewChannel.SAFE_PAYLOAD_BYTES - 256;
            int totalChunks = (bytes.length + chunkSize - 1) / chunkSize;

            PacketByteBuf startBuf = new PacketByteBuf(Unpooled.buffer());
            startBuf.writeInt(ChartPreviewChannel.OP_FILE_UPLOAD_START);
            writeUtf8String(startBuf, uploadId);
            writeUtf8String(startBuf, fileType);
            writeUtf8String(startBuf, fileName);
            startBuf.writeLong(bytes.length);
            writeUtf8String(startBuf, sha256);
            startBuf.writeInt(totalChunks);
            ClientPlayNetworking.send(new ChartPreviewPayload(startBuf));

            for (int i = 0; i < totalChunks; i++) {
                int offset = i * chunkSize;
                int length = Math.min(chunkSize, bytes.length - offset);
                PacketByteBuf chunkBuf = new PacketByteBuf(Unpooled.buffer());
                chunkBuf.writeInt(ChartPreviewChannel.OP_FILE_UPLOAD_CHUNK);
                writeUtf8String(chunkBuf, uploadId);
                chunkBuf.writeInt(i);
                chunkBuf.writeInt(length);
                chunkBuf.writeBytes(bytes, offset, length);
                ClientPlayNetworking.send(new ChartPreviewPayload(chunkBuf));
            }

            PacketByteBuf endBuf = new PacketByteBuf(Unpooled.buffer());
            endBuf.writeInt(ChartPreviewChannel.OP_FILE_UPLOAD_END);
            writeUtf8String(endBuf, uploadId);
            ClientPlayNetworking.send(new ChartPreviewPayload(endBuf));
            emit(new PreviewEvent(EventType.FILE_UPLOAD_ACK, true, "Uploading " + fileName, 0L, uploadId, fileType, 0, 0, "", ""));
        } catch (IOException exception) {
            emit(new PreviewEvent(EventType.ERROR, false, "File upload failed: " + exception.getMessage(), 0L));
        }
    }

    public void sendEditorOpen(String data) {
        if (!ensureReady()) {
            return;
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(ChartPreviewChannel.OP_C2S_EDITOR_OPEN);
        writeUtf8String(buf, data);
        ClientPlayNetworking.send(new ChartPreviewPayload(buf));
    }

    public void sendEditorSelect(String data) {
        if (!ensureReady()) {
            return;
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(ChartPreviewChannel.OP_C2S_EDITOR_SELECT);
        writeUtf8String(buf, data);
        ClientPlayNetworking.send(new ChartPreviewPayload(buf));
    }

    private void handleServerPacket(PacketByteBuf buf) {
        int opcode = buf.readInt();
        switch (opcode) {
            case ChartPreviewChannel.OP_HELLO_ACK -> {
                boolean ok = buf.readBoolean();
                int protocolVersion = buf.readInt();
                int maxPayloadBytes = buf.readInt();
                String defaultSchematicName = readUtf8String(buf);
                String message = readUtf8String(buf);
                handshakeReady = ok;
                handshakeMessage = message;
                emit(new PreviewEvent(EventType.HELLO_ACK, ok, message, 0L, "", "", protocolVersion, maxPayloadBytes, defaultSchematicName, ""));
            }
            case ChartPreviewChannel.OP_CHART_LOAD_ACK -> {
                boolean ok = buf.readBoolean();
                String error = readUtf8String(buf);
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
                String message = readUtf8String(buf);
                emit(new PreviewEvent(EventType.ERROR, false, message, 0L));
            }
            case ChartPreviewChannel.OP_FILE_UPLOAD_ACK -> {
                String uploadId = readUtf8String(buf);
                String fileType = readUtf8String(buf);
                boolean ok = buf.readBoolean();
                String message = readUtf8String(buf);
                emit(new PreviewEvent(EventType.FILE_UPLOAD_ACK, ok, message, 0L, uploadId, fileType, 0, 0, "", ""));
            }
            case ChartPreviewChannel.OP_S2C_EDITOR_OPEN -> {
                String data = readUtf8String(buf);
                emit(new PreviewEvent(EventType.EDITOR_OPEN, true, null, 0L, "", "", 0, 0, "", data));
            }
            case ChartPreviewChannel.OP_S2C_EDITOR_SELECT -> {
                String data = readUtf8String(buf);
                emit(new PreviewEvent(EventType.EDITOR_SELECT, true, null, 0L, "", "", 0, 0, "", data));
            }
            default -> {
            }
        }
    }

    private void emit(PreviewEvent event) {
        Consumer<PreviewEvent> globalHandler = globalEventHandler;
        if (globalHandler != null) {
            globalHandler.accept(event);
        }
        Consumer<PreviewEvent> handler = eventHandler;
        if (handler != null && event.type() != EventType.EDITOR_OPEN && event.type() != EventType.EDITOR_SELECT) {
            handler.accept(event);
        }
    }

    private boolean ensureCanSend() {
        if (canSend()) {
            return true;
        }
        emit(new PreviewEvent(EventType.ERROR, false, "Preview channel is unavailable. Join a RhythMC-Preview server first.", 0L));
        return false;
    }

    private boolean ensureReady() {
        if (!ensureCanSend()) {
            return false;
        }
        if (handshakeReady) {
            return true;
        }
        emit(new PreviewEvent(EventType.ERROR, false, "Preview server is not ready. Wait for handshake before editing.", 0L));
        return false;
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    private static void writeUtf8String(PacketByteBuf buf, String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readUtf8String(PacketByteBuf buf) {
        int length = buf.readInt();
        if (length <= 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
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

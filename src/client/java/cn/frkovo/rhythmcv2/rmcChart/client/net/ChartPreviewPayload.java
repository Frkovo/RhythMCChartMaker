package cn.frkovo.rhythmcv2.rmcChart.client.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Custom payload for the {@code rhythmc:chart_preview} plugin channel.
 * Wraps raw {@link PacketByteBuf} bytes for Fabric 1.21 custom-payload networking.
 */
public record ChartPreviewPayload(PacketByteBuf buf) implements CustomPayload {
    public static final CustomPayload.Id<ChartPreviewPayload> ID = new CustomPayload.Id<>(
            Identifier.of(ChartPreviewChannel.CHANNEL));

    public static final PacketCodec<PacketByteBuf, ChartPreviewPayload> CODEC = PacketCodec.of(
            (value, dest) -> dest.writeBytes(value.buf),
            src -> new ChartPreviewPayload(new PacketByteBuf(src.readBytes(src.readableBytes())))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

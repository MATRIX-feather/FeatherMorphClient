package xyz.nifeather.morph.shared.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xyz.nifeather.morph.shared.SharedValues;

import java.nio.charset.StandardCharsets;

public record MorphInitChannelPayload(String message) implements CustomPayload
{
    public static final PacketCodec<PacketByteBuf, MorphInitChannelPayload> CODEC = PacketCodec.of(
            (value, buf) -> BufferUtils.writeBufAuto(value.message(), buf),
            buf -> new MorphInitChannelPayload(BufferUtils.readBufAutoFallback(buf))
    );

    public static final CustomPayload.Id<MorphInitChannelPayload> id = new Id<>(SharedValues.initializeChannelIdentifier);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

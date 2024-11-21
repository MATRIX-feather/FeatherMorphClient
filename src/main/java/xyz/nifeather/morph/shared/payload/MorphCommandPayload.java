package xyz.nifeather.morph.shared.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xyz.nifeather.morph.shared.SharedValues;

public record MorphCommandPayload(String content) implements CustomPayload
{
    public static final PacketCodec<PacketByteBuf, MorphCommandPayload> CODEC = PacketCodec.of(
            (value, buf) -> BufferUtils.writeBufAuto(value.content, buf),
            buf -> new MorphCommandPayload(BufferUtils.readBufAuto(buf))
    );

    public static final CustomPayload.Id<MorphCommandPayload> id = new Id<>(SharedValues.commandChannelIdentifier);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

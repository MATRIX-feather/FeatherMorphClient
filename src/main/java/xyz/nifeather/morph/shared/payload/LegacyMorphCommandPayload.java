package xyz.nifeather.morph.shared.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xyz.nifeather.morph.shared.SharedValues;

public record LegacyMorphCommandPayload(String content) implements CustomPayload
{
    public static final PacketCodec<PacketByteBuf, LegacyMorphCommandPayload> CODEC = PacketCodec.of(
            (value, buf) -> BufferUtils.writeCommandBuf(value.content, buf),
            buf -> new LegacyMorphCommandPayload(BufferUtils.readCommandBuf(buf))
    );

    public static final Id<LegacyMorphCommandPayload> id = new Id<>(SharedValues.commandChannelIdentifierLegacy);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

package xyz.nifeather.morph.shared.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xyz.nifeather.morph.client.ServerHandler;

public record MorphCommandPayload(String content) implements CustomPayload
{
    public static final PacketCodec<PacketByteBuf, MorphCommandPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.content()),
            buf -> new MorphCommandPayload(MorphInitChannelPayload.readString(buf))
    );

    public static final CustomPayload.Id<MorphCommandPayload> id = new Id<>(ServerHandler.commandChannelIdentifier);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

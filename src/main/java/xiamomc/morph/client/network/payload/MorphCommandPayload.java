package xiamomc.morph.client.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import xiamomc.morph.client.ServerHandler;

public record MorphCommandPayload(String content) implements CustomPayload
{
    public static final PacketCodec<PacketByteBuf, MorphCommandPayload> CODEC = PacketCodecs.STRING
            .xmap(MorphCommandPayload::new, MorphCommandPayload::content)
            .cast();

    public static final CustomPayload.Id<MorphCommandPayload> id = new Id<>(ServerHandler.commandChannelIdentifier);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

package xiamomc.morph.client.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import xiamomc.morph.client.ServerHandler;

public record MorphVersionChannelPayload(int protocolVersion) implements CustomPayload
{
    public static final PacketCodec<PacketByteBuf, MorphVersionChannelPayload> CODEC = PacketCodecs.INTEGER
            .xmap(MorphVersionChannelPayload::new, MorphVersionChannelPayload::protocolVersion)
            .cast();

    public static final Id<MorphVersionChannelPayload> id = new Id<>(ServerHandler.versionChannelIdentifier);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

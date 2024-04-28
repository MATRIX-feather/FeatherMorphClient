package xiamomc.morph.client.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import xiamomc.morph.client.ServerHandler;

public record MorphInitChannelPayload(String message) implements CustomPayload
{
    public static final PacketCodec<RegistryByteBuf, MorphInitChannelPayload> CODEC = PacketCodecs.string(32768)
            .xmap(MorphInitChannelPayload::new, MorphInitChannelPayload::message)
            .cast();

    public static final CustomPayload.Id<MorphInitChannelPayload> id = new Id<>(ServerHandler.initializeChannelIdentifier);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

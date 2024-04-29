package xiamomc.morph.client.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import xiamomc.morph.client.ServerHandler;

import java.nio.charset.StandardCharsets;

public record MorphVersionChannelPayload(int protocolVersion) implements CustomPayload
{
    public static final PacketCodec<PacketByteBuf, MorphVersionChannelPayload> CODEC  = PacketCodec.of(
            (value, buf) -> buf.writeInt(value.protocolVersion()),
            buf -> new MorphVersionChannelPayload(parseInt(buf))
    );

    public static int parseInt(PacketByteBuf buf)
    {
        //System.out.println("Buf is '" + buf.toString(StandardCharsets.UTF_8) + "' :: with hashCode" + buf.hashCode());
        int read = -1;
        try
        {
            read = buf.readInt();
        }
        catch (Throwable t)
        {
            System.err.println("[FeatherMorph] Error parsing protocol version from server: " + t.getMessage());
            t.printStackTrace();
        }

        buf.clear();
        return read;
    }

    public static final Id<MorphVersionChannelPayload> id = new Id<>(ServerHandler.versionChannelIdentifier);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

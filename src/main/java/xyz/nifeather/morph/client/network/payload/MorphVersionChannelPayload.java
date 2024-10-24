package xyz.nifeather.morph.client.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xyz.nifeather.morph.client.ServerHandler;

public record MorphVersionChannelPayload(int protocolVersion) implements CustomPayload
{
    public MorphVersionChannelPayload(String string)
    {
        this(parse(string));
    }

    // Client --String-> Server
    // Server --Integer-> Client
    // :(
    public static final PacketCodec<PacketByteBuf, MorphVersionChannelPayload> CODEC  = PacketCodec.of(
            (value, buf) -> buf.writeInt(value.protocolVersion()), //Server
            buf -> new MorphVersionChannelPayload(parseBuf(buf)) // Client
    );

    public int getProtocolVersion()
    {
        return protocolVersion;
    }

    private static int parse(String input)
    {
        try
        {
            return Integer.parseInt(input);
        }
        catch (Throwable t)
        {
            System.err.println("[FeatherMorph] Failed to parse protocol version from input: " + t.getMessage());
        }

        return 1;
    }

    public static int parseBuf(PacketByteBuf buf)
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

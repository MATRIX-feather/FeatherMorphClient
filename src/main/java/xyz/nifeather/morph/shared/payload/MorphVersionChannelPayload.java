package xyz.nifeather.morph.shared.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xyz.nifeather.morph.client.ServerHandler;
import xyz.nifeather.morph.shared.SharedValues;

import java.nio.charset.StandardCharsets;

public record MorphVersionChannelPayload(int protocolVersion) implements CustomPayload
{
    public MorphVersionChannelPayload(String string)
    {
        this(parseInt(string));
    }

    // Client --String-> Server
    // Bukkit Server --Integer-> Client
    // Fabric Server --String-> Client
    // :(
    public static final PacketCodec<PacketByteBuf, MorphVersionChannelPayload> CODEC  = PacketCodec.of(
            (value, buf) -> buf.writeBytes(MorphInitChannelPayload.writeString("" + value.protocolVersion)), //Server
            buf -> new MorphVersionChannelPayload(parseBuf(buf)) // Client
    );

    public int getProtocolVersion()
    {
        return protocolVersion;
    }

    private static int parseInt(String input)
    {
        try
        {
            return Integer.parseInt(input);
        }
        catch (Throwable t)
        {
            SharedValues.LOGGER.error("Failed to parse protocol version from input: " + t.getMessage());
        }

        return 1;
    }

    public static int parseBuf(PacketByteBuf buf)
    {
        //System.out.println("Buf is '" + buf.toString(StandardCharsets.UTF_8) + "' :: with hashCode" + buf.hashCode());
        int read = -1;

        try
        {
            // If from a bukkit server
            read = buf.readInt();
        }
        catch (Throwable ignored)
        {
        }

        if (read == -1)
        {
            try
            {
                // If from a fabric server
                var str = buf.toString(StandardCharsets.UTF_8);
                read = Integer.parseInt(str);
            }
            catch (Throwable t)
            {
                SharedValues.LOGGER.error("Error parsing protocol version!");
            }
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

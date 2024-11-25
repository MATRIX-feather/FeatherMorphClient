package xyz.nifeather.morph.shared.payload;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xyz.nifeather.morph.shared.SharedValues;

import java.nio.charset.StandardCharsets;

@Environment(EnvType.CLIENT)
public record LegacyMorphVersionChannelPayload(int protocolVersion) implements CustomPayload
{
    // Client --String-> Server
    // Bukkit Server --Integer-> Client
    // Fabric Server --String-> Client
    // :(
    public static final PacketCodec<PacketByteBuf, LegacyMorphVersionChannelPayload> CODEC  = PacketCodec.of(
            (value, buf) -> BufferUtils.writeVersionBufAuto(value.protocolVersion, buf), //Client -> Server
            buf -> new LegacyMorphVersionChannelPayload(BufferUtils.readVersionBuf(buf)) // Server -> Client
    );

    public int getProtocolVersion()
    {
        return protocolVersion;
    }

    public static int parseInt(String input)
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

        // Kept for legacy servers.
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

    public static final Id<LegacyMorphVersionChannelPayload> id = new Id<>(SharedValues.versionChannelIdentifierLegacy);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

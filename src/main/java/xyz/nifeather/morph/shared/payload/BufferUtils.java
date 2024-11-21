package xyz.nifeather.morph.shared.payload;

import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.shared.SharedValues;

import java.nio.charset.StandardCharsets;

public class BufferUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger("MorphClient$BufferUtils");

    //region Write

    // int
    public static void writeVersionBufAuto(int content, PacketByteBuf buf)
    {
        if (SharedValues.client_UseNewPacketSerializeMethod)
            writeIntBuf(content, buf);
        else
            writeIntBufLegacy(content, buf);
    }

    public static void writeIntBuf(int content, PacketByteBuf buf)
    {
        buf.writeInt(content);
    }

    public static void writeIntBufLegacy(int content, PacketByteBuf buf)
    {
        var str = "" + content;
        var bytes = str.getBytes(StandardCharsets.UTF_8);

        buf.writeBytes(bytes);
    }

    // string
    public static void writeBufAuto(String content, PacketByteBuf buf)
    {
        if (SharedValues.client_UseNewPacketSerializeMethod)
            writeBuf(content, buf);
        else
            writeBufLegacy(content, buf);
    }

    public static void writeBuf(String content, PacketByteBuf buf)
    {
        buf.writeString(content);
    }

    public static void writeBufLegacy(String content, PacketByteBuf buf)
    {
        buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));
    }

    //endregion Write

    //region Read

    // int
    public static int readVersionBufAuto(PacketByteBuf buf)
    {
        try
        {
            return buf.readInt();
        }
        catch (Throwable t)
        {
            LOGGER.error("Can't read version from legacy buf: " + t.getMessage());
            return 1;
        }
    }

    // string

    public static String readBufAutoFallback(PacketByteBuf buf)
    {
        if (SharedValues.client_UseNewPacketSerializeMethod)
        {
            try
            {
                return readBuf(buf);
            }
            catch (Throwable t)
            {
                LOGGER.info("Can't read buffer with readBuf(), trying legacy method...");
                return readBufLegacy(buf);
            }
        }
        else
            return readBufLegacy(buf);
    }

    public static String readBufAuto(PacketByteBuf buf)
    {
        if (SharedValues.client_UseNewPacketSerializeMethod)
        {
            return readBuf(buf);
        }
        else
            return readBufLegacy(buf);
    }

    public static String readBuf(PacketByteBuf buf)
    {
        return buf.readString();
    }

    public static String readBufLegacy(PacketByteBuf buf)
    {
        var directBuffer = buf.readBytes(buf.readableBytes());
        var dst = new byte[directBuffer.capacity()];
        directBuffer.getBytes(0, dst);

        buf.clear();
        directBuffer.clear();
        return new String(dst, StandardCharsets.UTF_8);
    }

    //endregion Read
}

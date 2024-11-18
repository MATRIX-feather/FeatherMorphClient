package xyz.nifeather.morph.shared.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xyz.nifeather.morph.shared.SharedValues;

import java.nio.charset.StandardCharsets;

public record MorphInitChannelPayload(String message) implements CustomPayload
{
    public static final PacketCodec<PacketByteBuf, MorphInitChannelPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.message()),
            buf -> new MorphInitChannelPayload(readString(buf))
    );

    public static byte[] writeString(String string)
    {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    public static String readString(PacketByteBuf buf)
    {
        int lastRead = buf.readerIndex();
        int lastWrite = buf.writerIndex();

        try
        {
            var content = buf.readString();
            buf.clear();

            return content;
        }
        catch (Throwable t)
        {
            System.out.println("Failed to decode in modern pattern.");
            buf.readerIndex(lastRead);
            buf.writerIndex(lastWrite);
        }

        //System.out.println("Buf is '" + buf.toString(StandardCharsets.UTF_8) + "' :: with hashCode" + buf.hashCode());

        var directBuffer = buf.readBytes(buf.readableBytes());
        var dst = new byte[directBuffer.capacity()];
        directBuffer.getBytes(0, dst);

        buf.clear();
        directBuffer.clear();
        return new String(dst, StandardCharsets.UTF_8);
    }

    public static final CustomPayload.Id<MorphInitChannelPayload> id = new Id<>(SharedValues.initializeChannelIdentifier);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

package xiamomc.morph.client.network.payload;

import com.google.common.io.ByteStreams;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xiamomc.morph.client.ServerHandler;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record MorphInitChannelPayload(String message) implements CustomPayload
{
    public static final PacketCodec<PacketByteBuf, MorphInitChannelPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.message()),
            buf -> new MorphInitChannelPayload(readString(buf))
    );

    public static byte[] writeString(String string)
    {
        var out = ByteStreams.newDataOutput();
        out.writeUTF(string);

        return out.toByteArray();
    }

    public static String readString(PacketByteBuf buf)
    {
        //System.out.println("Buf is '" + buf.toString(StandardCharsets.UTF_8) + "' :: with hashCode" + buf.hashCode());

        var directBuffer = buf.readBytes(buf.readableBytes());
        var dst = new byte[directBuffer.capacity()];
        directBuffer.getBytes(0, dst);

        //System.out.printf("DST is '%s'%n", Arrays.toString(dst));

        var input = ByteStreams.newDataInput(dst);
        var asByteBuf = input.readUTF();

        buf.clear();
        directBuffer.clear();
        return asByteBuf;
    }

    public static final CustomPayload.Id<MorphInitChannelPayload> id = new Id<>(ServerHandler.initializeChannelIdentifier);

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return id;
    }
}

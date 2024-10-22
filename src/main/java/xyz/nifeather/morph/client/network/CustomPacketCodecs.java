package xyz.nifeather.morph.client.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.string.StringDecoder;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.encoding.StringEncoding;
import net.minecraft.network.encoding.VarInts;

import java.nio.charset.StandardCharsets;

public class CustomPacketCodecs
{
    /**
     * {@return a codec for a string value with maximum length {@code maxLength}}
     *
     * @see #STRING
     * @see net.minecraft.network.PacketByteBuf#readString(int)
     * @see net.minecraft.network.PacketByteBuf#writeString(String, int)
     */
    public static PacketCodec<ByteBuf, String> string(final int maxLength) {
        return new PacketCodec<>(){

            @Override
            public String decode(ByteBuf byteBuf)
            {
                System.out.println("~DECODE PACKET WITH MAX LENGTH " + maxLength);
                return StringEncoding.decode(byteBuf, maxLength);
            }

            @Override
            public void encode(ByteBuf byteBuf, String string)
            {
                System.out.println("~ENCODE PACKET WITH CONTENT '%s'".formatted(string));
                StringEncoding.encode(byteBuf, string, maxLength);
            }
        };
    }
}

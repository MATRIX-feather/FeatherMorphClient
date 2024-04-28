package xiamomc.morph.client.mixin;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.network.handler.PacketCodecDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Mixin(PacketCodecDispatcher.class)
public class CodecDispatcherMixin<B extends ByteBuf, V, T>
{
    @Shadow @Final private List<PacketCodecDispatcher.PacketType<B, V, T>> packetTypes;

     @Inject(
            method = "decode(Lio/netty/buffer/ByteBuf;)Ljava/lang/Object;",
            at = @At(value = "INVOKE", target = "Lio/netty/handler/codec/DecoderException;<init>(Ljava/lang/String;Ljava/lang/Throwable;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void fm$onError(B byteBuf, CallbackInfoReturnable<V> cir, int i, PacketCodecDispatcher.PacketType packetType, Exception exception)
    {
        System.err.println();
        System.err.println("- x - x - x - x - x - x - x - x - x - x - x -");
        System.err.println("Byte buf is " + (byteBuf).toString(StandardCharsets.UTF_8));
        System.err.println("- x - x - x - x - x - x - x - x - x - x - x -");
        System.err.println("Id is " + packetType.id());
        System.err.println("Exception message is " + exception.getMessage());
        exception.printStackTrace();
        System.err.println("- x - x - x - x - x - x - x - x - x - x - x -");

        System.err.println("All available types at this moment:");

        for (PacketCodecDispatcher.PacketType<B, V, T> type : packetTypes)
            System.err.println(" - " + type.id());

        System.err.println("- x - x - x - x - x - x - x - x - x - x - x -");
        System.err.println();
    }
}

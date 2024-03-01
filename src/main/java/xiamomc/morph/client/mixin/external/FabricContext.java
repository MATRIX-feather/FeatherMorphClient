package xiamomc.morph.client.mixin.external;

import io.netty.util.internal.logging.Slf4JLoggerFactory;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.impl.client.rendering.WorldRenderContextImpl;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamomc.morph.client.MorphClient;

@Mixin(WorldRenderContextImpl.class)
public class FabricContext
{
    @Shadow private ClientWorld world;

    @Shadow private Entity entity;
    @Unique
    private boolean hooked;

    @Inject(method = "prepare", at = @At("HEAD"))
    private void onPrepare(WorldRenderer worldRenderer, MatrixStack matrixStack, float tickDelta, long limitTime, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, VertexConsumerProvider consumers, Profiler profiler, boolean advancedTranslucency, ClientWorld world, CallbackInfo ci)
    {
        if (!hooked)
        {
            MorphClient.LOGGER.info("~~~HOOKING EVENTS FOR " + this);
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            {
                MorphClient.LOGGER.info("Clearing world field");
                this.world = null;
                this.entity = null;
            });
        }

        hooked = true;
    }
}

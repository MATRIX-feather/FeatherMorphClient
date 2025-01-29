package xyz.nifeather.morph.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xyz.nifeather.morph.client.graphics.PlayerRenderHelper;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin
{
    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Inject(method = "method_62214",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilderStorage;getEntityVertexConsumers()Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;",
                    shift = At.Shift.AFTER)
    )
    private void featherMorph$onRender(Fog fog, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f, Matrix4f matrix4f2, Handle handle, Handle handle2, Handle handle3, boolean bl, Frustum frustum, Handle handle4, CallbackInfo ci, @Local MatrixStack matrixStack)
    {
        var featherMorph$vertex = this.bufferBuilders.getEntityVertexConsumers();
        PlayerRenderHelper.instance().renderCrystalBeam(renderTickCounter, matrixStack, featherMorph$vertex, LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }
}

package xyz.nifeather.morph.client.mixin;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.graphics.PlayerRenderHelper;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin
{
    @Shadow @Final private BufferBuilderStorage bufferBuilders;

/*
    @ModifyVariable(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;<init>()V"))
    private MatrixStack featherMorph$captureMatrixStack(MatrixStack stack)
    {
        this.featherMorph$currentStack = stack;

        return stack;
    }
*/
    @Nullable
    @Unique
    private MatrixStack featherMorph$currentStack;

    @Inject(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilderStorage;getEntityVertexConsumers()Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;", shift = At.Shift.AFTER))
    private void featherMorph$onRender(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci)
    {
        if (featherMorph$currentStack != null)
        {
            var featherMorph$vertex = this.bufferBuilders.getEntityVertexConsumers();
            PlayerRenderHelper.instance().renderCrystalBeam(tickCounter, featherMorph$currentStack, featherMorph$vertex, 0xFFFFFF);

            featherMorph$currentStack = null;
        }
    }
}

package xyz.nifeather.morph.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.nifeather.morph.client.graphics.PlayerRenderHelper;
import xyz.nifeather.morph.client.syncers.ClientDisguiseSyncer;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin
{
    @Shadow @Final private MinecraftClient client;

    @Redirect(
            method = "renderArmHoldingItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;Z)V")
    )
    private void morphclient$renderArmHoldingItem_left(PlayerEntityRenderer instance, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Identifier skinTexture, boolean sleeveVisible)
    {
        if (morphclient$hasClientSyncer())
            morphclient$renderLeftArm(matrices, vertexConsumers, light);
        else
            instance.renderLeftArm(matrices, vertexConsumers, light, skinTexture, this.client.player.isPartVisible(PlayerModelPart.LEFT_SLEEVE));
    }

    @Redirect(
            method = "renderArmHoldingItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;Z)V")
    )
    private void morphclient$renderArmHoldingItem_right(PlayerEntityRenderer instance, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Identifier skinTexture, boolean sleeveVisible)
    {
        if (morphclient$hasClientSyncer())
            morphclient$renderRightArm(matrices, vertexConsumers, light);
        else
            instance.renderRightArm(matrices, vertexConsumers, light, skinTexture, this.client.player.isPartVisible(PlayerModelPart.RIGHT_SLEEVE));
    }

    @Redirect(
            method = "renderArm",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;Z)V")
    )
    private void morphclient$renderArmHoldingItem_right_alt(PlayerEntityRenderer instance, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Identifier skinTexture, boolean sleeveVisible)
    {
        if (morphclient$hasClientSyncer())
            morphclient$renderRightArm(matrices, vertexConsumers, light);
        else
            instance.renderRightArm(matrices, vertexConsumers, light, skinTexture, this.client.player.isPartVisible(PlayerModelPart.RIGHT_SLEEVE));
    }

    @Redirect(
            method = "renderArm",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;Z)V")
    )
    private void morphclient$renderArmHoldingItem_left_alt(PlayerEntityRenderer instance, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Identifier skinTexture, boolean sleeveVisible)
    {
        if (morphclient$hasClientSyncer())
            morphclient$renderLeftArm(matrices, vertexConsumers, light);
        else
            instance.renderLeftArm(matrices, vertexConsumers, light, skinTexture, this.client.player.isPartVisible(PlayerModelPart.RIGHT_SLEEVE));
    }

    @Unique
    private void morphclient$renderLeftArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light)
    {
        morphclient$rendererHelper.renderingLeftPart = true;

        this.morphclient$onArmRender(matrices, vertexConsumers, light);
    }

    @Unique
    private void morphclient$renderRightArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light)
    {
        morphclient$rendererHelper.renderingLeftPart = false;

        this.morphclient$onArmRender(matrices, vertexConsumers, light);
    }

    @Unique
    private static final PlayerRenderHelper morphclient$rendererHelper = PlayerRenderHelper.instance();

    @Unique
    private boolean morphclient$hasClientSyncer()
    {
        var clientSyncer = ClientDisguiseSyncer.getCurrentInstance();
        return clientSyncer != null && !clientSyncer.disposed();
    }

    @Unique
    private void morphclient$onArmRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                           int light)
    {
        morphclient$rendererHelper.onArmDrawCall(
                matrices, vertexConsumers,
                light
        );
    }
}

package xiamomc.morph.client.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.entities.MorphLocalPlayer;
import xiamomc.morph.client.graphics.PlayerRenderHelper;
import xiamomc.morph.client.syncers.ClientDisguiseSyncer;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>
{
    @Unique
    private static final PlayerRenderHelper morphclient$rendererHelper = PlayerRenderHelper.instance();

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(
            method = "getPositionOffset(Lnet/minecraft/client/network/AbstractClientPlayerEntity;F)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"),
            cancellable = true)
    private void morphclient$onPosCall(AbstractClientPlayerEntity abstractClientPlayerEntity, float f, CallbackInfoReturnable<Vec3d> cir)
    {
        var syncer = ClientDisguiseSyncer.getCurrentInstance();

        if (syncer != null
                && !syncer.disposed()
                && syncer.getBindingPlayer().equals(abstractClientPlayerEntity)
                && MorphClient.getInstance().morphManager.selfVisibleEnabled.get())
        {
            if (!(syncer.getDisguiseInstance() instanceof MorphLocalPlayer localPlayer))
                cir.setReturnValue(super.getPositionOffset(abstractClientPlayerEntity, f));
            else if (!localPlayer.isInSneakingPose())
                cir.setReturnValue(super.getPositionOffset(abstractClientPlayerEntity, f));
        }
    }

    @Inject(
            method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void morphclient$onLabelDrawCall(AbstractClientPlayerEntity abstractClientPlayerEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, CallbackInfo ci)
    {
        if (morphclient$rendererHelper.shouldHideLabel(abstractClientPlayerEntity))
            ci.cancel();
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    )
    public void morphclient$onRenderCall(LivingEntityRenderer<?, ?> renderer, LivingEntity player, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i)
    {
        if (!morphclient$rendererHelper.overrideEntityRender((AbstractClientPlayerEntity)player, f, g, matrixStack, vertexConsumerProvider, i))
            super.render((AbstractClientPlayerEntity) player, f, g, matrixStack, vertexConsumerProvider, i);
    }

    //[FirstPersonModel](https://github.com/tr7zw/FirstPersonModel) compat
    @Inject(method = "renderLeftArm", at = @At(value = "HEAD"))
    public void morphclient$onLeftArmDrawCall(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, CallbackInfo ci)
    {
        morphclient$rendererHelper.renderingLeftPart = true;
    }

    @Inject(method = "renderRightArm", at = @At(value = "HEAD"))
    public void morphclient$onRightArmDrawCall(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, CallbackInfo ci)
    {
        morphclient$rendererHelper.renderingLeftPart = false;
    }

    @Inject(method = "renderArm", at = @At(value = "HEAD"), cancellable = true)
    public void morphclient$onArmDrawCall(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo ci)
    {
        if (morphclient$rendererHelper.onArmDrawCall(matrices, vertexConsumers, light, player, arm, sleeve))
            ci.cancel();
    }
}

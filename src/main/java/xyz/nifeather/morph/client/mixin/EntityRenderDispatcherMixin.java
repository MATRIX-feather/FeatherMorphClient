package xyz.nifeather.morph.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.DisguiseInstanceTracker;
import xyz.nifeather.morph.client.entities.IMorphClientEntity;
import xyz.nifeather.morph.client.graphics.EntityRendererHelper;
import xyz.nifeather.morph.client.graphics.PlayerRenderHelper;
import xyz.nifeather.morph.shared.entities.IMorphEntity;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin
{
    @Shadow @Final private TextRenderer textRenderer;

    @ModifyVariable(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            index = 1,
            argsOnly = true)
    public Entity morphclient$modifyEntityToRender(Entity source)
    {
        if (PlayerRenderHelper.instance().skipRender)
            return source;

        if (((IMorphClientEntity)source).featherMorph$bypassesDispatcherRedirect())
            return source;

        var instanceTracker = DisguiseInstanceTracker.getInstance();

        var syncer = instanceTracker.getSyncerFor(source);
        if (syncer == null) return source;

        var morphclient$instance = syncer.getDisguiseInstance();
        return morphclient$instance == null ? source : morphclient$instance;
    }

    @Inject(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"
            )
    )
    public <E extends Entity, S extends EntityRenderState> void morphclient$tryRenderRevealName(E entity, double x, double y, double z, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityRenderer<? super E, S> renderer, CallbackInfo ci)
    {
        EntityRendererHelper.instance.renderRevealNameIfPossible((EntityRenderDispatcher)(Object) this, entity, textRenderer, matrices, vertexConsumers);
    }
}

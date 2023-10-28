package xiamomc.morph.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamomc.morph.client.graphics.EntityRendererHelper;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin
{
    @Shadow @Final private TextRenderer textRenderer;

    @Shadow @Final protected EntityRenderDispatcher dispatcher;

    @Inject(method = "render",
            at = @At(value = "HEAD"))
    public void onRender(Entity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci)
    {
        EntityRendererHelper.instance.renderRevealNameIfPossible(this.dispatcher, entity, textRenderer, matrices, vertexConsumers, ci, light);
    }
}

package xiamo.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import xiamo.morph.client.EntityCache;
import xiamo.morph.client.MorphClient;
import xiamo.morph.client.mixin.accessors.LivingRendererAccessor;

public class PlayerRenderHelper
{
    public PlayerRenderHelper()
    {
        MorphClient.selfViewIdentifier.onValueChanged((o, n) ->
        {
            //实体同步给MorphClient那里做了
            this.entity = EntityCache.getEntity(n);
        });
    }

    private Entity entity = null;

    public boolean onDrawCall(LivingEntity player, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i)
    {
        if (entity == null || player != MinecraftClient.getInstance().player || !MorphClient.getInstance().getModConfigData().clientViewVisible()) return false;

        var disguiseRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);

        //LoggerFactory.getLogger("d").info(player.getName() + " :: " + player.getDataTracker().get(MorphLocalPlayer.getPMPMask()));
        disguiseRenderer.render(entity, f, g, matrixStack, vertexConsumerProvider, i);
        return true;
    }

    public boolean renderingLeftPart;

    @SuppressWarnings("rawtypes")
    public boolean onArmDrawCall(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve)
    {
        if (entity == null || player != MinecraftClient.getInstance().player || !MorphClient.getInstance().getModConfigData().clientViewVisible()) return false;

        var disguiseRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);

        if (disguiseRenderer instanceof LivingEntityRenderer livingEntityRenderer)
        {
            var useLeftPart = renderingLeftPart;

            var model = livingEntityRenderer.getModel();
            ModelPart targetArm = null;

            if (entity instanceof MorphLocalPlayer)
            {
                var renderer = (PlayerEntityRenderer) livingEntityRenderer;

                if (useLeftPart)
                    renderer.renderLeftArm(matrices, vertexConsumers, light, (MorphLocalPlayer)entity);
                else
                    renderer.renderRightArm(matrices, vertexConsumers, light, (MorphLocalPlayer)entity);

                return true;
            }
            else if (model instanceof BipedEntityModel<?> bipedEntityModel)
            {
                targetArm = useLeftPart ? bipedEntityModel.leftArm : bipedEntityModel.rightArm;
            }

            if (targetArm != null)
            {
                var layer = ((LivingRendererAccessor) livingEntityRenderer).callGetRenderLayer((LivingEntity) entity, true, false, true);
                layer = layer == null ? RenderLayer.getSolid() : layer;

                model.setAngles(entity, 0, 0, 0, 0, 0);
                model.handSwingProgress = 0;

                targetArm.pitch = 0;
                targetArm.render(matrices, vertexConsumers.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);

                return true;
            }
        }

        return false;
    }
}

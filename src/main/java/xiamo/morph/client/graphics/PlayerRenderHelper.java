package xiamo.morph.client.graphics;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3f;
import org.jetbrains.annotations.Nullable;
import xiamo.morph.client.EntityCache;
import xiamo.morph.client.MorphClient;
import xiamo.morph.client.MorphLocalPlayer;
import xiamo.morph.client.mixin.accessors.LivingRendererAccessor;

import java.util.Map;

public class PlayerRenderHelper
{
    public PlayerRenderHelper()
    {
        MorphClient.selfViewIdentifier.onValueChanged((o, n) ->
        {
            //实体同步给DisguiseSyncer那里做了
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

    private final Map<EntityType<?>, ModelInfo> typeModelPartMap = new Object2ObjectOpenHashMap<>();

    private record ModelInfo(@Nullable ModelPart left, @Nullable ModelPart right, Vec3f offset, @Nullable Vec3f scale)
    {
        @Nullable
        public ModelPart getPart(boolean isLeftArm)
        {
            return isLeftArm ? left : right;
        }
    }

    public ModelInfo tryGetModel(EntityType<?> type, EntityModel<?> model)
    {
        var map = typeModelPartMap.getOrDefault(type, null);

        if (map != null)
            return map;

        ModelPart leftPart = null;
        ModelPart rightPart = null;
        Vec3f offset = new Vec3f(0, 0, 0);
        Vec3f scale = null;

        if (model instanceof BipedEntityModel<?> bipedEntityModel)
        {
            leftPart = bipedEntityModel.leftArm;
            rightPart = bipedEntityModel.rightArm;
        }
        else if (model instanceof SinglePartEntityModel<?> singlePartEntityModel)
        {
            leftPart = singlePartEntityModel.getChild(EntityModelPartNames.LEFT_ARM).orElse(null);
            rightPart = singlePartEntityModel.getChild(EntityModelPartNames.RIGHT_ARM).orElse(null);

            if (type == EntityType.IRON_GOLEM)
            {
                scale = new Vec3f(0.75f, 0.75f, 0.75f);
                offset.set(0, -0.2f, 0);
            }
            else if (type == EntityType.ALLAY)
            {
                offset.set(0, 0.2f, 0.1f);
                scale = new Vec3f(1.5f, 1.5f, 1.5f);
            }
        }

        map = new ModelInfo(leftPart, rightPart, offset, scale);
        typeModelPartMap.put(type, map);

        return map;
    }

    @SuppressWarnings("rawtypes")
    public boolean onArmDrawCall(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve)
    {
        if (entity == null || player != MinecraftClient.getInstance().player || !MorphClient.getInstance().getModConfigData().clientViewVisible()) return false;

        var disguiseRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);

        if (disguiseRenderer instanceof LivingEntityRenderer livingEntityRenderer)
        {
            var useLeftPart = renderingLeftPart;

            var model = livingEntityRenderer.getModel();
            ModelPart targetArm;
            ModelInfo modelInfo;

            if (entity instanceof MorphLocalPlayer)
            {
                var renderer = (PlayerEntityRenderer) livingEntityRenderer;

                if (useLeftPart)
                    renderer.renderLeftArm(matrices, vertexConsumers, light, (MorphLocalPlayer)entity);
                else
                    renderer.renderRightArm(matrices, vertexConsumers, light, (MorphLocalPlayer)entity);

                return true;
            }
            else
            {
                modelInfo = tryGetModel(entity.getType(), model);
                targetArm = modelInfo.getPart(useLeftPart);
            }

            if (targetArm != null)
            {
                var layer = ((LivingRendererAccessor) livingEntityRenderer).callGetRenderLayer((LivingEntity) entity, true, false, true);
                layer = layer == null ? RenderLayer.getSolid() : layer;

                model.setAngles(entity, 0, 0, 0, 0, 0);
                model.handSwingProgress = 0;

                var scale = modelInfo.scale;

                if (scale != null)
                    matrices.scale(scale.getX(), scale.getY(), scale.getZ());

                var offset = modelInfo.offset;
                matrices.translate(offset.getX(), offset.getY(), offset.getZ());

                //targetArm.roll = 0f;
                targetArm.pitch = 0;
                targetArm.render(matrices, vertexConsumers.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);

                return true;
            }
        }

        return false;
    }
}

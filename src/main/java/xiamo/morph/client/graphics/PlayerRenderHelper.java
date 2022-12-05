package xiamo.morph.client.graphics;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;
import org.jetbrains.annotations.Nullable;
import xiamo.morph.client.EntityCache;
import xiamo.morph.client.MorphClient;
import xiamo.morph.client.MorphLocalPlayer;
import xiamo.morph.client.mixin.accessors.DragonEntityRendererAccessor;
import xiamo.morph.client.mixin.accessors.LivingRendererAccessor;

import java.util.List;
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

    private record ModelInfo(@Nullable ModelPart left, @Nullable ModelPart right, Vec3f offset, Vec3f scale)
    {
        @Nullable
        public ModelPart getPart(boolean isLeftArm)
        {
            return isLeftArm ? left : right;
        }
    }

    public ModelInfo tryGetModel(EntityType<?> type, EntityModel<?> sourceModel)
    {
        var map = typeModelPartMap.getOrDefault(type, null);

        if (map != null)
            return map;

        ModelPart model = null;

        //尝试获取对应的模型
        //有些模型变换会影响全局渲染，所以我们需要创建一个新的模型（比方说雪傀儡和铁傀儡的手臂模型）
        var targetEntry = EntityModels.getModels().entrySet().stream()
                .filter(e -> e.getKey().getId().equals(EntityType.getId(type))).findFirst().orElse(null);

        if (targetEntry != null)
            model = targetEntry.getValue().createModel();

        ModelPart leftPart = null;
        ModelPart rightPart = null;
        Vec3f offset = new Vec3f(0, 0, 0);
        Vec3f scale = ModelWorkarounds.WorkaroundMeta.VECONE();

        if (model != null)
        {
            var leftPartNames = List.of(
                    EntityModelPartNames.LEFT_ARM,
                    EntityModelPartNames.LEFT_LEG,
                    EntityModelPartNames.LEFT_FRONT_LEG,
                    EntityModelPartNames.LEFT_HIND_LEG,
                    EntityModelPartNames.LEFT_FOOT,
                    EntityModelPartNames.LEFT_FRONT_FOOT,
                    EntityModelPartNames.LEFT_HIND_FOOT,
                    "part9"
            );

            var rightPartNames = List.of(
                    EntityModelPartNames.RIGHT_ARM,
                    EntityModelPartNames.RIGHT_LEG,
                    EntityModelPartNames.RIGHT_FRONT_LEG,
                    EntityModelPartNames.RIGHT_HIND_LEG,
                    EntityModelPartNames.RIGHT_FOOT,
                    EntityModelPartNames.RIGHT_FRONT_FOOT,
                    EntityModelPartNames.RIGHT_HIND_FOOT,
                    "part9"
            );

            if (sourceModel instanceof BipedEntityModel<?> bipedEntityModel)
            {
                leftPart = bipedEntityModel.leftArm;
                rightPart = bipedEntityModel.rightArm;
            }
            else
            {
                leftPart = this.tryGetChild(model, leftPartNames);
                rightPart = this.tryGetChild(model, rightPartNames);

                var meta = ModelWorkarounds.apply(type, leftPart, rightPart);

                offset = meta.offset();
                scale = meta.scale();
            }
        }

        map = new ModelInfo(leftPart, rightPart, offset, scale);
        typeModelPartMap.put(type, map);

        return map;
    }

    private ModelPart tryGetChild(ModelPart modelPart, String childName)
    {
        //From SinglePartEntityModel#getChild(String name)
        return modelPart.traverse().filter(part -> part.hasChild(childName)).findFirst().map(part -> part.getChild(childName)).orElse(null);
    }

    private ModelPart tryGetChild(ModelPart modelPart, List<String> childNames)
    {
        ModelPart part = null;

        for (var s : childNames)
        {
            part = tryGetChild(modelPart, s);

            if (part != null) break;
        }

        return part;
    }

    private final RenderLayer dragonLayer = RenderLayer.getEntityCutoutNoCull(new Identifier("textures/entity/enderdragon/dragon.png"));

    @SuppressWarnings("rawtypes")
    public boolean onArmDrawCall(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve)
    {
        if (entity == null || player != MinecraftClient.getInstance().player || !MorphClient.getInstance().getModConfigData().clientViewVisible()) return false;

        EntityRenderer<?> disguiseRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);

        ModelPart targetArm;
        ModelInfo modelInfo;
        RenderLayer layer = null;
        EntityModel model = null;

        if (disguiseRenderer instanceof EnderDragonEntityRenderer enderDragonEntityRenderer)
        {
            model = ((DragonEntityRendererAccessor) enderDragonEntityRenderer).getModel();
            layer = dragonLayer;
        }
        else if (disguiseRenderer instanceof LivingEntityRenderer livingEntityRenderer)
        {
            model = livingEntityRenderer.getModel();

            if (entity instanceof MorphLocalPlayer)
            {
                var renderer = (PlayerEntityRenderer) livingEntityRenderer;

                if (renderingLeftPart)
                    renderer.renderLeftArm(matrices, vertexConsumers, light, (MorphLocalPlayer)entity);
                else
                    renderer.renderRightArm(matrices, vertexConsumers, light, (MorphLocalPlayer)entity);

                return true;
            }

            layer = ((LivingRendererAccessor) livingEntityRenderer).callGetRenderLayer((LivingEntity) entity, true, false, true);
        }

        modelInfo = tryGetModel(entity.getType(), model);
        targetArm = modelInfo.getPart(renderingLeftPart);

        if (targetArm != null)
        {
            layer = layer == null ? RenderLayer.getSolid() : layer;

            model.setAngles(entity, 0, 0, 0, 0, 0);
            model.handSwingProgress = 0;

            var scale = modelInfo.scale;
            matrices.scale(scale.getX(), scale.getY(), scale.getZ());

            var offset = modelInfo.offset;
            matrices.translate(offset.getX(), offset.getY(), offset.getZ());

            targetArm.pitch = 0;
            targetArm.render(matrices, vertexConsumers.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);

            return true;
        }

        return false;
    }
}

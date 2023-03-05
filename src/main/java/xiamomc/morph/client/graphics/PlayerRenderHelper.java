package xiamomc.morph.client.graphics;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.*;
import xiamomc.morph.client.entities.MorphLocalPlayer;
import xiamomc.morph.client.mixin.accessors.LivingRendererAccessor;
import xiamomc.pluginbase.Annotations.Resolved;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class PlayerRenderHelper extends MorphClientObject
{
    public PlayerRenderHelper()
    {
        DisguiseSyncer.currentEntity.onValueChanged((o, n) ->
        {
            this.entity = n;

            allowRender = true;
        }, true);
    }

    @Resolved
    private ClientMorphManager morphManager;

    @Resolved
    private DisguiseSyncer syncer;

    public boolean shouldHideLabel(AbstractClientPlayerEntity player)
    {
        return (player instanceof MorphLocalPlayer);
    }

    private void onRenderException(Exception exception)
    {
        allowRender = false;
        exception.printStackTrace();

        if (entity != null)
        {
            try
            {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
            catch (Exception ee)
            {
                LoggerFactory.getLogger("MorphClient").error("无法移除实体：" + ee.getMessage());
                ee.printStackTrace();
            }

            entity = null;
        }

        var clientPlayer = MinecraftClient.getInstance().player;
        assert clientPlayer != null;

        MorphClient.getInstance().updateClientView(true, false);
        morphManager.selfViewIdentifier.set(null);

        clientPlayer.sendMessage(Text.literal("渲染当前实体时出现错误。"));
        clientPlayer.sendMessage(Text.literal("在当前伪装变更前客户端预览将被禁用以避免游戏崩溃。"));
    }

    private Entity entity = null;

    public boolean onDrawCall(LivingEntity player, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light)
    {
        if (!allowRender || syncer == null) return false;

        try
        {
            if (entity == null || player != MinecraftClient.getInstance().player || !MorphClient.getInstance().getModConfigData().clientViewVisible())
                return false;

            var disguiseRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);

            syncer.onGameRender();

            //LoggerFactory.getLogger("d").info(player.getName() + " :: " + player.getDataTracker().get(MorphLocalPlayer.getPMPMask()));
            disguiseRenderer.render(entity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);

            if (entity instanceof EnderDragonEntity)
                renderCrystalBeam(null, tickDelta, matrixStack, vertexConsumerProvider, light);
        }
        catch (Exception e)
        {
            onRenderException(e);
            return false;
        }

        return true;
    }

    public void renderCrystalBeam(@Nullable Camera camera, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i)
    {
        var connectedCrystal = syncer.getBeamTarget();

        if (connectedCrystal != null)
        {
            if (camera != null && camera.isThirdPerson()) return;

            //光柱目标的Y轴位移，数值越大最终的位置相较于相机越低
            //1.8(玩家高度) + 1(位移)
            var targetOffset = camera != null ? 1.8f + 1f : 0f;

            //若启用了客户端预览，则确保光柱最终位置在为未启用时的位置附近
            if (entity != null && camera != null && MorphClient.getInstance().getModConfigData().clientViewVisible())
            {
                var diff = entity.getStandingEyeHeight() - 1.8f;
                targetOffset += diff;
            }

            targetOffset = -targetOffset;

            var targetEntity = MinecraftClient.getInstance().player;
            assert targetEntity != null;

            var x = camera != null ? camera.getPos().x : targetEntity.getX();
            var y = camera != null ? camera.getPos().y + targetOffset : targetEntity.getY();
            var z = camera != null ? camera.getPos().z : targetEntity.getZ();

            var prevX = camera != null ? x : targetEntity.prevX;
            var prevY = camera != null ? y : targetEntity.prevY;
            var prevZ = camera != null ? z : targetEntity.prevZ;

            //相对位置，光柱从这里出发
            var relativeX = (float)(connectedCrystal.getX() - MathHelper.lerp(tickDelta, prevX, x));
            var relativeY = (float)(connectedCrystal.getY() - MathHelper.lerp(tickDelta, prevY, y));
            var relativeZ = (float)(connectedCrystal.getZ() - MathHelper.lerp(tickDelta, prevZ, z));

            //对matrixStack进行位移，使光柱终点停留在相机Y轴下方
            matrixStack.translate(0, targetOffset, 0);

            //渲染光柱
            EnderDragonEntityRenderer.renderCrystalBeam(relativeX,
                    relativeY + getCrystalYOffsetCopy(connectedCrystal, tickDelta),
                    relativeZ,
                    tickDelta, targetEntity.age, matrixStack, vertexConsumerProvider, i);

            //撤销位移
            matrixStack.translate(-0, -targetOffset, 0);
        }
    }

    private float getCrystalYOffsetCopy(Entity entity, float tickDelta)
    {
        var age = entity instanceof EndCrystalEntity endCrystalEntity ? endCrystalEntity.endCrystalAge : 0;

        float f = age + tickDelta;
        float g = MathHelper.sin(f * 0.2f) / 2.0f + 0.5f;
        g = (g * g + g) * 0.4f;
        return g - 1.4f;
    }

    private boolean allowRender = true;

    public boolean renderingLeftPart;

    private final Map<EntityType<?>, ModelInfo> typeModelPartMap = new Object2ObjectOpenHashMap<>();

    private record ModelInfo(@Nullable ModelPart left, @Nullable ModelPart right, Vec3d offset, Vec3d scale)
    {
        @Nullable
        public ModelPart getPart(boolean isLeftArm)
        {
            return isLeftArm ? left : right;
        }
    }

    public ModelInfo tryGetModel(EntityType<?> type, EntityModel<?> sourceModel)
    {
        if (sourceModel == null) return new ModelInfo(null, null, Vec3dUtils.of(0), Vec3dUtils.of(1));

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
        Vec3d offset = Vec3dUtils.of(0);
        Vec3d scale = Vec3dUtils.ONE();

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

                var meta = ModelWorkarounds.getInstance().apply(type, leftPart, rightPart);

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
        if (!allowRender) return false;

        try
        {
            if (entity == null || player != MinecraftClient.getInstance().player || !MorphClient.getInstance().getModConfigData().clientViewVisible()) return false;

            EntityRenderer<?> disguiseRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);

            ModelPart targetArm;
            ModelInfo modelInfo;
            RenderLayer layer = null;
            EntityModel model = null;

/*
            if (disguiseRenderer instanceof EnderDragonEntityRenderer enderDragonEntityRenderer)
            {
                model = ((DragonEntityRendererAccessor) enderDragonEntityRenderer).getModel();
                layer = dragonLayer;
            }
*/

            if (disguiseRenderer instanceof LivingEntityRenderer livingEntityRenderer)
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
                matrices.scale((float)scale.getX(), (float)scale.getY(), (float)scale.getZ());

                var offset = modelInfo.offset;
                matrices.translate(offset.getX(), offset.getY(), offset.getZ());

                targetArm.pitch = 0;
                targetArm.render(matrices, vertexConsumers.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);

                return true;
            }
        }
        catch (Exception e)
        {
            onRenderException(e);
        }

        return false;
    }
}

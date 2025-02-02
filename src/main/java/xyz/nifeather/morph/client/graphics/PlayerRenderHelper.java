package xyz.nifeather.morph.client.graphics;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.EntityModels;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.client.*;
import xyz.nifeather.morph.client.entities.MorphLocalPlayer;
import xyz.nifeather.morph.client.mixin.accessors.DragonEntityRendererAccessor;
import xyz.nifeather.morph.client.mixin.accessors.LivingRendererAccessor;
import xyz.nifeather.morph.client.syncers.ClientDisguiseSyncer;
import xyz.nifeather.morph.client.syncers.DisguiseSyncer;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Exceptions.NullDependencyException;

import java.util.List;
import java.util.Map;

public class PlayerRenderHelper extends MorphClientObject
{
    private static PlayerRenderHelper instance;

    public static PlayerRenderHelper instance()
    {
        if (instance == null) instance = new PlayerRenderHelper();

        return instance;
    }

    public PlayerRenderHelper()
    {
    }

    @Initializer
    private void load(ClientMorphManager morphManager)
    {
        morphManager.currentIdentifier.onValueChanged((o, n) ->
        {
            this.allowRender = true;
        });
    }

    @Resolved
    private DisguiseInstanceTracker instanceTracker;

    public boolean shouldHideLabel(@Nullable AbstractClientPlayerEntity player)
    {
        if (player == null) return false;

        var localSyncer = ClientDisguiseSyncer.getCurrentInstance();
        return localSyncer != null && player == localSyncer.getDisguiseInstance();
    }

    private void onRenderException(Exception exception)
    {
        allowRender = false;
        exception.printStackTrace();

        var syncer = ClientDisguiseSyncer.getCurrentInstance();
        if (syncer == null)
            throw new NullDependencyException("Render Exception with null Syncer ?!");

        var entity = syncer.getDisguiseInstance();

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
        }

        var clientPlayer = MinecraftClient.getInstance().player;
        assert clientPlayer != null;

        clientPlayer.sendMessage(Text.translatable("text.morphclient.error.render_disguise1"), false);
        clientPlayer.sendMessage(Text.translatable("text.morphclient.error.render_disguise2"), false);
    }

    @ApiStatus.Internal
    public boolean skipRender = false;

    private Camera camera()
    {
        return MinecraftClient.getInstance().gameRenderer.getCamera();
    }

    /**
     * 在玩家位置渲染通向 {@link ClientDisguiseSyncer#getBeamTarget()} 的光柱
     * @param tickCounter tickCounter
     * @param matrixStack {@link MatrixStack}
     * @param vertexConsumerProvider {@link VertexConsumerProvider}
     * @param light 光照等级
     */
    public void renderCrystalBeam(RenderTickCounter tickCounter, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light)
    {
        DisguiseSyncer abstractSyncer = instanceTracker.getSyncerFor(MinecraftClient.getInstance().player);

        if (!(abstractSyncer instanceof ClientDisguiseSyncer syncer)) return;

        var connectedCrystal = syncer.getBeamTarget();

        if (connectedCrystal == null) return;

        matrixStack.push();

        var cameraPos = camera().getPos();

        //相机XYZ
        var cameraX = cameraPos.x;
        var cameraY = cameraPos.y;
        var cameraZ = cameraPos.z;

        var player = MinecraftClient.getInstance().player;
        assert player != null;

        var tickDelta = tickCounter.getTickDelta(true);
        //通过插值的方式获取玩家XYZ可以避免让渲染出来的光柱看起来非常卡顿
        var lerpPlayerX = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        var lerpPlayerY = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        var lerpPlayerZ = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        //光柱目标的Y轴位移，数值越大最终的位置相较于相机越低
        var yOffset = 1f;

        //相对位置，光柱在这里结束
        var relativeX = (float)(connectedCrystal.getX() - lerpPlayerX);
        var relativeY = (float)(connectedCrystal.getY() - lerpPlayerY) + yOffset;
        var relativeZ = (float)(connectedCrystal.getZ() - lerpPlayerZ);

        //对matrixStack进行位移，将其中心设定在玩家处
        //光柱在这里开始
        //玩家位置 - 相机位置 = 目标位移
        matrixStack.translate(lerpPlayerX - cameraX, lerpPlayerY - cameraY - yOffset, lerpPlayerZ - cameraZ);

        //渲染光柱
        EnderDragonEntityRenderer.renderCrystalBeam(relativeX,
                relativeY + getCrystalYOffsetCopy(connectedCrystal, tickDelta),
                relativeZ,
                player.age + tickDelta, matrixStack, vertexConsumerProvider, light);

        matrixStack.pop();
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

    public record ModelInfo(@Nullable ModelPart left, @Nullable ModelPart right, Vec3d offset, Vec3d scale)
    {
        @Nullable
        public ModelPart getPart(boolean isLeftArm)
        {
            return isLeftArm ? left : right;
        }
    }

    public ModelInfo tryGetModel(EntityType<?> type, @Nullable EntityModel<?> sourceModel)
    {
        if (sourceModel == null) return new ModelInfo(null, null, Vec3dUtils.of(0), Vec3dUtils.of(1));

        var map = typeModelPartMap.getOrDefault(type, null);

        if (map != null)
            return map;

        ModelPart model = null;

        //尝试获取对应的模型
        //有些模型变换会影响全局渲染，所以我们需要创建一个新的模型（比方说雪傀儡和铁傀儡的手臂模型）
        var targetEntry = EntityModels.getModels().entrySet().stream()
                .filter(e -> e.getKey().id().equals(EntityType.getId(type))).findFirst().orElse(null);

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

    private final RenderLayer dragonLayer = RenderLayer.getEntityCutoutNoCull(Identifier.of("textures/entity/enderdragon/dragon.png"));

    /**
     * @return Whether rendered disguise instance
     */
    @SuppressWarnings("rawtypes")
    public boolean onArmDrawCall(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light)
    {
        //logger.info("On Arms!");
        if (!allowRender) return false;

        try
        {
            var syncer = ClientDisguiseSyncer.getCurrentInstance();

            if (syncer == null || syncer.disposed()) return false;
            var disguiseEntity = syncer.getDisguiseInstance();

            if (disguiseEntity == null) return false;

            EntityRenderer<?, ?> disguiseRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(disguiseEntity);

            ModelPart targetArm;
            ModelInfo modelInfo;
            RenderLayer layer = null;
            EntityModel model = null;

            if (disguiseRenderer instanceof EnderDragonEntityRenderer enderDragonEntityRenderer)
            {
                model = ((DragonEntityRendererAccessor) enderDragonEntityRenderer).getModel();
                layer = dragonLayer;
            }

            if (disguiseRenderer instanceof LivingEntityRenderer livingEntityRenderer)
            {
                model = livingEntityRenderer.getModel();

                if (disguiseEntity instanceof MorphLocalPlayer localPlayer)
                {
                    var renderer = (PlayerEntityRenderer) livingEntityRenderer;

                    if (renderingLeftPart)
                        renderer.renderLeftArm(matrices, vertexConsumers, light, localPlayer.getSkinTextures().texture(), true);
                    else
                        renderer.renderRightArm(matrices, vertexConsumers, light, localPlayer.getSkinTextures().texture(), true);

                    return true;
                }

                var renderState = (LivingEntityRenderState) livingEntityRenderer.createRenderState();
                livingEntityRenderer.updateRenderState(disguiseEntity, renderState, 0);
                layer = ((LivingRendererAccessor) livingEntityRenderer).callGetRenderLayer(renderState, true, false, true);
            }

            if (model != null)
                model.resetTransforms();

            modelInfo = tryGetModel(disguiseEntity.getType(), model);
            targetArm = modelInfo.getPart(renderingLeftPart);

            if (targetArm != null)
            {
                layer = layer == null ? RenderLayer.getSolid() : layer;

                targetArm.visible = true;
                //targetArm.resetTransform();

                var scale = modelInfo.scale;
                matrices.scale((float)scale.getX(), (float)scale.getY(), (float)scale.getZ());

                var offset = modelInfo.offset;
                matrices.translate(offset.getX(), offset.getY(), offset.getZ());

                light = (disguiseEntity.getType() == EntityType.ALLAY || disguiseEntity.getType() == EntityType.VEX)
                        ? LightmapTextureManager.MAX_LIGHT_COORDINATE
                        : light;

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

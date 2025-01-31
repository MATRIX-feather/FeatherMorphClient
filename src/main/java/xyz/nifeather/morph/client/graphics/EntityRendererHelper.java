package xyz.nifeather.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.DisguiseInstanceTracker;
import xyz.nifeather.morph.client.FeatherMorphClient;
import xyz.nifeather.morph.client.entities.IMorphClientEntity;
import xyz.nifeather.morph.client.entities.MorphLocalPlayer;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;

import java.util.Map;

public class EntityRendererHelper
{
    public EntityRendererHelper()
    {
        instance = this;
    }

    public static EntityRendererHelper instance;

    public static boolean doRenderRealName = false;

    private final int textColor = MaterialColors.Orange500.getColor();
    public final int textColorTransparent = ColorUtils.forOpacity(MaterialColors.Orange500, 0).getColor();

    @Nullable
    public final Map.Entry<Integer, String> getEntry(Integer id)
    {
        return DisguiseInstanceTracker.getInstance().playerMap.entrySet().stream()
                .filter(set -> id.equals(set.getKey()))
                .findFirst().orElse(null);
    }

    public void cacheReveal()
    {
    }

    public final void renderRevealNameIfPossible(EntityRenderDispatcher dispatcher,
                                           Entity renderingEntity, TextRenderer textRenderer,
                                           MatrixStack matrices, VertexConsumerProvider vertexConsumers)
    {
        if (!doRenderRealName) return;

        // 服务器发送来的揭示数据是 玩家ID <-> 玩家名 的格式
        // 因此当客户端玩家有伪装时，渲染其本体也会显示揭示标签
        // 但我们不想这样，所以跳过此实体的渲染
        if (renderingEntity == MinecraftClient.getInstance().player)
            return;

        int id = renderingEntity.getId();
        Entity masterEntity = null;

        // client renderer
        if (renderingEntity instanceof IMorphClientEntity iMorphEntity && iMorphEntity.featherMorph$isDisguiseEntity())
        {
            var syncer = DisguiseInstanceTracker.getInstance().getSyncerFor(iMorphEntity.featherMorph$getMasterEntityId());
            if (syncer != null)
            {
                masterEntity = syncer.getBindingPlayer();
                id = syncer.getBindingPlayer().getId();
            }
        }

        var entrySet = getEntry(id);
        if (entrySet == null) return;

        String revealName = entrySet.getValue();
        var disguiseEntityName = renderingEntity.getName().getString();

        String text = "%s(%s)".formatted(disguiseEntityName, revealName);

        renderLabelOnTop(matrices, vertexConsumers, textRenderer, renderingEntity, dispatcher, text, masterEntity);
    }

    public void renderLabelOnTop(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                 TextRenderer textRenderer,
                                 Entity entity, EntityRenderDispatcher dispatcher,
                                 String textToRender,
                                 @Nullable Entity masterEntity)
    {
        matrices.push();

        var nametagOffset = (entity.hasCustomName() || entity instanceof PlayerEntity) ? 0.25f : 0;
        if (entity instanceof MorphLocalPlayer morphLocalPlayer
            && morphLocalPlayer.getBindingPlayer() == MinecraftClient.getInstance().player)
        {
            nametagOffset = morphLocalPlayer.shouldRenderName() ? 0.25f : 0;
        }

        Vec3d labelRelativePosition = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, 0);

        if (labelRelativePosition == null)
            labelRelativePosition = new Vec3d(0, entity.getHeight(), 0);

        matrices.translate(labelRelativePosition.x, labelRelativePosition.y + 0.5f + nametagOffset, labelRelativePosition.z);

        matrices.multiply(dispatcher.getRotation());
        matrices.scale(0.025F, -0.025F, 0.025F);

        if (FeatherMorphClient.getInstance().getModConfigData().scaleNameTag)
        {
            var entityToLookup = masterEntity != null ? masterEntity : entity;
            var labelWorldPosition = entityToLookup.getPos().add(labelRelativePosition);
            var distance = dispatcher.camera.getPos().distanceTo(labelWorldPosition);
            var scale = Math.max(1, (float)distance / 7.5f);
            matrices.scale(scale, scale, scale);
        }

        float clientBackgroundOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
        int finalColor = (int)(clientBackgroundOpacity * 255.0f) << 24;

        var positionMatrix = matrices.peek().getPositionMatrix();
        var x = textRenderer.getWidth(textToRender) / -2f;

        //背景+文字
        textRenderer.draw(textToRender, x, 0,
                textColorTransparent, false,
                positionMatrix, vertexConsumers,
                TextRenderer.TextLayerType.SEE_THROUGH, finalColor, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        //文字
        textRenderer.draw(textToRender, x, 0,
                textColor, false,
                positionMatrix, vertexConsumers,
                TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        matrices.pop();
    }
}

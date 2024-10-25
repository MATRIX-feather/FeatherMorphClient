package xyz.nifeather.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.OtherClientPlayerEntity;
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
import xyz.nifeather.morph.client.MorphClient;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;
import xyz.nifeather.morph.shared.entities.IMorphEntity;

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

    public final void renderRevealNameIfPossible(EntityRenderDispatcher dispatcher,
                                           Entity renderingEntity, TextRenderer textRenderer,
                                           MatrixStack matrices, VertexConsumerProvider vertexConsumers)
    {
        if (!doRenderRealName) return;

        int id = renderingEntity.getId();

        // client renderer
        if (renderingEntity instanceof IMorphEntity iMorphEntity && iMorphEntity.featherMorph$isDisguiseEntity())
        {
            var syncer = DisguiseInstanceTracker.getInstance().getSyncerFor(iMorphEntity.featherMorph$getMasterEntityId());
            if (syncer != null)
                id = syncer.getBindingPlayer().getId();
        }

        var entrySet = getEntry(id);
        if (entrySet == null) return;

        String revealName = entrySet.getValue();
        var disguiseEntityName = renderingEntity.getName().getString();

        String text = "%s(%s)".formatted(disguiseEntityName, revealName);

        renderLabelOnTop(matrices, vertexConsumers, textRenderer, renderingEntity, dispatcher, text);
    }

    public void renderLabelOnTop(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                 TextRenderer textRenderer,
                                 Entity entity, EntityRenderDispatcher dispatcher,
                                 String textToRender)
    {
        matrices.push();

        var nametagOffset = (entity.hasCustomName() || entity instanceof PlayerEntity) ? 0.25f : 0;

        Vec3d labelRelativePosition = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, 0);

        if (labelRelativePosition == null)
            labelRelativePosition = new Vec3d(0, entity.getHeight(), 0);

        matrices.translate(labelRelativePosition.x, labelRelativePosition.y + 0.5f + nametagOffset, labelRelativePosition.z);

        matrices.multiply(dispatcher.getRotation());
        matrices.scale(0.025F, -0.025F, 0.025F);

        if (MorphClient.getInstance().getModConfigData().scaleNameTag)
        {
            var labelWorldPosition = entity.getPos().add(labelRelativePosition);
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

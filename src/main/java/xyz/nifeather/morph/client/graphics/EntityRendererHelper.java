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
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.MorphClient;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;
import xyz.nifeather.morph.client.syncers.ClientDisguiseSyncer;

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
        return MorphClient.getInstance().morphManager.playerMap.entrySet().stream()
                .filter(set -> id.equals(set.getKey()))
                .findFirst().orElse(null);
    }

    public final void renderRevealNameIfPossible(EntityRenderDispatcher dispatcher,
                                           Entity renderingEntity, TextRenderer textRenderer,
                                           MatrixStack matrices, VertexConsumerProvider vertexConsumers)
    {
        if (!doRenderRealName) return;

        Integer id = renderingEntity.getId();

        var entrySet = getEntry(id);
        if (entrySet == null) return;

        String text = entrySet.getValue();
        if (text.equals(renderingEntity.getName().getString())) return;

        //var syncer = ClientDisguiseSyncer.getCurrentInstance();
        //if (syncer != null && renderingEntity != ClientDisguiseSyncer.getCurrentInstance().getDisguiseInstance())
        //    renderingEntity.ignoreCameraFrustum = true;

        var typeText = renderingEntity instanceof PlayerEntity playerEntity
                ? playerEntity.getName().getString()
                : renderingEntity.getType().getName().getString();

        text = "%s(%s)".formatted(typeText, text);

        renderLabelOnTop(matrices, vertexConsumers, textRenderer, renderingEntity, dispatcher, text);
    }

    public void renderLabelOnTop(MatrixStack matrices, VertexConsumerProvider vertexConsumers, TextRenderer textRenderer, Entity entity, EntityRenderDispatcher dispatcher, String text)
    {
        matrices.push();

        var exOffset = (entity.hasCustomName() || entity instanceof OtherClientPlayerEntity) ? 0.25f : -0.25f;

        //Recover behavior of the old entity.getNameLabelHeight() -> entity.getHeight() + 0.5f
        var point = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getYaw());
        if (point == null)
            matrices.translate(0, entity.getHeight() + 0.5f + exOffset, 0);
        else
            matrices.translate(point.x, (entity.hasCustomName() ? 0.3 : 0) + point.y + 0.5f, point.z);

        matrices.multiply(dispatcher.getRotation());
        matrices.scale(0.025F, -0.025F, 0.025F);

        if (MorphClient.getInstance().getModConfigData().scaleNameTag)
        {
            var distance = dispatcher.camera.getPos().distanceTo(entity.getPos());
            var scale = Math.max(1, (float)distance / 7.5f);
            matrices.scale(scale, scale, scale);
        }

        float clientBackgroundOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
        int finalColor = (int)(clientBackgroundOpacity * 255.0f) << 24;

        var positionMatrix = matrices.peek().getPositionMatrix();
        var x = textRenderer.getWidth(text) / -2f;

        //背景
        textRenderer.draw(text, x, 0,
                textColorTransparent, false,
                positionMatrix, vertexConsumers,
                TextRenderer.TextLayerType.SEE_THROUGH, finalColor, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        //文字
        textRenderer.draw(text, x, 0,
                textColor, false,
                positionMatrix, vertexConsumers,
                TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        matrices.pop();
    }
}

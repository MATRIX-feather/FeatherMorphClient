package xiamomc.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.color.MaterialColors;

public class EntityRendererHelper
{
    public EntityRendererHelper()
    {
        instance = this;
    }

    public static EntityRendererHelper instance;

    public void onRender(EntityRenderDispatcher dispatcher, Entity entity, TextRenderer textRenderer, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci)
    {
        var uuid = entity.getId();

        var morphManager = MorphClient.getInstance().morphManager;
        var entrySet = morphManager.playerMap.entrySet().stream()
                .filter(set -> set.getKey().equals(uuid))
                .findFirst().orElse(null);

        if (entrySet == null) return;

        String text = entrySet.getValue();
        if (text.equals(entity.getEntityName())) return;

        matrices.push();

        var exOffset = (entity.hasCustomName() || entity instanceof OtherClientPlayerEntity) ? 0.25f : -0.25f;

        matrices.translate(0, entity.getNameLabelHeight() +exOffset, 0);
        matrices.multiply(dispatcher.getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);

        float clientBackgroundOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
        int finalColor = (int)(clientBackgroundOpacity * 255.0f) << 24;

        textRenderer.draw(text, textRenderer.getWidth(text) / -2f, 0,
                MaterialColors.Yellow500.getColor(), false,
                matrices.peek().getPositionMatrix(), vertexConsumers,
                TextRenderer.TextLayerType.SEE_THROUGH, finalColor, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        matrices.pop();
    }
}

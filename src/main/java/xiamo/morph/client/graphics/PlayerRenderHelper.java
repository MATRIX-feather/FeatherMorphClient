package xiamo.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import xiamo.morph.client.EntityCache;
import xiamo.morph.client.MorphClient;

public class PlayerRenderHelper
{
    public PlayerRenderHelper()
    {
        MorphClient.currentIdentifier.onValueChanged((o, n) ->
        {
            //实体同步给MorphClient那里做了
            this.entity = EntityCache.getEntity(n);
        });
    }

    private Entity entity = null;

    public boolean onDrawCall(float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i)
    {
        if (entity == null) return false;

        var disguiseRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);

        disguiseRenderer.render(entity, f, g, matrixStack, vertexConsumerProvider, i);
        return true;
    }
}

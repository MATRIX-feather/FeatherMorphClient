package xiamo.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import xiamo.morph.client.EntityCache;
import xiamo.morph.client.MorphClient;

import static net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity;

public class InventoryRenderHelper
{
    public InventoryRenderHelper()
    {
        MorphClient.selfViewIdentifier.onValueChanged((o, n) ->
        {
            allowRender = true;

            this.entity = EntityCache.getEntity(n);
        });
    }

    public boolean allowRender = true;
    private LivingEntity entity;

    public void onRenderCall(int x, int y, int size, float mouseX, float mouseY)
    {
        if (!allowRender) return;
        var modConfig = MorphClient.getInstance().getModConfigData();

        if (entity != null
                && (modConfig.alwaysShowPreviewInInventory || modConfig.clientViewVisible()))
        {
            try
            {
                drawEntity(x, y, size, mouseX, mouseY, entity);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                allowRender = false;
            }
        }
        else
        {
            var clientPlayer = MinecraftClient.getInstance().player;

            if (clientPlayer != null)
                drawEntity(x, y, size, mouseX, mouseY, clientPlayer);
        }
    }
}

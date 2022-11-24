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
        MorphClient.currentIdentifier.onValueChanged((o, n) ->
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

        var modInstance = MorphClient.getInstance();
        var clientPlayer = MinecraftClient.getInstance().player;

        if (entity != null
                && (modInstance.getModConfigData().alwaysShowPreviewInInventory
                        || modInstance.selfVisibleToggled.get()
                        || modInstance.getModConfigData().clientViewVisible()))
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
            drawEntity(x, y, size, mouseX, mouseY, clientPlayer);
        }
    }
}

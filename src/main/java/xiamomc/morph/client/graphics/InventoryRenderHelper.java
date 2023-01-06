package xiamomc.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import xiamomc.morph.client.DisguiseSyncer;
import xiamomc.morph.client.MorphClient;

import static net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity;

public class InventoryRenderHelper
{
    private static InventoryRenderHelper instance;

    public static InventoryRenderHelper getInstance()
    {
        if (instance == null) instance = new InventoryRenderHelper();

        return instance;
    }

    public InventoryRenderHelper()
    {
        DisguiseSyncer.currentEntity.onValueChanged((o, n) ->
        {
            allowRender = true;

            this.entity = n;
        }, true);
    }

    public boolean allowRender = true;
    private LivingEntity entity;

    public void onRenderCall(int x, int y, int size, float mouseX, float mouseY)
    {
        if (!allowRender) return;
        var modConfig = MorphClient.getInstance().getModConfigData();

        if (entity != null && (modConfig.clientViewVisible() || modConfig.alwaysShowPreviewInInventory))
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

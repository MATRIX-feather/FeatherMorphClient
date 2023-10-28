package xiamomc.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import xiamomc.morph.client.syncers.ClientDisguiseSyncer;
import xiamomc.morph.client.MorphClient;

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
        ClientDisguiseSyncer.currentEntity.onValueChanged((o, n) ->
        {
            allowRender = true;

            this.entity = n;
        }, true);
    }

    public boolean allowRender = true;
    private LivingEntity entity;

    public void onRenderCall(DrawContext context, int x1, int y1, int x2, int y2, int size, float f, float mouseX, float mouseY)
    {
        if (!allowRender) return;
        var modConfig = MorphClient.getInstance().getModConfigData();

        if (entity != null && (modConfig.clientViewVisible() || modConfig.alwaysShowPreviewInInventory))
        {
            try
            {
                InventoryScreen.drawEntity(context, x1, y1, x2, y2, size, f, mouseX, mouseY, entity);
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
                InventoryScreen.drawEntity(context, x1, y1, x2, y2, size, f, mouseX, mouseY, clientPlayer);
        }
    }
}

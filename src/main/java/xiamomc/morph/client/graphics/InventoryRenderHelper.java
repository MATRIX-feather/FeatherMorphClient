package xiamomc.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.MorphClientObject;
import xiamomc.morph.client.syncers.ClientDisguiseSyncer;
import xiamomc.morph.client.MorphClient;
import xiamomc.pluginbase.Annotations.Initializer;

public class InventoryRenderHelper extends MorphClientObject
{
    private static InventoryRenderHelper instance;

    public static InventoryRenderHelper getInstance()
    {
        if (instance == null) instance = new InventoryRenderHelper();

        return instance;
    }

    @Initializer
    private void load(ClientMorphManager morphManager)
    {
        morphManager.currentIdentifier.onValueChanged((o, n) ->
        {
            this.allowRender = true;
        });
    }

    public boolean allowRender = true;

    public void onRenderCall(DrawContext context, int x1, int y1, int x2, int y2, int size, float f, float mouseX, float mouseY)
    {
        if (!allowRender) return;
        var modConfig = MorphClient.getInstance().getModConfigData();

        var syncer = ClientDisguiseSyncer.getCurrentInstance();
        var syncerNotAvailable = syncer == null || syncer.disposed();
        var entity = syncerNotAvailable ? null : syncer.getDisguiseInstance();

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

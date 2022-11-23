package xiamo.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.EntityCache;
import xiamo.morph.client.MorphClient;

import java.nio.charset.MalformedInputException;

import static net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity;

public class InventoryRenderHelper
{
    public boolean init;
    public void doInit()
    {
        init = true;

        allowRender = true;
        allowTick = true;

        MorphClient.currentIdentifier.onValueChanged((o, n) ->
        {
            this.onCurrentChanged(n);
        }, true);
    }

    private void onCurrentChanged(String newIdentifier)
    {
        var clientWorld = MinecraftClient.getInstance().world;
        assert clientWorld != null;

        if (entity != null)
        {
            entity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

            entity.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);

            clientWorld.removeEntity(entity.getId(), Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }

        this.entity = EntityCache.getEntity(newIdentifier);

        allowRender = true;
        allowTick = true;

        if (entity != null)
        {
            clientWorld.addEntity(entity.getId(), entity);
        }
    }

    public LivingEntity entity;
    public boolean allowRender;

    private boolean allowTick;

    public void onScreenTick()
    {
        if (!allowTick) return;

        try
        {
            var clientPlayer = MinecraftClient.getInstance().player;
            assert clientPlayer != null;

            if (entity != null)
            {
                var playerPos = clientPlayer.getPos();
                entity.setPosition(playerPos.x, -32767, playerPos.z + 5);

                entity.tick();

                entity.equipStack(EquipmentSlot.MAINHAND, clientPlayer.getEquippedStack(EquipmentSlot.MAINHAND));
                entity.equipStack(EquipmentSlot.OFFHAND, clientPlayer.getEquippedStack(EquipmentSlot.OFFHAND));

                entity.equipStack(EquipmentSlot.HEAD, clientPlayer.getEquippedStack(EquipmentSlot.HEAD));
                entity.equipStack(EquipmentSlot.CHEST, clientPlayer.getEquippedStack(EquipmentSlot.CHEST));
                entity.equipStack(EquipmentSlot.LEGS, clientPlayer.getEquippedStack(EquipmentSlot.LEGS));
                entity.equipStack(EquipmentSlot.FEET, clientPlayer.getEquippedStack(EquipmentSlot.FEET));

                entity.setPose(clientPlayer.getPose());
                entity.setSwimming(clientPlayer.isSwimming());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            allowTick = false;
        }

    }

    public void onRenderCall(int x, int y, int size, float mouseX, float mouseY)
    {
        if (!init) doInit();

        if (!allowRender) return;

        var clientPlayer = MinecraftClient.getInstance().player;

        if (entity != null)
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

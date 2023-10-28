package xiamomc.morph.client.syncers;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.client.EntityCache;

public class OtherClientDisguiseSyncer extends DisguiseSyncer
{
    public OtherClientDisguiseSyncer(AbstractClientPlayerEntity clientPlayer, String morphId, int networkId)
    {
        super(clientPlayer, morphId, networkId);
    }

    @Override
    protected void syncPosition()
    {
        if (disguiseInstance == null) return;

        var playerPos = bindingPlayer.getPos();

        /*
        if (prevPlayerPos == null) prevPlayerPos = Vec3d.ZERO;
        if (renderPos == null) renderPos = Vec3d.ZERO;

        var xDiff = playerPos.x - prevPlayerPos.x;
        var yDiff = playerPos.y - prevPlayerPos.y;
        var zDiff = playerPos.z - prevPlayerPos.z;

        var diff = new Vec3d(xDiff, yDiff, zDiff);
        renderPos = renderPos.add(diff);
        */

        //暂时先这样
        disguiseInstance.setPosition(playerPos);
    }

    private Vec3d prevPlayerPos;

    private Vec3d renderPos;

    @Override
    protected void onDispose()
    {
        if (disguiseInstance != null)
            bindingPlayer.setPosition(disguiseInstance.getPos());
    }

    private EntityCache localCache;

    @Override
    protected @NotNull EntityCache getEntityCache()
    {
        if (localCache == null) localCache = new EntityCache();

        return localCache;
    }

    @Override
    public void syncTick()
    {
        if (disguiseInstance == null || disposed()) return;

        disguiseInstance.equipStack(EquipmentSlot.MAINHAND, bindingPlayer.getEquippedStack(EquipmentSlot.MAINHAND));
        disguiseInstance.equipStack(EquipmentSlot.OFFHAND, bindingPlayer.getEquippedStack(EquipmentSlot.OFFHAND));

        disguiseInstance.equipStack(EquipmentSlot.HEAD, bindingPlayer.getEquippedStack(EquipmentSlot.HEAD));
        disguiseInstance.equipStack(EquipmentSlot.CHEST, bindingPlayer.getEquippedStack(EquipmentSlot.CHEST));
        disguiseInstance.equipStack(EquipmentSlot.LEGS, bindingPlayer.getEquippedStack(EquipmentSlot.LEGS));
        disguiseInstance.equipStack(EquipmentSlot.FEET, bindingPlayer.getEquippedStack(EquipmentSlot.FEET));

        baseSync();
        syncPosition();
        syncYawPitch();
    }

    @Override
    public void syncDraw()
    {
    }

    @Override
    protected void initialSync()
    {
    }
}

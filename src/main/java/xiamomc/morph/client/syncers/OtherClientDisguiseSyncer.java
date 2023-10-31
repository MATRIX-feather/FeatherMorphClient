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

        //暂时先这样
        disguiseInstance.setPosition(playerPos);
    }

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

        baseSync();
        syncPosition();
        syncYawPitch();
        disguiseInstance.setGlowing(bindingPlayer.isGlowing());
    }

    @Override
    public void syncDraw()
    {
        syncYawPitch();
    }

    @Override
    protected void initialSync()
    {
    }
}

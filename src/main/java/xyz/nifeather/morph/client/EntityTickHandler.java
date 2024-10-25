package xyz.nifeather.morph.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.syncers.DisguiseSyncer;

@Environment(EnvType.CLIENT)
public class EntityTickHandler
{
    /**
     *
     */
    public static void cancelIfIsDisguiseAndNotSyncing(CallbackInfo ci, Object entity)
    {
        var syncers = DisguiseInstanceTracker.getInstance().getAllSyncer();

        // 遍历所有Syncer
        for (DisguiseSyncer syncer : syncers)
        {
            // 如果存在Syncer，其伪装实例是此实体，并且此Syncer尚未处于同步流程
            if (syncer.getDisguiseInstance() == entity && !syncer.isSyncing())
            {
                // 取消此次调用
                ci.cancel();
                break;
            }
        }
    }
}

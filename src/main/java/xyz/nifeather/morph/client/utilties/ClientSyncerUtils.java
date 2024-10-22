package xyz.nifeather.morph.client.utilties;

import net.minecraft.entity.Entity;
import xyz.nifeather.morph.client.syncers.ClientDisguiseSyncer;
import xyz.nifeather.morph.client.syncers.DisguiseSyncer;

import java.util.function.Consumer;

public class ClientSyncerUtils
{
    public static void runIfSyncerEntityValid(Consumer<Entity> consumer)
    {
        runIfSyncerValid(syncer ->
        {
            var entity = syncer.getDisguiseInstance();
            if (entity != null && !entity.isRemoved())
                consumer.accept(entity);
        });
    }

    public static void runIfSyncerValid(Consumer<DisguiseSyncer> consumer)
    {
        var syncer = ClientDisguiseSyncer.getCurrentInstance();
        if (syncer == null || syncer.disposed()) return;

        consumer.accept(syncer);
    }
}

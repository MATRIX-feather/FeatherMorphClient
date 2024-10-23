package xyz.nifeather.morph.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.syncers.DisguiseSyncer;
import xiamomc.morph.network.commands.S2C.clientrender.*;
import xiamomc.pluginbase.Annotations.Resolved;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisguiseInstanceTracker extends MorphClientObject
{
    //region

    public static DisguiseInstanceTracker getInstance()
    {
        return instance;
    }

    private static DisguiseInstanceTracker instance;

    public DisguiseInstanceTracker()
    {
        instance = this;
    }

    //endregion

    @Resolved
    private ClientMorphManager manager;

    //region CommandHandling

    private final Map<Integer, String> trackingDisguises = new Object2ObjectArrayMap<>();

    public Map<Integer, String> getTrackingDisguises()
    {
        return new Object2ObjectArrayMap<>(trackingDisguises);
    }

    public void onSyncCommand(S2CRenderMapSyncCommand s2CRenderMapSyncCommand)
    {
        this.reset();

        var map = s2CRenderMapSyncCommand.getMap();
        trackingDisguises.putAll(map);
        map.forEach(this::addSyncerIfNotExist);
    }

    public void onAddCommand(S2CRenderMapAddCommand s2CRenderMapAddCommand)
    {
        if (!s2CRenderMapAddCommand.isValid()) return;

        var networkId = s2CRenderMapAddCommand.getPlayerNetworkId();
        trackingDisguises.put(networkId, s2CRenderMapAddCommand.getMobId());

        var prevSyncer = getSyncerFor(networkId);
        if (prevSyncer != null)
            this.removeSyncer(prevSyncer);

        addSyncerIfNotExist(networkId, s2CRenderMapAddCommand.getMobId());
    }

    public void onRemoveCommand(S2CRenderMapRemoveCommand s2CRenderMapRemoveCommand)
    {
        if (!s2CRenderMapRemoveCommand.isValid()) return;

        var id = s2CRenderMapRemoveCommand.getPlayerNetworkId();
        trackingDisguises.remove(id);

        var syncer = idSyncerMap.getOrDefault(id, null);
        if (syncer != null)
            this.removeSyncer(syncer);
    }

    public void onClearCommand(S2CRenderMapClearCommand s2CRenderMapClearCommand)
    {
        this.reset();
    }

    public void onMetaCommand(S2CRenderMapMetaCommand metaCommand)
    {
        var meta = metaCommand.getArgumentAt(0);

        if (meta == null)
        {
            logger.warn("Received S2CRenderMapMetaCommand with no meta! Not Processing...");
            logger.warn("Packet: " + metaCommand.buildCommand());
            return;
        }

        var networkId = meta.networkId;
        if (networkId == -1)
        {
            logger.warn("Received S2CRenderMapMetaCommand with -1 network id! Not Processing...");
            return;
        }

        var registry = MinecraftClient.getInstance().player.getWorld().getRegistryManager();
        var newMeta = ConvertedMeta.of(meta, registry);
        var currentMeta = getMetaFor(networkId);

        if (newMeta != null)
            currentMeta.mergeFrom(newMeta);

        currentMeta.outdated = true;
        idMetaMap.put(networkId, currentMeta);
    }

    //endregion

    public void reset()
    {
        trackingDisguises.clear();
        this.playerMap.clear();

        var mapCopy = new Object2ObjectArrayMap<>(idSyncerMap);
        mapCopy.forEach((id, syncer) -> this.removeSyncer(syncer));

        idMetaMap.clear();
    }

    //region Meta Tracking

    private final Map<Integer, ConvertedMeta> idMetaMap = new HashMap<>();

    public ConvertedMeta getMetaFor(Entity entity)
    {
        return getMetaFor(entity.getId());
    }

    @NotNull
    public ConvertedMeta getMetaFor(int networkId)
    {
        var meta = idMetaMap.getOrDefault(networkId, null);

        return meta == null ? new ConvertedMeta() : meta;
    }

    //endregion

    //region Syncer Tracking

    private final Map<Integer, DisguiseSyncer> idSyncerMap = new Object2ObjectArrayMap<>();

    public List<DisguiseSyncer> getAllSyncer()
    {
        return new ObjectArrayList<>(idSyncerMap.values());
    }

    public void removeSyncer(DisguiseSyncer targetSyncer)
    {
        targetSyncer.dispose();
        var optional = idSyncerMap.entrySet().stream()
                .filter(e -> e.getValue().equals(targetSyncer))
                .findFirst();

        if (optional.isPresent())
        {
            idSyncerMap.remove(optional.get().getKey());
        }
        else
        {
            logger.warn("Trying to remove an DisguiseSyncer that is not in the list?!");
            Thread.dumpStack();
        }
    }

    @Nullable
    public DisguiseSyncer getSyncerFor(Entity entity)
    {
        return idSyncerMap.getOrDefault(entity.getId(), null);
    }

    @Nullable
    public DisguiseSyncer getSyncerFor(int networkId)
    {
        return idSyncerMap.getOrDefault(networkId, null);
    }

    //endregion

    public final Map<Integer, String> playerMap = new Object2ObjectArrayMap<>();

    @Nullable
    public DisguiseSyncer addSyncerIfNotExist(int networkId, String did)
    {
        if (idSyncerMap.containsKey(networkId))
        {
            var syncer = idSyncerMap.get(networkId);

            // 如果Syncer已调用了dispose方法，则当作不存在处理
            if (!syncer.disposed())
                return syncer;
        };

        var world = MinecraftClient.getInstance().world;
        var entity = world.getEntityById(networkId);

        if (!(entity instanceof AbstractClientPlayerEntity player)) return null;

        var syncer = manager.createSyncerFor(player, did, networkId);
        idSyncerMap.put(networkId, syncer);

        return syncer;
    }

    @Nullable
    public DisguiseSyncer setSyncer(Entity entity, String did)
    {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return null;

        var networkId = entity.getId();

        // 移除之前的Syncer
        var prevSyncer = idSyncerMap.getOrDefault(networkId, null);

        if (prevSyncer != null)
            this.removeSyncer(prevSyncer);

        // 然后再添加新的
        var syncer = manager.createSyncerFor(player, did, player.getId());
        idSyncerMap.put(networkId, syncer);

        return syncer;
    }

    @Nullable
    public DisguiseSyncer addSyncerIfNotExist(Entity entity)
    {
        var networkId = entity.getId();
        if (idSyncerMap.containsKey(networkId)) return idSyncerMap.get(networkId);

        var tracking = trackingDisguises.getOrDefault(networkId, "no");

        if (tracking.equals("no")) return null;
        if (!(entity instanceof AbstractClientPlayerEntity player)) return null;

        var syncer = manager.createSyncerFor(player, tracking, player.getId());
        idSyncerMap.put(networkId, syncer);

        return syncer;
    }
}

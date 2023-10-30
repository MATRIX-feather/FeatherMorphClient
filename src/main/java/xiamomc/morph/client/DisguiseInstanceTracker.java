package xiamomc.morph.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.syncers.DisguiseSyncer;
import xiamomc.morph.network.commands.S2C.clientrender.*;
import xiamomc.pluginbase.Annotations.Initializer;
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

    @Initializer
    private void load()
    {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            this.clearTracking();
        });
    }

    private void clearTracking()
    {
        idSyncerMap.forEach((id, syncer) -> syncer.dispose());
        this.idSyncerMap.clear();
        this.trackingDisguises.clear();
    }

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

        var prevSyncer = getSyncerFor(networkId);
        if (prevSyncer != null)
            removeSyncer(prevSyncer);

        trackingDisguises.put(networkId, s2CRenderMapAddCommand.getMobId());
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
            logger.warn("Received S2CRenderMapMetaCommand with no meta?");
            logger.warn("Packet: " + metaCommand.buildCommand());
            return;
        }

        var networkId = meta.networkId;
        if (networkId == -1)
        {
            logger.warn("Received S2CRenderMapMetaCommand with -1 network id?");
            return;
        }

        var convertedMeta = ConvertedMeta.of(meta);
        var currentMeta = getMetaFor(networkId);

        if (convertedMeta != null)
            currentMeta.mergeFrom(convertedMeta);

        currentMeta.outdated = true;
        idMetaMap.put(networkId, currentMeta);
    }

    private void reset()
    {
        trackingDisguises.clear();
        idSyncerMap.forEach((id, syncer) -> syncer.dispose());
        idSyncerMap.clear();
        idMetaMap.clear();
    }

    //endregion

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

    private Map<Integer, DisguiseSyncer> idSyncerMap = new Object2ObjectArrayMap<>();

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

        logger.info("Remove syncer call: " + targetSyncer + " :: get " + optional);
        optional.ifPresent(e -> idSyncerMap.remove(e.getKey()));
    }

    @Nullable
    public DisguiseSyncer getSyncerFor(AbstractClientPlayerEntity player)
    {
        return idSyncerMap.getOrDefault(player.getId(), null);
    }

    @Nullable
    public DisguiseSyncer getSyncerFor(int networkId)
    {
        return idSyncerMap.getOrDefault(networkId, null);
    }

    //endregion

    //region Compound Handling

    public NbtCompound getNbtFor(int id)
    {
        //TODO: Implement this
        return new NbtCompound();
    }

    //endregion

    @Nullable
    public DisguiseSyncer addSyncerIfNotExist(int networkId, String did)
    {
        if (idSyncerMap.containsKey(networkId)) return idSyncerMap.get(networkId);

        var world = MinecraftClient.getInstance().world;
        var entity = world.getEntityById(networkId);

        if (!(entity instanceof AbstractClientPlayerEntity player)) return null;

        var syncer = manager.getSyncerFor(player, did, networkId);
        idSyncerMap.put(networkId, syncer);

        return syncer;
    }

/*

    @Nullable
    public DisguiseSyncer refreshSyncer(Entity entity)
    {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return null;

        var networkId = entity.getId();

        var tracking = trackingDisguises.getOrDefault(networkId, "no");
        if (tracking.equals("no")) return null;

        var prevSyncer = idSyncerMap.getOrDefault(networkId, null);

        if (prevSyncer != null)
            this.removeSyncer(prevSyncer);

        var syncer = manager.getSyncerFor(player, tracking, player.getId());
        idSyncerMap.put(networkId, syncer);

        return syncer;
    }

*/

    @Nullable
    public DisguiseSyncer addSyncerIfNotExist(Entity entity)
    {
        var networkId = entity.getId();
        if (idSyncerMap.containsKey(networkId)) return idSyncerMap.get(networkId);

        var tracking = trackingDisguises.getOrDefault(networkId, "no");

        if (tracking.equals("no")) return null;
        if (!(entity instanceof AbstractClientPlayerEntity player)) return null;

        var syncer = manager.getSyncerFor(player, tracking, player.getId());
        idSyncerMap.put(networkId, syncer);

        return syncer;
    }
}

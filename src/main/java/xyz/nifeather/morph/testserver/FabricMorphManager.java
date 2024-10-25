package xyz.nifeather.morph.testserver;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.commands.S2C.S2CCurrentCommand;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapAddCommand;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapRemoveCommand;
import xiamomc.morph.network.commands.S2C.map.S2CMapCommand;
import xiamomc.morph.network.commands.S2C.map.S2CMapRemoveCommand;
import xiamomc.morph.network.commands.S2C.map.S2CPartialMapCommand;
import xiamomc.morph.network.commands.S2C.set.S2CSetAvailableAnimationsCommand;
import xiamomc.pluginbase.Annotations.Resolved;
import xyz.nifeather.morph.client.AnimationNames;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FabricMorphManager
{
    private final List<String> availableDisguises = new ObjectArrayList<>();

    private void listEntityTypes()
    {
        var server = VirtualServer.server;
        if (server == null)
        {
            VirtualServer.LOGGER.warn("Server is NULL, not listing entity types!");
            return;
        }

        var entityTypeRegistry = server.getRegistryManager()
                .getOptional(Registries.ENTITY_TYPE.getKey())
                .orElse(null);

        if (entityTypeRegistry == null)
        {
            VirtualServer.LOGGER.warn("Entity type registry is NULL, not listing entity types!");
            return;
        }

        entityTypeRegistry.getKeys().forEach(key ->
        {
            var type = entityTypeRegistry.get(key);
            if (type == null) return;

            var world = server.getOverworld();

            var instance = type.create(world, SpawnReason.COMMAND);

            if (!(instance instanceof LivingEntity living)) return;

            this.availableDisguises.add(key.getValue().toString());
        });

        availableDisguises.addAll(List.of(
                "player:Faruzan_",
                "player:Notch",
                "player:Ganyu",
                "player:Nahida"
        ));

        VirtualServer.LOGGER.info("Done init valid entity types.");
    }

    public List<String> getUnlockedDisguises(PlayerEntity player)
    {
        if (availableDisguises.isEmpty()) listEntityTypes();

        return new ObjectArrayList<>(availableDisguises);
    }

    private final Map<PlayerEntity, DisguiseSession> disguiseSessionMap = new ConcurrentHashMap<>();

    @Nullable
    public DisguiseSession getSessionFor(PlayerEntity player)
    {
        return disguiseSessionMap.getOrDefault(player, null);
    }

    public List<DisguiseSession> listAllSession()
    {
        return new ObjectArrayList<>(disguiseSessionMap.values());
    }

    public void morph(ServerPlayerEntity player, String identifier)
    {
        disguiseSessionMap.put(player, new DisguiseSession(player, identifier));

        var clientHandler = VirtualServer.instance.clientHandler;
        clientHandler.sendCommand(player, new S2CCurrentCommand(identifier));
        clientHandler.sendCommand(player, new S2CSetAvailableAnimationsCommand(
                AnimationNames.CRAWL,
                AnimationNames.DIGDOWN,
                AnimationNames.LAY,
                AnimationNames.DANCE
        ));

        var cmd = new S2CRenderMapAddCommand(player.getId(), identifier);

        Map<Integer, String> revealMap = new Object2ObjectOpenHashMap<>();
        revealMap.put(player.getId(), player.getName().getString());

        var cmdReveal = new S2CPartialMapCommand(revealMap);
        for (ServerPlayerEntity serverPlayerEntity : VirtualServer.server.getPlayerManager().getPlayerList())
        {
            clientHandler.sendCommand(serverPlayerEntity, cmd);
            clientHandler.sendCommand(serverPlayerEntity, cmdReveal);
        }
    }

    public void unMorph(ServerPlayerEntity player)
    {
        disguiseSessionMap.remove(player);

        var clientHandler = VirtualServer.instance.clientHandler;
        clientHandler.sendCommand(player, new S2CCurrentCommand(null));

        var cmd = new S2CRenderMapRemoveCommand(player.getId());
        var cmdReveal = new S2CMapRemoveCommand(player.getId());
        for (ServerPlayerEntity serverPlayerEntity : VirtualServer.server.getPlayerManager().getPlayerList())
        {
            clientHandler.sendCommand(serverPlayerEntity, cmd);
            clientHandler.sendCommand(serverPlayerEntity, cmdReveal);
        }
    }

    public void dispose()
    {
        VirtualServer.LOGGER.info("Disposing FabricMorphManager");
        availableDisguises.clear();
    }
}

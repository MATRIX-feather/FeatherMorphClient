package xyz.nifeather.morph.server;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.shedaniel.cloth.clothconfig.shadowed.org.yaml.snakeyaml.error.YAMLException;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.commands.S2C.S2CCurrentCommand;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapAddCommand;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapRemoveCommand;
import xiamomc.morph.network.commands.S2C.map.S2CMapRemoveCommand;
import xiamomc.morph.network.commands.S2C.map.S2CPartialMapCommand;
import xiamomc.morph.network.commands.S2C.set.S2CSetAvailableAnimationsCommand;
import xyz.nifeather.morph.client.AnimationNames;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FabricMorphManager
{
    private final List<String> availableDisguises = new ObjectArrayList<>();

    private void listEntityTypes()
    {
        var server = MorphServer.server;
        if (server == null)
        {
            MorphServer.LOGGER.warn("Server is NULL, not listing entity types!");
            return;
        }

        var entityTypeRegistry = server.getRegistryManager()
                .getOptional(Registries.ENTITY_TYPE.getKey())
                .orElse(null);

        if (entityTypeRegistry == null)
        {
            MorphServer.LOGGER.warn("Entity type registry is NULL, not listing entity types!");
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

        MorphServer.LOGGER.info("Done init valid entity types.");
    }

    public List<String> getUnlockedDisguises(PlayerEntity player)
    {
        if (availableDisguises.isEmpty()) listEntityTypes();

        return new ObjectArrayList<>(availableDisguises);
    }

    private final Map<PlayerEntity, FabricDisguiseSession> disguiseSessionMap = new ConcurrentHashMap<>();

    @Nullable
    public FabricDisguiseSession getSessionFor(PlayerEntity player)
    {
        return disguiseSessionMap.getOrDefault(player, null);
    }

    public List<FabricDisguiseSession> listAllSession()
    {
        return new ObjectArrayList<>(disguiseSessionMap.values());
    }

    public void morph(ServerPlayerEntity player, String identifier)
    {
        disguiseSessionMap.put(player, new FabricDisguiseSession(player, identifier));

        if (identifier.startsWith("minecraft"))
        {
            var type = EntityType.get(identifier);

            if (type.isEmpty())
            {
                player.sendMessage(Text.literal("No such entity!"));
                return;
            }

            if (!type.get().getBaseClass().isAssignableFrom(LivingEntity.class))
            {
                player.sendMessage(Text.literal("Not a living entity!"));
                return;
            }
        }
        else if (!identifier.startsWith("player"))
        {
            player.sendMessage(Text.literal("Invalid id '%s'".formatted(identifier)));
            return;
        }

        var clientHandler = MorphServer.instance.clientHandler;
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
        for (ServerPlayerEntity serverPlayerEntity : MorphServer.server.getPlayerManager().getPlayerList())
        {
            clientHandler.sendCommand(serverPlayerEntity, cmd);
            clientHandler.sendCommand(serverPlayerEntity, cmdReveal);
        }

        player.sendMessage(Text.literal("Disguising as '%s'".formatted(identifier)));
    }

    public void unMorph(ServerPlayerEntity player)
    {
        disguiseSessionMap.remove(player);

        var clientHandler = MorphServer.instance.clientHandler;
        clientHandler.sendCommand(player, new S2CCurrentCommand(null));

        var cmd = new S2CRenderMapRemoveCommand(player.getId());
        var cmdReveal = new S2CMapRemoveCommand(player.getId());
        for (ServerPlayerEntity serverPlayerEntity : MorphServer.server.getPlayerManager().getPlayerList())
        {
            clientHandler.sendCommand(serverPlayerEntity, cmd);
            clientHandler.sendCommand(serverPlayerEntity, cmdReveal);
        }

        player.sendMessage(Text.literal("Undisguised"));
    }

    public void dispose()
    {
        MorphServer.LOGGER.info("Disposing FabricMorphManager");
        availableDisguises.clear();
    }
}

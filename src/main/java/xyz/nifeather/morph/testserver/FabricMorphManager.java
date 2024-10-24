package xyz.nifeather.morph.testserver;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;

import java.util.List;

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

    public void dispose()
    {
        VirtualServer.LOGGER.info("Disposing FabricMorphManager");
        availableDisguises.clear();
    }
}

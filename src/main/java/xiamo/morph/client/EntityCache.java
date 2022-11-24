package xiamo.morph.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.mixin.accessors.ArmorStandEntityAccessor;

import java.util.Map;

public class EntityCache
{
    private static final Map<String, LivingEntity> cacheMap = new Object2ObjectOpenHashMap<>();

    @Nullable
    public static LivingEntity getEntity(String identifier)
    {
        if (identifier == null) return null;

        var cache = cacheMap.getOrDefault(identifier, null);

        LoggerFactory.getLogger("morph").info("Cache of " + identifier + " is " + cache);

        if (cache != null && !cache.isRemoved()) return cache;

        LivingEntity living = null;

        if (identifier.startsWith("minecraft:"))
        {
            var typeOptional = EntityType.get(identifier);

            if (typeOptional.isEmpty()) return null;

            var type = typeOptional.get();

            var world = MinecraftClient.getInstance().world;
            if (world == null) return null;

            var instance = type.create(world);

            if (!(instance instanceof LivingEntity le)) return null;

            living = le;

            if (instance instanceof ArmorStandEntity armorStandEntity)
                ((ArmorStandEntityAccessor)armorStandEntity).callSetShowArms(true);

            if (instance instanceof EnderDragonEntity dragon)
                dragon.getPhaseManager().setPhase(PhaseType.HOVER);

            living.setSilent(true);
        }
/*
        else if (identifier.startsWith("player:"))
        {
            var splitedId = identifier.split(":", 2);

            if (splitedId.length != 2) return null;
            var profile = new GameProfile(UUID.randomUUID(), splitedId[1]);
            var player = new OtherClientPlayerEntity(MinecraftClient.getInstance().world, profile, null);

            LoggerFactory.getLogger("morph").info("Fetching skin for " + splitedId[1]);

            MinecraftClient.getInstance().getSkinProvider().loadSkin(profile, (type, id, texture) ->
            {
                LoggerFactory.getLogger("morph").info("Loading skin for " + identifier);

                AbstractClientPlayerEntity.loadSkin(id, splitedId[1]);

            }, true);

            living = player;
        }
*/

        cacheMap.put(identifier, living);

        LoggerFactory.getLogger("morph").info("Pushing " + identifier + " into EntityCache.");

        return living;
    }
}

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
    private static final Map<EntityType<?>, LivingEntity> cacheMap = new Object2ObjectOpenHashMap<>();

    @Nullable
    public static LivingEntity getEntity(EntityType<?> type)
    {
        var cache = cacheMap.getOrDefault(type, null);

        LoggerFactory.getLogger("morph").info("Cache of " + type.getTranslationKey() + " is " + cache);

        if (cache != null && !cache.isRemoved()) return cache;

        var world = MinecraftClient.getInstance().world;
        if (world == null) return null;

        var instance = type.create(world);

        if (!(instance instanceof LivingEntity living)) return null;

        if (instance instanceof ArmorStandEntity armorStandEntity)
            ((ArmorStandEntityAccessor)armorStandEntity).callSetShowArms(true);

        if (instance instanceof EnderDragonEntity dragon)
            dragon.getPhaseManager().setPhase(PhaseType.HOVER);

        living.setSilent(true);

        cacheMap.put(type, living);

        LoggerFactory.getLogger("morph").info("Pushing " + type.getTranslationKey() + " into EntityCache.");

        return living;
    }

    @Nullable
    public static LivingEntity getEntity(String identifier)
    {
        if (identifier == null) return null;

        var type = EntityType.get(identifier);

        if (type.isEmpty()) return null;
        else return getEntity(type.get());
    }
}

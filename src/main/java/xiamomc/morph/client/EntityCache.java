package xiamomc.morph.client;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.entities.MorphLocalPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EntityCache
{
    private static final Map<String, LivingEntity> cacheMap = new Object2ObjectOpenHashMap<>();

    public static void clearCache()
    {
        cacheMap.clear();
    }

    public static boolean containsId(int id)
    {
        try
        {
            //照理说values里不该出现null值，但这确实发生了
            return cacheMap.values().stream().filter(l -> l.getId() == id).findFirst().orElse(null) != null;
        }
        catch (Exception e)
        {
            LoggerFactory.getLogger("MorphClient").error("Error checking cache: " + e.getMessage());
            e.printStackTrace();

            cacheMap.remove(null);

            return false;
        }
    }

    public static void discardEntity(String identifier)
    {
        var entity = cacheMap.getOrDefault(identifier, null);

        if (entity != null)
        {
            MorphClient.getInstance().schedule(() ->
            {
                entity.discard();
                entity.onRemoved();
            });

            cacheMap.remove(identifier);
        }
    }

    private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static final Lock readLock = rwLock.readLock();
    private static final Lock writeLock = rwLock.writeLock();

    @Nullable
    public static LivingEntity getEntity(String identifier)
    {
        if (identifier == null) return null;

        LivingEntity cache;

        readLock.lock();

        try
        {
            cache = cacheMap.getOrDefault(identifier, null);
        }
        finally
        {
            readLock.unlock();
        }

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
        }
        else if (identifier.startsWith("player:"))
        {
            var splitedId = identifier.split(":", 2);

            if (splitedId.length != 2) return null;
            var profile = new GameProfile(UUID.randomUUID(), splitedId[1]);
            living = new MorphLocalPlayer(MinecraftClient.getInstance().world, profile);
        }

        if (living == null) return null;

        writeLock.lock();

        try
        {
            cacheMap.put(identifier, living);
        }
        finally
        {
            writeLock.unlock();
        }

        //LoggerFactory.getLogger("morph").info("Pushing " + identifier + " into EntityCache.");

        return living;
    }
}

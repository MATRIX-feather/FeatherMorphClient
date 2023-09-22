package xiamomc.morph.client;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ApiServices;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.entities.MorphLocalPlayer;
import xiamomc.pluginbase.Bindables.Bindable;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
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

    public static final Bindable<Boolean> droppingCaches = new Bindable<>();

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

    private static final long lockWait = 10;

    private static final Map<String, Boolean> isLivingMap = new Object2ObjectArrayMap<>();

    public static boolean isLiving(String identifier)
    {
        return isLivingMap.getOrDefault(identifier, false);
    }

    public static final String tag = "FMC_ClientView";

    public static void dropAll()
    {
        droppingCaches.set(true);
        MorphClient.LOGGER.info("Clearing entity caches...");
        cacheMap.forEach((id, entity) ->
        {
            entity.discard();
            cacheMap.remove(id);
        });

        cacheMap.clear();
        droppingCaches.set(false);
    }

    @Nullable
    public static LivingEntity getEntity(String identifier)
    {
        if (identifier == null) return null;

        LivingEntity cache;

        boolean locked;
        try
        {
            locked = readLock.tryLock(lockWait, TimeUnit.MILLISECONDS);
        }
        catch (Throwable t)
        {
            MorphClient.LOGGER.warn("Unable to lock entity cache for read: " + t.getMessage());
            locked = false;
        }

        if (!locked)
        {
            MorphClient.LOGGER.warn("Unable to lock entity cache for read: Timed out.");
            return null;
        }

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

            try (var world = MinecraftClient.getInstance().world)
            {
                if (world == null) return null;

                var instance = type.create(world);

                if (!(instance instanceof LivingEntity le))
                {
                    isLivingMap.put(identifier, false);
                    return null;
                }

                le.addCommandTag(tag);

                living = le;
            }
            catch (Throwable t)
            {
                MorphClient.LOGGER.error("Error occurred while creating entity: %s".formatted(t.getMessage()));
                t.printStackTrace();

                return null;
            }
        }
        else if (identifier.startsWith("player:"))
        {
            var splitedId = identifier.split(":", 2);

            if (splitedId.length != 2) return null;
            var profile = new GameProfile(Util.NIL_UUID, splitedId[1]);

            try (var world = MinecraftClient.getInstance().world)
            {
                living = new MorphLocalPlayer(world, profile);
            }
            catch (Throwable t)
            {
                MorphClient.LOGGER.error("Error occurred while creating entity: %s".formatted(t.getMessage()));
                t.printStackTrace();
                return null;
            }

            isLivingMap.put(identifier, true);
        }

        if (living == null) return null;

        try
        {
            locked = writeLock.tryLock(lockWait, TimeUnit.MILLISECONDS);
        }
        catch (Throwable t)
        {
            MorphClient.LOGGER.warn("Unable to lock entity cache for write: " + t.getMessage());
            t.printStackTrace();

            return null;
        }

        if (!locked)
        {
            MorphClient.LOGGER.warn("Unable to lock entity cache for write: Timed out");
            return null;
        }

        try
        {
            isLivingMap.put(identifier, true);
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

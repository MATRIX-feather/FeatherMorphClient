package xiamomc.morph.client;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.entities.MorphLocalPlayer;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EntityCache
{
    public static EntityCache getGlobalCache()
    {
        return globalInstance;
    }

    private static final EntityCache globalInstance = new EntityCache();

    private final Map<String, LivingEntity> cacheMap = new Object2ObjectOpenHashMap<>();

    public void clearCache()
    {
        cacheMap.clear();
    }

    public boolean containsId(int id)
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

    public final Bindable<Boolean> droppingCaches = new Bindable<>();

    public void discardEntity(String identifier)
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

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    private final long lockWait = 10;

    private final Map<String, Boolean> isLivingMap = new Object2ObjectArrayMap<>();

    public boolean isLiving(String identifier)
    {
        return isLivingMap.getOrDefault(identifier, false);
    }

    public static final String tag = "FMC_ClientView";

    public void dropAll()
    {
        droppingCaches.set(true);
        cacheMap.forEach((id, entity) ->
        {
            entity.discard();
            cacheMap.remove(id);
        });

        cacheMap.clear();
        droppingCaches.set(false);
    }

    @Nullable
    public LivingEntity getEntity(String identifier, PlayerEntity bindingPlayer)
    {
        if (identifier == null) return null;

        if (disposed.get())
            throw new RuntimeException("Cannot access getEntity() for a disposed EntityCache.");

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

                le.setUuid(UUID.randomUUID());

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
                living = new MorphLocalPlayer(world, profile, bindingPlayer);
                living.setUuid(UUID.randomUUID());
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
            living.addCommandTag(tag);
            living.addCommandTag("BINDING_" + bindingPlayer.getId());

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

    private AtomicBoolean disposed = new AtomicBoolean(false);

    public void dispose()
    {
        if (this == globalInstance) return;

        this.dropAll();
        disposed.set(true);
    }
}

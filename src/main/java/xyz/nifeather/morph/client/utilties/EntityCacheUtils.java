package xyz.nifeather.morph.client.utilties;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import xyz.nifeather.morph.client.MorphClient;

import java.util.Map;
import java.util.function.Consumer;

public class EntityCacheUtils
{
    public static void postEntityRemove(Entity entity)
    {
        onRemove.forEach((obj, consumer) ->
        {
            try
            {
                consumer.accept(entity);
            }
            catch (Throwable t)
            {
                MorphClient.LOGGER.error("Error occurred while processing postEntityRemove hook '%s': %s".formatted(consumer, t));
                t.printStackTrace();
            }
        });
    }

    public static void addOnEntityRemoveHook(Object obj, Consumer<Entity> consumer)
    {
        onRemove.put(obj, consumer);
    }

    public static void removeOnEntityRemoveHook(Object object)
    {
        onRemove.remove(object);
    }

    private static final Map<Object, Consumer<Entity>> onRemove = new Object2ObjectOpenHashMap<>();

    public static void onEntityAdd(Entity entity)
    {
        onAdd.forEach((obj, consumer) ->
        {
            try
            {
                consumer.accept(entity);
            }
            catch (Throwable t)
            {
                MorphClient.LOGGER.error("Error occurred while processing onEntityAdd hook '%s': %s".formatted(consumer, t));
                t.printStackTrace();
            }
        });
    }

    public static void addOnEntityAddHook(Object obj, Consumer<Entity> consumer)
    {
        onRemove.put(obj, consumer);
    }

    public static void removeOnEntityAddHook(Object object)
    {
        onRemove.remove(object);
    }

    private static final Map<Object, Consumer<Entity>> onAdd = new Object2ObjectOpenHashMap<>();

}

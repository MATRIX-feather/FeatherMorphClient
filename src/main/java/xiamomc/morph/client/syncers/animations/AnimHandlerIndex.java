package xiamomc.morph.client.syncers.animations;

import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.syncers.animations.impl.AllayAnimationHandler;
import xiamomc.morph.client.syncers.animations.impl.ArmadilloAnimationHandler;
import xiamomc.morph.client.syncers.animations.impl.SnifferAnimationHandler;
import xiamomc.morph.client.syncers.animations.impl.WardenAnimationHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimHandlerIndex
{
    private final Map<String, AnimationHandler> handlerMap = new ConcurrentHashMap<>();

    public AnimHandlerIndex()
    {
        register("minecraft:warden", new WardenAnimationHandler());
        register("minecraft:sniffer", new SnifferAnimationHandler());
        register("minecraft:allay", new AllayAnimationHandler());
        register("minecraft:armadillo", new ArmadilloAnimationHandler());
    }

    public void register(String disguiseIdentifier, AnimationHandler handler)
    {
        handlerMap.put(disguiseIdentifier, handler);
    }

    @Nullable
    public AnimationHandler get(String disguiseIdentifier)
    {
        return handlerMap.getOrDefault(disguiseIdentifier, null);
    }
}

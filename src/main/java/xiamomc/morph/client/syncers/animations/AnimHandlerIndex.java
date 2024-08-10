package xiamomc.morph.client.syncers.animations;

import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.syncers.animations.impl.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimHandlerIndex
{
    private final Map<String, AnimationHandler> handlerMap = new ConcurrentHashMap<>();

    public AnimHandlerIndex()
    {
        register(EntityType.WARDEN, new WardenAnimationHandler());
        register(EntityType.SNIFFER, new SnifferAnimationHandler());
        register(EntityType.ALLAY, new AllayAnimationHandler());
        register(EntityType.ARMADILLO, new ArmadilloAnimationHandler());
        register(EntityType.SHULKER, new ShulkerAnimationHandler());
        register(EntityType.CAT, new CatAnimationHandler());
        register(EntityType.PARROT, new ParrotAnimationHandler());
        register(EntityType.PIGLIN, new PiglinAnimationHandler());
        register(EntityType.PUFFERFISH, new PufferfishAnimationHandler());
        register(EntityType.FOX, new FoxAnimationHandler());
        register(EntityType.FROG, new FrogAnimationHandler());
        register(EntityType.PANDA, new PandaAnimationHandler());
        register(EntityType.WOLF, new WolfAnimationHandler());
        register(EntityType.PLAYER, new PlayerAnimationHandler());
    }

    public void register(EntityType<?> type, AnimationHandler handler)
    {
        this.register(EntityType.getId(type).toString(), handler);
    }

    public void register(String disguiseIdentifier, AnimationHandler handler)
    {
        handlerMap.put(disguiseIdentifier, handler);
    }

    @Nullable
    public AnimationHandler get(String disguiseIdentifier)
    {
        if (disguiseIdentifier.startsWith("player:"))
            disguiseIdentifier = "minecraft:player";

        return handlerMap.getOrDefault(disguiseIdentifier, null);
    }
}

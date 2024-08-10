package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PiglinEntity;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

public class PiglinAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof PiglinEntity piglin))
            throw new IllegalArgumentException("Entity not a Piglin!");

        switch (animationId)
        {
            case AnimationNames.DANCE_START -> piglin.setDancing(true);
            case AnimationNames.STOP -> piglin.setDancing(false);
        }
    }
}

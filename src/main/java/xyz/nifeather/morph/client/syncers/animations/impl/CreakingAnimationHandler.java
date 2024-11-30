package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreakingEntity;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

public class CreakingAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof CreakingEntity creaking))
            throw new IllegalArgumentException("Entity not a Creaking!");

        if (animationId.equals(AnimationNames.MAKE_ACTIVE))
            creaking.setActive(true);

        switch (animationId)
        {
            case AnimationNames.MAKE_ACTIVE -> creaking.setActive(true);
            case AnimationNames.MAKE_INACTIVE -> creaking.setActive(false);
        }
    }
}

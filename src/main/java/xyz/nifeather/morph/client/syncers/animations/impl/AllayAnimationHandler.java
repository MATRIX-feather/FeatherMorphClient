package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.entities.IAllay;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

public class AllayAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof IAllay allay))
            throw new IllegalArgumentException("Entity not an Allay!");

        switch (animationId)
        {
            case AnimationNames.DANCE_START -> allay.morphclient$forceSetDancing(true);
            case AnimationNames.STOP -> allay.morphclient$forceSetDancing(false);
        }
    }
}

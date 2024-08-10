package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AllayEntity;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.entities.IAllay;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

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

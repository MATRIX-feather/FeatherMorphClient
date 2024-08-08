package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CatEntity;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

public class CatAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof CatEntity cat))
            throw new IllegalArgumentException("Entity not a Cat!");

        switch (animationId)
        {
            case AnimationNames.LAY_START -> cat.setInSleepingPose(true);
            case AnimationNames.LAY_STOP -> cat.setInSleepingPose(false);
        }
    }
}

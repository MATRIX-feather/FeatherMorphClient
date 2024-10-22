package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CatEntity;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

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
            case AnimationNames.STANDUP ->
            {
                cat.setInSleepingPose(false);
                cat.setSitting(false);
                cat.setInSittingPose(false);
            }
            case AnimationNames.SIT ->
            {
                cat.setSitting(true);
                cat.setInSittingPose(true);
            }
        }
    }
}

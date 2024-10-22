package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.WolfEntity;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

public class WolfAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof WolfEntity wolf))
            throw new IllegalArgumentException("Entity not a Wolf!");

        switch (animationId)
        {
            case AnimationNames.SIT -> wolf.setInSittingPose(true);
            case AnimationNames.STANDUP -> wolf.setInSittingPose(false);
        }
    }
}

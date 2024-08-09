package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.passive.FrogEntity;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

public class FrogAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof FrogEntity frog))
            throw new IllegalArgumentException("Entity not a Frog!");

        switch (animationId)
        {
            case AnimationNames.EAT ->
            {
                frog.setPose(EntityPose.USING_TONGUE);
                frog.setPose(EntityPose.STANDING);
            }
        }
    }
}

package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.passive.FrogEntity;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.entities.IEntity;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

public class FrogAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof FrogEntity frog))
            throw new IllegalArgumentException("Entity not a Frog!");

        var mixinFrog = (IEntity) frog;

        switch (animationId)
        {
            case AnimationNames.EAT -> mixinFrog.featherMorph$setOverridePose(EntityPose.USING_TONGUE);
            case AnimationNames.POSE_RESET -> mixinFrog.featherMorph$setOverridePose(null);
        }
    }
}

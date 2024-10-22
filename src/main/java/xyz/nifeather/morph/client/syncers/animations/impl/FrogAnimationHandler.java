package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.passive.FrogEntity;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.entities.IEntity;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

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
            case AnimationNames.EAT -> mixinFrog.featherMorph$overridePose(EntityPose.USING_TONGUE);
            case AnimationNames.RESET -> mixinFrog.featherMorph$overridePose(null);
        }
    }
}

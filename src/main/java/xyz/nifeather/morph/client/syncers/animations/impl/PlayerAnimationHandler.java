package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.entities.MorphLocalPlayer;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

public class PlayerAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof MorphLocalPlayer localPlayer))
            throw new IllegalArgumentException("Entity noy a Local Player!");

        switch (animationId)
        {
            case AnimationNames.LAY -> localPlayer.setOverridePose(EntityPose.SLEEPING);
            case AnimationNames.CRAWL -> localPlayer.setOverridePose(EntityPose.SWIMMING);
            case AnimationNames.STANDUP -> localPlayer.setOverridePose(null);
        }
    }
}

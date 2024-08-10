package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.entities.MorphLocalPlayer;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

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
            case AnimationNames.PROSTRATE -> localPlayer.setOverridePose(EntityPose.SWIMMING);
            case AnimationNames.STANDUP -> localPlayer.setOverridePose(null);
        }
    }
}

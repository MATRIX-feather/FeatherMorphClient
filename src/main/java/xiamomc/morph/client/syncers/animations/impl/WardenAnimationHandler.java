package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.mob.WardenEntity;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

public class WardenAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof WardenEntity warden))
            throw new IllegalArgumentException("Entity not a Warden!");

        switch (animationId)
        {
            case AnimationNames.ROAR -> warden.setPose(EntityPose.ROARING);
            case AnimationNames.SNIFF -> warden.setPose(EntityPose.SNIFFING);
            case AnimationNames.DISAPPEAR -> warden.setPose(EntityPose.DIGGING);
            case AnimationNames.APPEAR ->
            {
                warden.diggingAnimationState.stop();
                warden.setPose(EntityPose.EMERGING);
            }
        }
    }
}

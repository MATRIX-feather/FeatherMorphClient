package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.mob.WardenEntity;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.entities.IEntity;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

public class WardenAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof WardenEntity warden))
            throw new IllegalArgumentException("Entity not a Warden!");

        var mixinWarden = (IEntity) warden;

        switch (animationId)
        {
            case AnimationNames.ROAR -> mixinWarden.featherMorph$setOverridePose(EntityPose.ROARING);
            case AnimationNames.SNIFF -> mixinWarden.featherMorph$setOverridePose(EntityPose.SNIFFING);
            case AnimationNames.DISAPPEAR -> mixinWarden.featherMorph$setOverridePose(EntityPose.DIGGING);
            case AnimationNames.APPEAR ->
            {
                warden.diggingAnimationState.stop();
                mixinWarden.featherMorph$setOverridePose(EntityPose.EMERGING);
            }
            case AnimationNames.POSE_RESET -> mixinWarden.featherMorph$setOverridePose(null);
        }
    }
}

package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.mob.WardenEntity;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.entities.IMorphClientEntity;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

public class WardenAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof WardenEntity warden))
            throw new IllegalArgumentException("Entity not a Warden!");

        var mixinWarden = (IMorphClientEntity) warden;

        switch (animationId)
        {
            case AnimationNames.ROAR ->
            {
                mixinWarden.featherMorph$overridePose(EntityPose.ROARING);
                mixinWarden.featherMorph$setNoAcceptSetPose(true);
            }
            case AnimationNames.SNIFF ->
            {
                mixinWarden.featherMorph$overridePose(EntityPose.SNIFFING);
                mixinWarden.featherMorph$setNoAcceptSetPose(true);
            }
            case AnimationNames.DIGDOWN ->
            {
                mixinWarden.featherMorph$overridePose(EntityPose.DIGGING);
                mixinWarden.featherMorph$setNoAcceptSetPose(true);
            }
            case AnimationNames.INTERNAL_VANISH -> mixinWarden.featherMorph$overrideInvisibility(true);
            case AnimationNames.APPEAR ->
            {
                mixinWarden.featherMorph$overrideInvisibility(false);
                warden.diggingAnimationState.stop();

                mixinWarden.featherMorph$setNoAcceptSetPose(false);
                mixinWarden.featherMorph$overridePose(EntityPose.EMERGING);
                mixinWarden.featherMorph$setNoAcceptSetPose(true);
            }
            case AnimationNames.TRY_RESET, AnimationNames.RESET ->
            {
                mixinWarden.featherMorph$overridePose(null);
                mixinWarden.featherMorph$setNoAcceptSetPose(false);
            }
        }
    }
}

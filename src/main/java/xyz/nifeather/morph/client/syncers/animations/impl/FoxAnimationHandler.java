package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.FoxEntity;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.entities.IFox;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

public class FoxAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof FoxEntity fox))
            throw new IllegalArgumentException("Entity not a Fox!");

        var mixinFox = (IFox) fox;

        switch (animationId)
        {
            case AnimationNames.SIT ->
            {
                mixinFox.morphclient$forceSetSleeping(false);
                fox.setSitting(true);
            }
            case AnimationNames.SLEEP ->
            {
                fox.setSitting(false);
                mixinFox.morphclient$forceSetSleeping(true);
            }
            case AnimationNames.STANDUP ->
            {
                mixinFox.morphclient$forceSetSleeping(false);
                fox.setSitting(false);
            }
        }
    }
}

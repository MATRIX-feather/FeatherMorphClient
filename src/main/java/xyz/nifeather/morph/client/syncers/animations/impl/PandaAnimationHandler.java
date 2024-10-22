package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.PandaEntity;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

public class PandaAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if(!(entity instanceof PandaEntity panda))
            throw new IllegalArgumentException("Entity not a Panda!");

        switch (animationId)
        {
            case AnimationNames.SIT -> panda.setSitting(true);
            case AnimationNames.STANDUP -> panda.setSitting(false);
        }
    }
}

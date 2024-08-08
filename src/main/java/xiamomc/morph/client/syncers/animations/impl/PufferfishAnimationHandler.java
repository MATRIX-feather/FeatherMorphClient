package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.PufferfishEntity;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

public class PufferfishAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof PufferfishEntity pufferfish))
            throw new IllegalArgumentException("Entity not a Pufferfish!");

        switch (animationId)
        {
            case AnimationNames.INFLATE -> pufferfish.setPuffState(PufferfishEntity.FULLY_PUFFED);
            case AnimationNames.DEFLATE -> pufferfish.setPuffState(PufferfishEntity.NOT_PUFFED);
        }
    }
}

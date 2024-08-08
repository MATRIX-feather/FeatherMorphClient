package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.util.math.BlockPos;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

public class ParrotAnimationHandler extends AnimationHandler
{
    private static final BlockPos bPos = new BlockPos(0, 0, 0);

    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof ParrotEntity parrot))
            throw new IllegalArgumentException("Entity not a Parrot!");

        switch (animationId)
        {
            case AnimationNames.DANCE_START -> parrot.setNearbySongPlaying(bPos, true);
            case AnimationNames.DANCE_STOP -> parrot.setNearbySongPlaying(bPos, false);
        }
    }
}

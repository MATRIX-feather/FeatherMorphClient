package xiamomc.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.ArmadilloEntity;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.syncers.animations.AnimationHandler;

public class ArmadilloAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof ArmadilloEntity armadillo))
            throw new IllegalArgumentException("Entity not an Armadillo!");

        switch (animationId)
        {
            case AnimationNames.PANIC_ROLLING -> armadillo.setState(ArmadilloEntity.State.ROLLING);
            case AnimationNames.PANIC_SCARED -> armadillo.setState(ArmadilloEntity.State.SCARED);
            case AnimationNames.PANIC_UNROLLING -> armadillo.setState(ArmadilloEntity.State.UNROLLING);
            case AnimationNames.PANIC_IDLE -> armadillo.setState(ArmadilloEntity.State.IDLE);
        }
    }
}

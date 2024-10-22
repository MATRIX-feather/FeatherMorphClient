package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ShulkerEntity;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.mixin.accessors.ShulkerEntityAccessor;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

public class ShulkerAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof ShulkerEntity shulker))
            throw new IllegalArgumentException("Entity not a Shulker!");

        var asAccessor = (ShulkerEntityAccessor) shulker;

        switch (animationId)
        {
            case AnimationNames.PEEK_START -> asAccessor.callSetPeekAmount(30);
            case AnimationNames.OPEN_START -> asAccessor.callSetPeekAmount(100);
            case AnimationNames.PEEK_STOP, AnimationNames.OPEN_STOP -> asAccessor.callSetPeekAmount(0);
        }
    }
}

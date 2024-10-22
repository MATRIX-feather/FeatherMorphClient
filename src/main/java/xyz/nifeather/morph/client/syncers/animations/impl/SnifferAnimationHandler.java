package xyz.nifeather.morph.client.syncers.animations.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SnifferEntity;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;

public class SnifferAnimationHandler extends AnimationHandler
{
    @Override
    public void play(Entity entity, String animationId)
    {
        if (!(entity instanceof SnifferEntity sniffer))
            throw new IllegalArgumentException("Entity not a Sniffer!");

        switch (animationId)
        {
            case AnimationNames.SNIFF ->
            {
                sniffer.startState(SnifferEntity.State.IDLING);
                sniffer.startState(SnifferEntity.State.SNIFFING);
            }
        }
    }
}

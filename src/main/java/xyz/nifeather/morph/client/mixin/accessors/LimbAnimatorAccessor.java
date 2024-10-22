package xyz.nifeather.morph.client.mixin.accessors;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LimbAnimator.class)
public interface LimbAnimatorAccessor
{
    @Accessor
    public void setPrevSpeed(float prevSpd);

    @Accessor
    public void setSpeed(float spd);

    @Accessor
    public void setPos(float pos);

    @Accessor
    public float getPrevSpeed();
}

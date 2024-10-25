package xyz.nifeather.morph.client.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.EntityTickHandler;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onTick(CallbackInfo ci)
    {
        if (((LivingEntity)(Object)this).getWorld().isClient())
            EntityTickHandler.cancelIfIsDisguiseAndNotSyncing(ci, this);
    }
}

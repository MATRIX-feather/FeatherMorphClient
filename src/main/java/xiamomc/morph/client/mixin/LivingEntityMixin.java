package xiamomc.morph.client.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamomc.morph.client.EntityTickHandler;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onTick(CallbackInfo ci)
    {
        EntityTickHandler.cancelIfIsDisguiseAndNotSyncing(ci, this);
    }
}

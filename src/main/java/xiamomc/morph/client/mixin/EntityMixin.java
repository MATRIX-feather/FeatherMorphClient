package xiamomc.morph.client.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiamomc.morph.client.EntityCache;

@Mixin(Entity.class)
public abstract class EntityMixin
{
    @Shadow
    private int id;

    @Inject(method = "squaredDistanceTo(Lnet/minecraft/entity/Entity;)D", at = @At("HEAD"), cancellable = true)
    private void morphClient$onSquaredDistanceToCall(CallbackInfoReturnable<Double> cir)
    {
        if (EntityCache.containsId(id))
            cir.setReturnValue(1d);
    }
}

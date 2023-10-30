package xiamomc.morph.client.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamomc.morph.client.syncers.ClientDisguiseSyncer;
import xiamomc.morph.client.utilties.ClientSyncerUtils;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onTick(CallbackInfo ci)
    {
        ClientSyncerUtils.runIfSyncerEntityValid(entity ->
        {
            if (this.equals(entity) && !ClientDisguiseSyncer.syncing)
                ci.cancel();
        });
    }
}

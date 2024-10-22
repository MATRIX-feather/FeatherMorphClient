package xyz.nifeather.morph.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin
{
    /*
    @Inject(method = "getReachDistance", at = @At("HEAD"), cancellable = true)
    private void feathermorph$onGetReachDistance(CallbackInfoReturnable<Float> cir)
    {
        if (ServerHandler.reach > 0)
            cir.setReturnValue(ServerHandler.reach);
    }
    */
}

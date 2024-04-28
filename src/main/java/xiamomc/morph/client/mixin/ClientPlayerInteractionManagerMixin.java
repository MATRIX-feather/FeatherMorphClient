package xiamomc.morph.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiamomc.morph.client.ServerHandler;

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

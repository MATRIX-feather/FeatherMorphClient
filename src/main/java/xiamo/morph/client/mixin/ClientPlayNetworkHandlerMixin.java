package xiamo.morph.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamo.morph.client.MorphClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin
{
    @Inject(method = "onGameJoin", at = @At(value = "RETURN"))
    private void onConnect(CallbackInfo ci)
    {
        MorphClient.getInstance().initializeData();
    }
}

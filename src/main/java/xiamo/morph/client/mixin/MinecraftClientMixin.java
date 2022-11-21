package xiamo.morph.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamo.morph.client.MorphClient;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin
{
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",
            at = @At(value = "RETURN", shift = At.Shift.AFTER))
    private void onDisconnect(CallbackInfo ci)
    {
        MorphClient.getInstance().resetServerStatus();
    }
}

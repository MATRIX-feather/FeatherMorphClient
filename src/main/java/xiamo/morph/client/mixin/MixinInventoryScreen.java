package xiamo.morph.client.mixin;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamo.morph.client.MorphClient;
import xiamo.morph.client.graphics.InventoryRenderHelper;

@Mixin(InventoryScreen.class)
public class MixinInventoryScreen
{
    private final InventoryRenderHelper helper = MorphClient.renderHelper;

    @Redirect(method = "drawBackground",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawEntity(IIIFFLnet/minecraft/entity/LivingEntity;)V"))
    public void bbb(int x, int y, int size, float mouseX, float mouseY, LivingEntity no)
    {
       helper.onRenderCall(x, y, size, mouseX, mouseY);
    }
}

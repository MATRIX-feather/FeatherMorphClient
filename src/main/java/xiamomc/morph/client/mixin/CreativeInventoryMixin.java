package xiamomc.morph.client.mixin;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xiamomc.morph.client.graphics.InventoryRenderHelper;

@Mixin(CreativeInventoryScreen.class)
public class CreativeInventoryMixin
{
    private static final InventoryRenderHelper helper = InventoryRenderHelper.getInstance();

    @Redirect(method = "drawBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawEntity(Lnet/minecraft/client/util/math/MatrixStack;IIIFFLnet/minecraft/entity/LivingEntity;)V"))
    public void onBackgroundDrawCall(MatrixStack matrixStack, int x, int y, int size, float mouseX, float mouseY, LivingEntity no)
    {
        helper.onRenderCall(matrixStack, x, y, size, mouseX, mouseY);
    }
}

package xiamomc.morph.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xiamomc.morph.client.graphics.InventoryRenderHelper;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin
{
    private static final InventoryRenderHelper helper = InventoryRenderHelper.getInstance();

    @Redirect(method = "drawBackground",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIFFLnet/minecraft/entity/LivingEntity;)V"))
    public void onBackgroundDrawCall(DrawContext matrices, int x, int y, int size, float mouseX, float mouseY, LivingEntity entity)
    {
       helper.onRenderCall(matrices, x, y, size, mouseX, mouseY);
    }
}

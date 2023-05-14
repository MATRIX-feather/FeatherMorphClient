package xiamomc.morph.client.graphics.toasts;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class NewDisguiseSetToast extends LinedToast
{
    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    @Override
    public int getWidth()
    {
        var descLength = textRenderer.getWidth(getDescription());
        var titleLength = textRenderer.getWidth(getTitle());
        var max1 = Math.max(descLength, titleLength) + 20;

        return Math.max(super.getWidth(), max1);
    }

    private static final Identifier TEX = Identifier.of(Identifier.DEFAULT_NAMESPACE, "textures/gui/info_icon.png");

    @Override
    protected void postBackgroundDrawing(MatrixStack matrices, ToastManager manager, long startTime)
    {
        super.postBackgroundDrawing(matrices, manager, startTime);

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, TEX);

        DrawableHelper.drawTexture(matrices, this.getWidth() / 16 - 2, 6, 0, 0, 20, 20, 20, 20);
    }
}

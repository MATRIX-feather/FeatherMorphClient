package xiamomc.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import xiamomc.pluginbase.Annotations.Initializer;

public class LoadingSpinner extends MDrawable
{
    private static final Identifier LOADING_TEX = new Identifier("morphclient", "textures/gui/loading.png");

    public LoadingSpinner()
    {
        this.setWidth(16);
        this.setHeight(16);
    }

    @Override
    protected void onRender(MatrixStack matrixStack, int mouseX, int mouseY, float delta)
    {
        renderLoading(matrixStack, 0, 0);
        super.onRender(matrixStack, mouseX, mouseY, delta);
    }

    @Deprecated
    public void renderLoading(MatrixStack matrixStack, int x, int y)
    {
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, LOADING_TEX);
        int offset = (int)plugin.getCurrentTick() / 4;

        DrawableHelper.drawTexture(matrixStack, x, y, 0, 16 * offset, 16, 16, 16, 128);
    }
}

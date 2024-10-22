package xyz.nifeather.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class LoadingSpinner extends MDrawable
{
    private static final Identifier LOADING_TEX = Identifier.of("morphclient", "textures/gui/loading.png");

    public LoadingSpinner()
    {
        this.setWidth(16);
        this.setHeight(16);
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, LOADING_TEX);
        int offset = (int)plugin.getCurrentTick() / 4;

        context.drawTexture(LOADING_TEX, 0, 0, 0, 16 * offset, 16, 16, 16, 128);
        super.onRender(context, mouseX, mouseY, delta);
    }
}

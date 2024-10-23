package xyz.nifeather.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public class LoadingSpinner extends MDrawable
{
    private static final Identifier LOADING_TEX = Identifier.of("morphclient", "loading");

    public LoadingSpinner()
    {
        this.setWidth(16);
        this.setHeight(16);
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        RenderSystem.enableBlend();

        context.drawGuiTexture(RenderLayer::getGuiTextured, LOADING_TEX, 0, 0, 16, 16);
        super.onRender(context, mouseX, mouseY, delta);
    }
}

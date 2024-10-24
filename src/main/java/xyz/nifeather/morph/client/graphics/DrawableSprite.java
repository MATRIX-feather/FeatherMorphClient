package xyz.nifeather.morph.client.graphics;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public class DrawableSprite extends MDrawable
{
    private final Identifier textureIdentifier;

    public DrawableSprite(Identifier textureIdentifier)
    {
        this.textureIdentifier = textureIdentifier;
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        context.drawGuiTexture(RenderLayer::getGuiTextured, textureIdentifier,
                this.getX(), this.getY(),
                Math.round(this.getRenderWidth()), Math.round(this.getRenderHeight()));
    }
}

package xyz.nifeather.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
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
        RenderSystem.enableBlend();

        context.drawGuiTexture(textureIdentifier,
                this.getX(), this.getY(), 0,
                Math.round(this.getRenderWidth()), Math.round(this.getRenderHeight()));
    }
}

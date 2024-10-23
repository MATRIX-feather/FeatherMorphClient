package xyz.nifeather.morph.client.graphics;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xyz.nifeather.morph.client.graphics.color.Colors;

public class DrawableSprite extends MDrawable
{
    private final Identifier textureIdentifier;

    public DrawableSprite(Identifier textureIdentifier)
    {
        this.textureIdentifier = textureIdentifier;

        this.alpha.onUpdate = this::updateRenderAlpha;
    }

    private int drawColor = -1;

    private void updateRenderAlpha(float newAlpha)
    {
        drawColor = ColorUtils.forOpacity(Colors.WHITE, this.alpha.get()).getColor();
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        context.drawGuiTexture(RenderLayer::getGuiTextured, textureIdentifier,
                this.getX(), this.getY(),
                Math.round(this.getRenderWidth()), Math.round(this.getRenderHeight()),
                drawColor);
    }
}

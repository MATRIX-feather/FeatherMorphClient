package xyz.nifeather.morph.client.graphics.container;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import xyz.nifeather.morph.client.graphics.MDrawable;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;

public class DrawableButtonWrapper extends MDrawable
{
    private final ButtonWidget widget;

    public DrawableButtonWrapper(ButtonWidget widget)
    {
        this.setHeight(widget.getHeight());
        this.setWidth(widget.getWidth());
        this.widget = widget;
    }

    @Override
    protected void updatePosition()
    {
        super.updatePosition();

        widget.setX(Math.round(this.getScreenSpaceX()));
        widget.setY(Math.round(this.getScreenSpaceY()));
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        context.fill(0, 0, renderWidth, renderHeight, MaterialColors.Amber500.getColor());

        context.getMatrices().translate(-this.getScreenSpaceX(), -this.getScreenSpaceY(), 0);
        widget.render(context, mouseX, mouseY, delta);

        super.onRender(context, mouseX, mouseY, delta);
    }

    @Override
    public void setFocused(boolean focused)
    {
        widget.setFocused(focused);
    }

    @Override
    public boolean isFocused()
    {
        return widget.isFocused();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        return widget.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        return widget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        widget.mouseClicked(mouseX, mouseY, button);
        return true;
    }
}

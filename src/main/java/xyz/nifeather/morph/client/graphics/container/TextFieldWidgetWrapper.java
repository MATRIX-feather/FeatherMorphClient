package xyz.nifeather.morph.client.graphics.container;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.graphics.MDrawable;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;

import java.util.function.Consumer;

public class TextFieldWidgetWrapper extends MDrawable
{
    public TextFieldWidget widget;

    public TextFieldWidgetWrapper(TextFieldWidget fieldWidget)
    {
        this.widget = fieldWidget;

        this.setHeight(fieldWidget.getHeight());
        this.setWidth(fieldWidget.getWidth());
    }

    public TextFieldWidget widget()
    {
        return widget;
    }

    public void setChangedListener(Consumer<String> changedListener)
    {
        widget.setChangedListener(changedListener);
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
    public void setWidth(float w)
    {
        super.setWidth(w);

        widget.setWidth(Math.round(w));
    }

    @Override
    public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation)
    {
        return widget.getNavigationPath(navigation);
    }

    @Override
    public ScreenRect getNavigationFocus()
    {
        return widget.getNavigationFocus();
    }

    @Override
    public @Nullable GuiNavigationPath getFocusedPath()
    {
        return widget.getFocusedPath();
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
    public void setHeight(float h)
    {
        super.setHeight(h);

        widget.setHeight(Math.round(h));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        return widget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        return widget.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers)
    {
        return widget.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        logger.info("CLICKL! is Over? " + widget.isMouseOver(mouseX, mouseY));
        return widget.mouseClicked(mouseX, mouseY, button);
    }
}

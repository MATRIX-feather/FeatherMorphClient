package xyz.nifeather.morph.client.graphics;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MButtonWidget extends ButtonWidget implements IMDrawable
{
    protected MButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, NarrationSupplier narrationSupplier)
    {
        super(x, y, width, height, message, onPress, narrationSupplier);
    }

    public static MButtonWidget from(ButtonWidget widget, PressAction onPress)
    {
        return new MButtonWidget(
                widget.getX(), widget.getY(),
                widget.getWidth(), widget.getHeight(),
                widget.getMessage(), onPress,
                ButtonWidget.DEFAULT_NARRATION_SUPPLIER
        );
    }

    @Override
    public void invalidatePosition()
    {
    }

    @Override
    public void invalidateLayout()
    {
    }

    @Override
    public void setWidth(float width)
    {
        this.setWidth(Math.round(width));
    }

    @Override
    public void setHeight(float height)
    {
        this.setHeight(Math.round(height));
    }

    @Override
    public void setSize(Vector2f vector)
    {
        this.setWidth(vector.getX());
        this.setHeight(vector.getY());
    }

    @Override
    public float getRenderWidth()
    {
        return this.getWidth();
    }

    @Override
    public float getRenderHeight()
    {
        return this.getHeight();
    }

    @Override
    public @NotNull MarginPadding getPadding()
    {
        return new MarginPadding(0);
    }

    @Nullable
    private IMDrawable parent;

    @Override
    public void setParent(@Nullable IMDrawable parent)
    {
        this.parent = parent;
    }

    @Override
    public @Nullable IMDrawable getParent()
    {
        return this.parent;
    }

    @Override
    public float getScreenSpaceX()
    {
        return getX();
    }

    @Override
    public float getScreenSpaceY()
    {
        return getY();
    }

    private int depth = 0;

    /**
     * Depth of this IMDrawable, higher value means this drawable should be rendered below others
     */
    @Override
    public int getDepth()
    {
        return this.depth;
    }

    @Override
    public void setDepth(int depth)
    {
        this.depth = depth;
    }
}

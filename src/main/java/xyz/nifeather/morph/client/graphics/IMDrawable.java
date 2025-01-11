package xyz.nifeather.morph.client.graphics;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.util.math.Vector2f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IMDrawable extends Drawable, Element, Selectable
{
    public void invalidatePosition();
    public void invalidateLayout();

    public void setWidth(float width);
    public void setHeight(float height);
    public void setSize(Vector2f vector);

    public float getRenderWidth();
    public float getRenderHeight();

    @NotNull
    public MarginPadding getPadding();

    public void setParent(@Nullable IMDrawable parent);

    @Nullable
    public IMDrawable getParent();

    public float getScreenSpaceX();
    public float getScreenSpaceY();

    /**
     * Depth of this IMDrawable, higher value means this drawable should be rendered below others
     */
    public int getDepth();
    public void setDepth(int depth);

    default void dispose()
    {
    }
}

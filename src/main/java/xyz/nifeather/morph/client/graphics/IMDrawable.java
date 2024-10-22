package xyz.nifeather.morph.client.graphics;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;

public interface IMDrawable extends Drawable, Element, Selectable
{
    public void invalidatePosition();
    public void invalidateLayout();

    /**
     * Depth of this IMDrawable, higher value means this drawable should be rendered below others
     */
    public int getDepth();
    public void setDepth(int depth);

    default void dispose()
    {
    }
}

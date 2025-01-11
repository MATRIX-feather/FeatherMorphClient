package xyz.nifeather.morph.client.graphics.container;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.graphics.IMDrawable;
import xyz.nifeather.morph.client.graphics.MDrawable;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BasicContainer<T extends IMDrawable> extends MDrawable implements ParentElement
{
    //region Layout Validation

    @Override
    @ApiStatus.Internal
    public void invalidatePosition()
    {
        invalidateLayout();
        super.invalidatePosition();

        this.children.forEach(IMDrawable::invalidatePosition);
    }

    public List<T> children()
    {
        return new ObjectArrayList<>(children);
    }

    @Override
    public void invalidateLayout()
    {
        this.children.forEach(IMDrawable::invalidateLayout);
        super.invalidateLayout();
    }

    protected void updateLayout()
    {
        for (IMDrawable child : children)
            child.setParent(this);

        super.updateLayout();
    }

    //endregion Layout Validation

    protected final List<T> children = new ObjectArrayList<>();

    public void add(T drawable) {
        children.add(drawable);
        drawable.setParent(this);

        invalidateLayout();
    }

    public void addRange(T... drawables) {
        children.addAll(Arrays.stream(drawables).toList());

        invalidateLayout();
    }

    public void addRange(Collection<T> drawables) {
        children.addAll(drawables);

        invalidateLayout();
    }

    public void remove(T drawable) {
        drawable.setParent(null);
        this.children.remove(drawable);
        drawable.dispose();

        invalidateLayout();
    }

    public void removeRange(T[] drawables) {
        children.removeAll(Arrays.stream(drawables).toList());
        for (T drawable : drawables)
            drawable.dispose();

        invalidateLayout();
    }

    public void removeRange(Collection<T> drawables) {
        children.removeAll(drawables);
        for (T drawable : drawables)
            drawable.dispose();

        invalidateLayout();
    }

    public void clear() {
        children.forEach(drawable ->
        {
            drawable.setParent(null);
            drawable.dispose();
        });

        children.clear();

        invalidateLayout();
    }

    public boolean contains(T drawable) {
        return children.contains(drawable);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        var filter = this.children.stream().filter(child ->
        {
            return mouseX > child.getScreenSpaceX() && mouseX < child.getScreenSpaceX() + child.getRenderWidth()
                    && mouseY > child.getScreenSpaceY() && mouseY < child.getScreenSpaceY() + child.getRenderHeight();
        }).toList();

        boolean handled = false;

        for (T t : filter)
        {
            if (t.mouseClicked(mouseX, mouseY, button))
            {
                handled = true;
                break;
            }
        }

        return handled;
    }

    //region ParentElement

    @Override
    public boolean isDragging()
    {
        return dragging;
    }

    private boolean dragging;

    @Override
    public void setDragging(boolean dragging)
    {
        this.dragging = dragging;
    }

    @Override
    public @Nullable Element getFocused()
    {
        return focusedElement;
    }

    private Element focusedElement;

    @Override
    public void setFocused(@Nullable Element focused)
    {
        if (this.focusedElement != null)
            this.focusedElement.setFocused(false);

        this.focusedElement = focused;

        if (focused != null)
            focused.setFocused(true);
    }

    //endregion ParentElement

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.onRender(context, mouseX, mouseY, delta);

        var matrices = context.getMatrices();

        matrices.push();

        matrices.translate(0, 0, 50);

        //context.fill(0, 0, renderWidth, renderHeight, MaterialColors.Blue500.getColor());

        try
        {
            this.children.forEach(d ->
            {
                var haveDepth = d.getDepth() != 0;

                if (haveDepth)
                    matrices.translate(0, 0, -d.getDepth());

                d.render(context, mouseX, mouseY, delta);

                if (haveDepth)
                    matrices.translate(0, 0, d.getDepth());
            });
        }
        finally
        {
            //matrices.translate(0, 0, -50);
            matrices.pop();
        }
    }

    @Override
    public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation)
    {
        return ParentElement.super.getNavigationPath(navigation);
    }

    @Override
    public void dispose()
    {
        super.dispose();
        children.forEach(IMDrawable::dispose);
    }
}

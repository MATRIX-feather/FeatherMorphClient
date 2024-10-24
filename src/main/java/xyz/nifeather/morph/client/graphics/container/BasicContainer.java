package xyz.nifeather.morph.client.graphics.container;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.ApiStatus;
import xyz.nifeather.morph.client.graphics.MDrawable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BasicContainer<T extends MDrawable> extends MDrawable
{
    //region Layout Validation

    @Override
    @ApiStatus.Internal
    public void invalidatePosition()
    {
        invalidateLayout();
        super.invalidatePosition();

        this.children.forEach(MDrawable::invalidatePosition);
    }

    @Override
    public void invalidateLayout()
    {
        this.children.forEach(MDrawable::invalidateLayout);
        super.invalidateLayout();
    }

    protected void updateLayout()
    {
        for (MDrawable child : children)
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
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.onRender(context, mouseX, mouseY, delta);

        var matrices = context.getMatrices();

        matrices.push();

        matrices.translate(0, 0, 50);

        try
        {
            this.children.forEach(d ->
            {
                matrices.translate(0, 0, -d.getDepth());
                d.render(context, mouseX, mouseY, delta);
                matrices.translate(0, 0, d.getDepth());
            });
        }
        finally
        {
            matrices.translate(0, 0, -50);
            matrices.pop();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        children.forEach(MDrawable::dispose);
    }
}

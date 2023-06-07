package xiamomc.morph.client.graphics;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Container extends MDrawable
{
    //region Layout Validation

    @Override
    @ApiStatus.Internal
    public void invalidatePosition()
    {
        invalidateLayout();
        super.invalidatePosition();
    }

    private final AtomicBoolean layoutValid = new AtomicBoolean(false);

    private void updateLayout()
    {
        for (MDrawable child : children)
            child.setParent(this);

        layoutValid.set(true);
    }

    protected void invalidateLayout()
    {
        layoutValid.set(false);
    }

    //endregion Layout Validation

    private final List<MDrawable> children = new ObjectArrayList<>();

    public void add(MDrawable drawable)
    {
        children.add(drawable);

        invalidateLayout();
    }

    public void addRange(MDrawable... drawables)
    {
        children.addAll(Arrays.stream(drawables).toList());

        invalidateLayout();
    }

    public void addRange(Collection<MDrawable> drawables)
    {
        children.addAll(drawables);

        invalidateLayout();
    }

    public void remove(MDrawable drawable)
    {
        drawable.setParent(null);

        invalidateLayout();
    }

    public void removeRange(MDrawable[] drawables)
    {
        children.removeAll(Arrays.stream(drawables).toList());

        invalidateLayout();
    }

    public void removeRange(Collection<MDrawable> drawables)
    {
        children.removeAll(drawables);

        invalidateLayout();
    }

    public void clear()
    {
        children.forEach(MDrawable::dispose);
        children.clear();

        invalidateLayout();
    }

    public boolean contains(MDrawable drawable)
    {
        return children.contains(drawable);
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        if (!layoutValid.get()) updateLayout();

        super.onRender(context, mouseX, mouseY, delta);
        this.children.forEach(d -> d.render(context, mouseX, mouseY, delta));
    }

    @Override
    public void dispose()
    {
        super.dispose();
        children.forEach(MDrawable::dispose);
    }
}

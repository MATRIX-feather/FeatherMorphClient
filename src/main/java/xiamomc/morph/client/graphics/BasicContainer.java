package xiamomc.morph.client.graphics;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BasicContainer<T extends MDrawable> extends MDrawable
{
    //region Layout Validation

    @Override
    @ApiStatus.Internal
    public void invalidatePosition() {
        invalidateLayout();
        super.invalidatePosition();
    }

    private final AtomicBoolean layoutValid = new AtomicBoolean(false);

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

        invalidateLayout();
    }

    public void removeRange(T[] drawables) {
        children.removeAll(Arrays.stream(drawables).toList());

        invalidateLayout();
    }

    public void removeRange(Collection<T> drawables) {
        children.removeAll(drawables);

        invalidateLayout();
    }

    public void clear() {
        children.forEach(MDrawable::dispose);
        children.clear();

        invalidateLayout();
    }

    public boolean contains(T drawable) {
        return children.contains(drawable);
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!layoutValid.get()) updateLayout();

        super.onRender(context, mouseX, mouseY, delta);
        this.children.forEach(d -> d.render(context, mouseX, mouseY, delta));
    }

    @Override
    public void dispose() {
        super.dispose();
        children.forEach(MDrawable::dispose);
    }
}

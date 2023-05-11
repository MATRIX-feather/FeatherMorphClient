package xiamomc.morph.client.graphics;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Container extends MDrawable
{
    //region Padding

    @NotNull
    private Margin padding = new Margin();

    public Margin getPadding()
    {
        return padding;
    }

    public void setPadding(Margin padding)
    {
        padding = padding == null ? new Margin() : padding;
        this.padding = padding;
    }

    //endregion Padding

    //region Layout Validation

    @Override
    protected void invalidatePosition()
    {
        invalidateLayout();
        super.invalidatePosition();
    }

    private final AtomicBoolean layoutValid = new AtomicBoolean(false);

    private void updateLayout()
    {
        var childXOffset = 0;
        var childYOffset = 0;

        //todo: Padding中的Bottom和Right暂时没有作用，因为：
        //todo: Anchor & y3 == y3 时没有翻转y轴
        //todo: Anchor & x3 == x3 时没有翻转x轴

        var maskX = (anchor.posMask << 4) >> 4;
        if ((maskX & PosMask.x1) == PosMask.x1)
            childXOffset += padding.left;
        else if ((maskX & PosMask.x2) == PosMask.x2)
            childXOffset += padding.left - padding.right;
        else if ((maskX & PosMask.x3) == PosMask.x3)
            childXOffset += padding.left;

        var maskY = (anchor.posMask >> 4) << 4;
        if ((maskY & PosMask.y1) == PosMask.y1)
            childYOffset += padding.top;
        else if ((maskY & PosMask.y2) == PosMask.y2)
            childYOffset += padding.top - padding.bottom;
        else if ((maskY & PosMask.y3) == PosMask.y3)
            childYOffset += padding.top;

        for (MDrawable child : children)
        {
            child.applyParentX(childXOffset);
            child.applyParentY(childYOffset);
        }

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
        children.remove(drawable);

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
    protected void onRender(MatrixStack matrixStack, int mouseX, int mouseY, float delta)
    {
        if (!layoutValid.get()) updateLayout();

        super.onRender(matrixStack, mouseX, mouseY, delta);
        this.children.forEach(d -> d.render(matrixStack, mouseX, mouseY, delta));
    }

    @Override
    public void dispose()
    {
        super.dispose();
        children.forEach(MDrawable::dispose);
    }
}

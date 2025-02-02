package xyz.nifeather.morph.client.screens.disguise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.FeatherMorphClient;
import xyz.nifeather.morph.client.graphics.IMDrawable;
import xyz.nifeather.morph.client.graphics.MarginPadding;
import xyz.nifeather.morph.client.graphics.transforms.Recorder;
import xyz.nifeather.morph.client.graphics.transforms.Transformer;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;

import java.util.List;

public class DisguiseList extends ElementListWidget<EntityDisplayEntry> implements IMDrawable
{
    public DisguiseList(MinecraftClient minecraftClient, int width, int height, int topPadding, int bottomPadding, int itemHeight)
    {
        super(minecraftClient, width, height, 0, itemHeight);
    }

    public void addChild(EntityDisplayEntry entry)
    {
        entry.updateParentAllowedScreenSpaceWidth(this.getRowWidth());
        this.children().add(entry);
    }

    public void addChildrenRange(List<EntityDisplayEntry> entry)
    {
        entry.forEach(this::addChild);
    }

    public void clearChildren()
    {
        this.clearChildren(true);
    }

    public void clearChildren(boolean disposeChildren)
    {
        if (disposeChildren)
            children().forEach(EntityDisplayEntry::clearChildren);

        clearEntries();
    }

    private boolean smoothScroll()
    {
        return FeatherMorphClient.getInstance().getModConfigData().disguiseListSmoothScroll;
    }

    @Override
    public void setFocused(boolean focused)
    {
        super.setFocused(focused);
    }

    public void setHeight(int nH)
    {
        this.height = nH;
    }

    public void setWidth(int w)
    {
        this.width = w;
        var rowWidth = this.getRowWidth();

        this.children().forEach(entry -> entry.updateParentAllowedScreenSpaceWidth(rowWidth));
    }

    public void scrollTo(EntityDisplayEntry widget)
    {
        if (widget == null || !children().contains(widget)) return;

        var amount = children().indexOf(widget) * itemHeight - itemHeight * 4;
        var maxScroll = this.getMaxScrollY();
        if (amount > maxScroll) amount = maxScroll;

        this.setScrollY(amount);
    }

    private long duration = 300;

    @Override
    public void setScrollY(double targetAmount)
    {
        super.setScrollY(targetAmount);

        targetAmount = MathHelper.clamp(targetAmount, 0, getMaxScrollY());

        if (smoothScroll() && !noTransform)
            Transformer.transform(this.scrollAmount, targetAmount, duration, Easing.OutQuint);
        else
            this.scrollAmount.set(targetAmount);
    }

    private final Recorder<Double> scrollAmount = new Recorder<>(0D);

    private boolean noTransform = false;

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY)
    {
        duration = 125;

        this.noTransform = true;
        var result = super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        this.noTransform = false;

        return result;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        duration = 300;

        verticalAmount *= 3f * FeatherMorphClient.getInstance().getModConfigData().scrollSpeed;
        horizontalAmount *= 3f * FeatherMorphClient.getInstance().getModConfigData().scrollSpeed;

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public double getScrollY()
    {
        return (!noTransform || smoothScroll())
               ? this.scrollAmount.get()
               : super.getScrollY();
    }

    @Override
    protected int getScrollbarThumbY()
    {
        return Math.max(this.getY(), Math.round((float)this.getScrollY() * (this.height - this.getScrollbarThumbHeight()) / this.getMaxScrollY() + this.getY()));
        //return super.getScrollbarThumbY();
    }

    @Override
    protected void drawHeaderAndFooterSeparators(DrawContext context)
    {
    }

    //private double diff;

    @Override
    public int getRowWidth()
    {
        return Math.round(this.getWidth() * 0.7f);
    }

    public void setHeaderHeight(int newHeaderHeight)
    {
        this.headerHeight = newHeaderHeight;
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
        return this.width;
    }

    @Override
    public float getRenderHeight()
    {
        return this.height;
    }

    @Override
    public @NotNull MarginPadding getPadding()
    {
        return new MarginPadding(0);
    }

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

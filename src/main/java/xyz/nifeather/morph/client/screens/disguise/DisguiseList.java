package xyz.nifeather.morph.client.screens.disguise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.util.math.MathHelper;
import xyz.nifeather.morph.client.MorphClient;
import xyz.nifeather.morph.client.graphics.IMDrawable;
import xyz.nifeather.morph.client.graphics.transforms.Recorder;
import xyz.nifeather.morph.client.graphics.transforms.Transformer;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;

public class DisguiseList extends ElementListWidget<EntityDisplayEntry> implements IMDrawable
{
    public DisguiseList(MinecraftClient minecraftClient, int width, int height, int topPadding, int bottomPadding, int itemHeight)
    {
        super(minecraftClient, width, height, 0, itemHeight);
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
        return MorphClient.getInstance().getModConfigData().disguiseListSmoothScroll;
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
    }

    public void scrollTo(EntityDisplayEntry widget)
    {
        if (widget == null || !children().contains(widget)) return;

        //top和bottom此时可能正处于动画中，因此需要我们自己确定最终屏幕的可用空间大小
        //在界面Header和Footer替换成我们自己的实现之前先这样
        var fontMargin = 4;
        var topPadding = MinecraftClient.getInstance().textRenderer.fontHeight * 2 + fontMargin * 2;
        var bottomPadding = 30;
        var finalScreenSpaceHeight = this.height - topPadding - bottomPadding;

        var amount = children().indexOf(widget) * itemHeight - itemHeight * 4;
        var maxScroll = this.getEntryCount() * this.itemHeight - finalScreenSpaceHeight + 4;
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

        verticalAmount *= 3f * MorphClient.getInstance().getModConfigData().scrollSpeed;
        horizontalAmount *= 3f * MorphClient.getInstance().getModConfigData().scrollSpeed;

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

    //private double diff;

    @Override
    public int getRowWidth()
    {
        return 200;
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

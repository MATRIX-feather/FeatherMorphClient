package xiamomc.morph.client.screens.disguise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.MathHelper;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.IMDrawable;
import xiamomc.morph.client.graphics.color.ColorUtils;
import xiamomc.morph.client.graphics.color.Colors;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;

import java.awt.*;

public class DisguiseList extends ElementListWidget<EntityDisplayWidget> implements IMDrawable
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
            children().forEach(EntityDisplayWidget::clearChildren);

        clearEntries();
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

    public void scrollTo(EntityDisplayWidget widget)
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

        this.setScrollAmount(amount);
    }

    private long duration = 525;

    @Override
    public void setScrollAmount(double targetAmount)
    {
        targetAmount = MathHelper.clamp(targetAmount, 0, getMaxScroll());

        //this.diff = targetAmount - this.scrollAmount.get();
        this.targetAmount = targetAmount;

        Transformer.transform(this.scrollAmount, targetAmount, duration, Easing.OutExpo);
    }

    private final Recorder<Double> scrollAmount = new Recorder<>(0D);

    private boolean returnEasing = true;

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY)
    {
        duration = 125;

        this.returnEasing = false;
        var result = super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        this.returnEasing = true;

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
    public double getScrollAmount()
    {
        return returnEasing ? this.scrollAmount.get() : targetAmount;
    }

    @Override
    protected boolean isSelectButton(int button) {
        return true;
    }

    //private double diff;

    private double targetAmount;

    @Override
    public int getRowWidth()
    {
        return 200;
    }

    public void setHeaderHeight(int newHeaderHeight)
    {
        this.headerHeight = newHeaderHeight;
        this.setRenderHeader(true, newHeaderHeight);
    }
}

package xiamomc.morph.client.screens.disguise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.morph.client.utilties.TransformUtils;
import xiamomc.pluginbase.Bindables.Bindable;

public class DisguiseList extends ElementListWidget<EntityDisplayWidget>
{
    private final Bindable<Double> scrollAmount = new Bindable<>(0d);

    public DisguiseList(MinecraftClient minecraftClient, int width, int height, int topPadding, int bottomPadding, int itemHeight)
    {
        super(minecraftClient, width, height, topPadding, bottomPadding, itemHeight);

        this.setRenderBackground(false);
    }

    public void clearChildren()
    {
        children().forEach(EntityDisplayWidget::clearChildren);
        clearEntries();
    }

    public void setHeight(int nH)
    {
        this.height = nH;
    }

    public void setWidth(int w)
    {
        this.width = w;
    }

    public void setBottomPadding(int b)
    {
        this.bottom = b;
    }

    public int getBottomPadding()
    {
        return this.bottom;
    }

    public int getTopPadding()
    {
        return this.top;
    }

    public void setTopPadding(int newPadding)
    {
        this.top = newPadding;
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

        var amount = children().indexOf(widget) * itemHeight - itemHeight * 3;
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

        Transformer.transformBindable(this.scrollAmount, targetAmount, duration, Easing.OutExpo);
    }

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
    public boolean mouseScrolled(double mouseX, double mouseY, double amount)
    {
        duration = 300;

        amount *= 3.5f;
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public double getScrollAmount()
    {
        return returnEasing ? this.scrollAmount.get() : targetAmount;
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
        this.setRenderHeader(true, newHeaderHeight);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        /*
        var textRenderer = MinecraftClient.getInstance().textRenderer;

        var textY = this.getTopPadding() + textRenderer.fontHeight;
        textRenderer.drawWithShadow(matrices, "Current: %s".formatted(this.scrollAmount.get()), 0, textY, 0xffffff);

        textY += textRenderer.fontHeight;
        textRenderer.drawWithShadow(matrices, "Target: %s".formatted(targetAmount), 0, textY, 0xffffff);

        textY += textRenderer.fontHeight;
        textRenderer.drawWithShadow(matrices, "Diff: %s".formatted(diff), 0, textY, 0xffffff);
        */

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBackground(MatrixStack matrices)
    {
    }
}

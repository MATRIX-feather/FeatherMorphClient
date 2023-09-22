package xiamomc.morph.client.screens.disguise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.MathHelper;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.IMDrawable;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;

public class DisguiseList extends ElementListWidget<EntityDisplayWidget> implements IMDrawable
{
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

    @Override
    protected void renderHeader(DrawContext context, int x, int y)
    {
        super.renderHeader(context, x, y);
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
        this.setRenderHeader(true, newHeaderHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
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

        context.setShaderColor(0.2F, 0.2F, 0.2F, 1.0F);

        context.drawTexture(Screen.OPTIONS_BACKGROUND_TEXTURE,
                0, 0,
                (float)this.right, -getTopPadding(),
                this.right - this.left, this.getTopPadding(), 32, 32);

        context.drawTexture(Screen.OPTIONS_BACKGROUND_TEXTURE,
                0, this.bottom,
                (float)this.right, 0,
                this.right - this.left, this.getBottomPadding(), 32, 32);

        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        super.render(context, mouseX, mouseY, delta);

        context.fillGradient(RenderLayer.getGuiOverlay(), this.left, this.top, this.right, this.top + 4, -16777216, 0, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), this.left, this.bottom - 4, this.right, this.bottom, 0, -16777216, 0);
    }
}

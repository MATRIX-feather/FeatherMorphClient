package xiamomc.morph.client.screens.disguise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import xiamomc.morph.client.MorphClient;

public class DisguiseList extends ElementListWidget<LivingEntityDisguiseWidget>
{
    public DisguiseList(MinecraftClient minecraftClient, int width, int height, int topPadding, int bottomPadding, int itemHeight)
    {
        super(minecraftClient, width, height, topPadding, bottomPadding, itemHeight);
    }

    public void clearChildren()
    {
        children().forEach(LivingEntityDisguiseWidget::clearChildren);
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

    public void scrollTo(LivingEntityDisguiseWidget widget)
    {
        if (widget == null || !children().contains(widget)) return;

        var amount = children().indexOf(widget) * itemHeight - itemHeight * 3;
        var maxScroll = this.getMaxScroll();
        if (amount > maxScroll) amount = maxScroll;

        this.setScrollAmount(amount);
    }

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
        this.setRenderBackground(false);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBackground(MatrixStack matrices)
    {
    }
}

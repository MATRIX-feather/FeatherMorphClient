package xiamo.morph.client.screens.disguise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;

public class IdentifierDrawableList extends ElementListWidget<StringWidget>
{
    public IdentifierDrawableList(MinecraftClient minecraftClient, int i, int j, int k, int l, int m)
    {
        super(minecraftClient, i, j, k, l, m);
    }

    public void setHeight(int nH)
    {
        this.height = nH;
    }

    public void setWidth(int w)
    {
        this.width = w;
    }

    public void setBottom(int b)
    {
        this.bottom = b;
    }

    public void scrollTo(StringWidget widget)
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

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        this.setRenderBackground(false);
        this.setRenderHeader(false, 0);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBackground(MatrixStack matrices)
    {
    }
}

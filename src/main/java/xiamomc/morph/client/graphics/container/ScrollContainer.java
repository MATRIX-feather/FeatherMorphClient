package xiamomc.morph.client.graphics.container;

import net.minecraft.client.gui.DrawContext;
import xiamomc.morph.client.graphics.MDrawable;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;

public class ScrollContainer extends BasicContainer<MDrawable>
{
    private float spacing = 0f;

    public void setSpacing(float newSpacing)
    {
        spacing = newSpacing;
        invalidateLayout();
    }

    public float getSpacing()
    {
        return spacing;
    }

    private final Recorder<Double> scrollAmount = new Recorder<>(0d);

    public void scrollTo(double amount)
    {
        Transformer.transform(scrollAmount, amount, 300, Easing.OutQuint);
    }

    public void scrollTo(MDrawable drawable)
    {
        if (!this.contains(drawable)) return;
        var index = children.indexOf(drawable);

        double amount = 0d;
        for (int i = 0; i < index; i++)
        {
            amount += children.get(i).getHeight();

            if (i != index - 1) amount += spacing;
        }

        this.scrollTo(amount);
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        var matrices = context.getMatrices();

        super.onRender(context, mouseX, mouseY, delta);
    }

    @Override
    protected void updateLayout()
    {
        super.updateLayout();
    }
}

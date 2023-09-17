package xiamomc.morph.client.graphics.container;

import net.minecraft.client.gui.DrawContext;
import xiamomc.morph.client.graphics.*;
import xiamomc.pluginbase.Annotations.Initializer;

public class BasicFlowContainer<T extends MDrawable> extends BasicContainer<T>
{
    private int spacing = 0;

    public int getSpacing()
    {
        return spacing;
    }

    public void setSpacing(int newVal)
    {
        if (newVal == spacing) return;

        spacing = newVal;
        invalidateLayout();
    }

    private Axes flowAxes = Axes.Both;

    public Axes getFlowAxes()
    {
        return flowAxes;
    }

    public void setFlowAxes(Axes val)
    {
        if (val == this.flowAxes) return;
        if (val == Axes.None || val == null) val = Axes.Both;

        this.flowAxes = val;
        invalidateLayout();
    }

    public BasicFlowContainer()
    {
    }

    @Initializer
    private void load()
    {
    }

    @Override
    protected void updateLayout()
    {
        var currentX = 0;
        var currentY = 0;

        var childMaxHeight = 0f;

        for (var child : children)
        {
            if (child.getAnchor() != Anchor.TopLeft)
                throw new IllegalArgumentException("Anchors except TopLeft are not supported yet.");

            // Both下的自动换行
            if (flowAxes == Axes.Both)
            {
                var maxWidth = this.width - this.padding.left - this.padding.right;
                if (currentX + child.getWidth() > maxWidth)
                {
                    currentY += childMaxHeight + spacing;
                    currentX = 0;
                    childMaxHeight = 0;
                }
            }

            // 设置位置
            child.setX(currentX);
            child.setY(currentY);

            // 更新XY
            switch (flowAxes)
            {
                case Both ->
                {
                    currentX += Math.max(0, child.getWidth()) + spacing;
                    childMaxHeight = Math.max(childMaxHeight, child.getHeight());
                }

                case X -> currentX += Math.max(0, child.getWidth()) + spacing;

                case Y -> currentY += Math.max(0, child.getHeight()) + spacing;
            }
        }

        super.updateLayout();
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.onRender(context, mouseX, mouseY, delta);

        context.fill(0, 0, (int)width, (int)height, 0xa0333333);
    }
}

package xiamomc.morph.client.graphics;

import net.minecraft.client.gui.DrawContext;
import xiamomc.morph.client.graphics.color.Colors;

public class Box extends MDrawable
{
    public int color = Colors.WHITE.getColor();

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.onRender(context, mouseX, mouseY, delta);

        context.fill(0, 0,
                renderWidth, renderHeight,
                color);
    }
}

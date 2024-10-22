package xyz.nifeather.morph.client.screens.spinner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;
import xyz.nifeather.morph.client.graphics.Axes;
import xyz.nifeather.morph.client.graphics.Box;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xyz.nifeather.morph.client.graphics.container.Container;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;

public class ClickableSpinnerWidget extends Container
{
    private final Box hover = new Box();
    private final Box background = new Box();

    public ClickableSpinnerWidget()
    {
        hover.setRelativeSizeAxes(Axes.Both);
        hover.setAlpha(0);
        hover.color = ColorUtils.fromHex("#000000").getColor();
        hover.setDepth(-1);

        background.setRelativeSizeAxes(Axes.Both);
        background.setDepth(1);
        background.color = ColorUtils.forOpacity(ColorUtils.fromHex("#333333"), 0.4f).getColor();

        this.add(background);
        this.add(hover);
    }

    private final int borderColor = ColorUtils.fromHex("#aaaaaa").getColor();

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.onRender(context, mouseX, mouseY, delta);

        // Draw border

        context.getMatrices().translate(0, 0, 100);

        context.drawBorder(0, 0, (int)this.width, (int)this.height, borderColor);

        context.getMatrices().translate(0, 0, -100);

        // End draw border
    }

    private Runnable onClick;

    public void onClick(Runnable runnable)
    {
        this.onClick = runnable;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (!hovered() && !isFocused()) return false;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
        {
            if (this.onClick != null)
                this.onClick.run();

            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void onHover()
    {
        super.onHover();

        hover.fadeTo(0.5f, 300, Easing.OutQuint);
    }

    @Override
    protected void onHoverLost()
    {
        super.onHoverLost();

        hover.fadeOut(300, Easing.OutQuint);
    }
}

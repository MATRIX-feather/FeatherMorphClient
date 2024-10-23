package xyz.nifeather.morph.client.screens.spinner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import xyz.nifeather.morph.client.graphics.Axes;
import xyz.nifeather.morph.client.graphics.DrawableSprite;
import xyz.nifeather.morph.client.graphics.container.Container;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;

public class ClickableSpinnerWidget extends Container
{
    protected final DrawableSprite spriteBackground;
    protected final DrawableSprite spriteBorder;
    protected final DrawableSprite spriteHover;

    protected Identifier getPathOf(String variant)
    {
        return Identifier.of("morphclient", "spinner_default/" + variant);
    }

    protected long getFadeDuration()
    {
        return 300;
    }

    protected DrawableSprite createDrawableSprite(Identifier textureIdentifier)
    {
        var drawableSprite = new DrawableSprite(textureIdentifier);

        drawableSprite.setRelativeSizeAxes(Axes.Both);

        return drawableSprite;
    }

    public ClickableSpinnerWidget()
    {
        spriteBackground = createDrawableSprite(getPathOf("background"));
        spriteBorder = createDrawableSprite(getPathOf("border"));
        spriteHover = createDrawableSprite(getPathOf("hover"));

        spriteHover.setAlpha(0);
        spriteHover.setDepth(-1);

        spriteBorder.setDepth(-100);

        spriteBackground.setDepth(10);

        this.add(spriteBackground);
        this.add(spriteBorder);
        this.add(spriteHover);
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

        spriteHover.fadeTo(0.5f, getFadeDuration(), Easing.OutQuint);
    }

    @Override
    protected void onHoverLost()
    {
        super.onHoverLost();

        spriteHover.fadeOut(getFadeDuration(), Easing.OutQuint);
    }
}

package xiamomc.morph.client.screens.emote;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import xiamomc.morph.client.ServerHandler;
import xiamomc.morph.client.graphics.*;
import xiamomc.morph.client.graphics.color.ColorUtils;
import xiamomc.morph.client.graphics.color.Colors;
import xiamomc.morph.client.graphics.color.MaterialColors;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.morph.network.commands.C2S.C2SAnimationCommand;
import xiamomc.pluginbase.Annotations.Resolved;

public class SingleEmoteWidget extends MDrawable
{
    private final Box hover = new Box();

    public SingleEmoteWidget()
    {
        hover.setParent(this);
        hover.setRelativeSizeAxes(Axes.Both);
        hover.setAlpha(0);

        hover.color = ColorUtils.fromHex("#000000").getColor();
        setText(Text.translatable("gui.none"));
    }

    private final int fillColor = ColorUtils.forOpacity(ColorUtils.fromHex("#333333"), 0.4f).getColor();
    private final int borderColor = ColorUtils.fromHex("#aaaaaa").getColor();

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.onRender(context, mouseX, mouseY, delta);

        context.fill(0, 0, Math.round(this.getFinalWidth()), Math.round(this.getFinalHeight()), fillColor);

        hover.render(context, mouseX, mouseY, delta);
        title.render(context, mouseX, mouseY, delta);

        context.getMatrices().translate(0, 0, 1);

        context.drawBorder(0, 0, (int)this.width, (int)this.height, borderColor);

        context.getMatrices().translate(0, 0, -1);
    }

    private final DrawableText title = new DrawableText();

    public void setText(Text text)
    {
        title.setAnchor(Anchor.Centre);
        title.setText(text);
        title.setParent(this);
    }

    private String emote;

    public void setEmote(String identifier)
    {
        this.emote = identifier;
        this.setText(Text.translatable("emote.morphclient." + identifier));
    }

    @Resolved(shouldSolveImmediately = true)
    private ServerHandler serverHandler;

    private Runnable onClick;

    public void onClick(Runnable runnable)
    {
        this.onClick = runnable;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (!hovered() && !isFocused()) return false;

        if (this.emote != null)
            serverHandler.sendCommand(new C2SAnimationCommand(this.emote));

        if (this.onClick != null)
            this.onClick.run();

        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void onHover()
    {
        super.onHover();

        hover.fadeTo(0.3f, 300, Easing.OutQuint);
    }

    @Override
    protected void onHoverLost()
    {
        super.onHoverLost();

        hover.fadeOut(300, Easing.OutQuint);
    }
}

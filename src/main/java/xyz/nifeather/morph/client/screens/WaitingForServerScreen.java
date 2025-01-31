package xyz.nifeather.morph.client.screens;

import me.shedaniel.math.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.FeatherMorphClient;
import xyz.nifeather.morph.client.graphics.*;
import xyz.nifeather.morph.client.graphics.transforms.Transformer;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;
import xyz.nifeather.morph.client.screens.disguise.DisguiseScreen;
import xiamomc.pluginbase.Bindables.Bindable;

public class WaitingForServerScreen extends FeatherScreen
{
    @Nullable
    private final FeatherScreen nextScreen;

    public WaitingForServerScreen(@NotNull FeatherScreen next)
    {
        this(Text.empty(), next);
    }

    protected WaitingForServerScreen(Text title, @NotNull FeatherScreen next)
    {
        super(title);

        this.nextScreen = next;
        closeButton = this.buildButtonWidget(0, 0, 150, 20, Text.translatable("gui.back"), (button) ->
        {
            this.close();
        });
    }

    private final DrawableText notReadyText = new DrawableText(Text.translatable("gui.morphclient.waiting_for_server"));
    private final MButtonWidget closeButton;

    private final Bindable<Float> backgroundDim = new Bindable<>(0f);

    public float getCurrentDim()
    {
        return backgroundDim.get();
    }

    private final Bindable<Boolean> serverReady = new Bindable<>();

    private final LoadingSpinner loadingSpinner = new LoadingSpinner();

    @Override
    protected void onScreenEnter(Screen last)
    {
        super.onScreenEnter(last);

        var morphClient = FeatherMorphClient.getInstance();
        this.serverReady.bindTo(morphClient.serverHandler.serverReady);

        if (serverReady.get())
        {
            this.push(nextScreen);
        }
        else
        {
            serverReady.onValueChanged((o, n) ->
            {
                FeatherMorphClient.getInstance().schedule(() ->
                {
                    if (isCurrent() && n)
                        this.push(nextScreen);
                });
            }, true);

            this.addRange(new IMDrawable[]
            {
                notReadyText,
                closeButton,
                loadingSpinner
            });

            loadingSpinner.setAnchor(Anchor.Centre);
            loadingSpinner.setY(40);
            notReadyText.setAnchor(Anchor.Centre);

            if (last instanceof DisguiseScreen disguiseScreen)
                backgroundDim.set(disguiseScreen.getBackgroundDim());

            Transformer.transform(backgroundDim, 0.3f, 300, Easing.OutQuint);
        }
    }

    @Override
    protected void onScreenResize()
    {
        loadingSpinner.invalidatePosition();
        notReadyText.invalidatePosition();
        super.onScreenResize();
    }

    @Override
    protected void onScreenExit(@Nullable Screen nextScreen)
    {
        serverReady.dispose();

        super.onScreenExit(nextScreen);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        var color = Color.ofRGBA(0, 0, 0, backgroundDim.get());
        context.fillGradient(0, 0, this.width, this.height, color.getColor(), color.getColor());

        //notReadyText.setScreenY(this.height / 2);
        //notReadyText.setScreenX((this.width -textRenderer.getWidth(notReadyText.getText()))  / 2);

        closeButton.setX(this.width / 2 - 75);
        closeButton.setY(this.height - 29);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderInGameBackground(DrawContext context)
    {
    }

    @Override
    protected void renderDarkening(DrawContext context)
    {
    }

    @Override
    protected void renderDarkening(DrawContext context, int x, int y, int width, int height)
    {
    }
}

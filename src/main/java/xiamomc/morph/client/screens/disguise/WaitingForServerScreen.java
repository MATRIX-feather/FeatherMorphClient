package xiamomc.morph.client.screens.disguise;

import me.shedaniel.math.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.*;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.morph.client.screens.FeatherScreen;
import xiamomc.pluginbase.Bindables.Bindable;

public class WaitingForServerScreen extends FeatherScreen
{
    public WaitingForServerScreen()
    {
        this(Text.empty());
    }

    protected WaitingForServerScreen(Text title)
    {
        super(title);

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
    protected void onScreenEnter(FeatherScreen last)
    {
        super.onScreenEnter(last);

        var morphClient = MorphClient.getInstance();
        this.serverReady.bindTo(morphClient.serverHandler.serverReady);

        if (serverReady.get())
        {
            this.push(new DisguiseScreen());
        }
        else
        {
            serverReady.onValueChanged((o, n) ->
            {
                MorphClient.getInstance().schedule(() ->
                {
                    if (isCurrent() && n)
                        this.push(new DisguiseScreen());
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

            Transformer.transform(backgroundDim, 0.5f, 300, Easing.OutQuint);
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
    protected void onScreenExit(@Nullable FeatherScreen nextScreen)
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
}

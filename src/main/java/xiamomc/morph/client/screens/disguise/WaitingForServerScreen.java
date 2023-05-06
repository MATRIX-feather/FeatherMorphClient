package xiamomc.morph.client.screens.disguise;

import me.shedaniel.math.Color;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.DrawableText;
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

        closeButton = this.buildWidget(0, 0, 150, 20, Text.translatable("gui.back"), (button) ->
        {
            this.close();
        });
    }

    private final DrawableText notReadyText = new DrawableText(Text.translatable("gui.morphclient.waiting_for_server"));
    private final ButtonWidget closeButton;

    private final Bindable<Float> backgroundDim = new Bindable<>(0f);

    public float getCurrentDim()
    {
        return backgroundDim.get();
    }

    @Override
    protected void onScreenEnter(FeatherScreen last)
    {
        super.onScreenEnter(last);

        var morphClient = MorphClient.getInstance();
        var serverReady = morphClient.serverHandler.serverReady;

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

            this.addDrawable(notReadyText);
            this.addDrawableChild(closeButton);

            if (last instanceof DisguiseScreen disguiseScreen)
                backgroundDim.set(disguiseScreen.getBackgroundDim());

            Transformer.transform(backgroundDim, 0.5f, 300, Easing.OutQuint);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        notReadyText.setScreenY(this.height / 2);
        notReadyText.setScreenX((this.width -textRenderer.getWidth(notReadyText.getText()))  / 2);

        closeButton.setX(this.width / 2 - 75);
        closeButton.setY(this.height - 29);

        var color = Color.ofRGBA(0, 0, 0, backgroundDim.get());
        this.fillGradient(matrices, 0, 0, this.width, this.height, color.getColor(), color.getColor());
        super.render(matrices, mouseX, mouseY, delta);
    }
}

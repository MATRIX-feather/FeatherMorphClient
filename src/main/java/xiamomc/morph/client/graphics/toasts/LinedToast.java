package xiamomc.morph.client.graphics.toasts;

import me.shedaniel.math.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.MorphClientObject;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.concurrent.atomic.AtomicBoolean;

public class LinedToast extends MorphClientObject implements Toast
{
    public LinedToast()
    {
    }

    private final Recorder<Integer> outlineWidth = Recorder.of(0);

    protected final Bindable<Visibility> visibility = new Bindable<Toast.Visibility>(Visibility.HIDE);

    @Initializer
    private void load()
    {
        outlineWidth.set(this.getWidth());

        this.visibility.onValueChanged((o, visible) ->
        {
            if (visible == Visibility.SHOW)
                Transformer.delay(250).then(() -> Transformer.transform(outlineWidth, 2, 600, Easing.OutQuint));
            else
                Transformer.transform(outlineWidth, this.getWidth(), 600, Easing.OutQuad);
        }, true);
    }

    private final AtomicBoolean layoutValid = new AtomicBoolean(false);

    protected void invalidateLayout()
    {
        layoutValid.set(false);
    }

    protected void updateLayout()
    {
        if (title != null)
        {
            Text titleDisplay = Text.literal(textRenderer.trimToWidth(title, this.getTextWidth()).getString());

            if (!titleDisplay.getString().equalsIgnoreCase(title.getString()))
                titleDisplay = Text.of(titleDisplay.getString() + "...");

            this.titleDisplay = titleDisplay;
        }
        else
            this.titleDisplay = Text.literal("Null title");

        if (description != null)
        {
            Text descDisplay = Text.literal(textRenderer.trimToWidth(description, this.getTextWidth()).getString());

            if (!descDisplay.getString().equalsIgnoreCase(description.getString()))
                descDisplay = Text.of(descDisplay.getString() + "...");

            this.descDisplay = descDisplay;
        }
        else
            this.descDisplay = Text.literal("");

        layoutValid.set(true);
    }

    public void setTitle(Text text)
    {
        this.title = text;
        this.invalidateLayout();
    }

    @Nullable
    public Text getTitle()
    {
        return title;
    }

    public void setDescription(Text text)
    {
        this.description = text;
        this.invalidateLayout();
    }

    @Nullable
    public Text getDescription()
    {
        return description;
    }

    private static final Text defaultText = Text.empty();

    private Text title;
    private Text description;
    private Text titleDisplay = defaultText;
    private Text descDisplay = defaultText;
    private Color lineColor = Color.ofRGB(255, 255, 255);

    @NotNull
    public Color getLineColor()
    {
        return lineColor;
    }

    public void setLineColor(@Nullable Color newColor)
    {
        if (newColor == null) newColor = Color.ofRGB(255, 255, 255);
        this.lineColor = newColor;
    }

    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    protected void postTextDrawing(DrawContext context, ToastManager manager, long startTime)
    {
    }

    protected void postBackgroundDrawing(DrawContext context, ToastManager manager, long startTime)
    {
    }

    protected void postDraw(DrawContext context, ToastManager manager, long startTime)
    {
    }

    protected boolean drawProgress()
    {
        return MorphClient.getInstance().getModConfigData().displayToastProgress;
    }

    protected float getTextStartX()
    {
        return this.getWidth() * 0.25F - 4;
    }

    protected int getTextWidth()
    {
        return (int) (this.getWidth() * 0.65F);
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime)
    {
        if (!layoutValid.get())
            updateLayout();

        // Draw background
        context.fill(0, 0, this.getWidth(), this.getHeight(), 0xFF333333);

        var progress = Math.min(1, startTime / (5000.0 * manager.getNotificationDisplayTimeMultiplier()));

        // Draw progress bar
        if (drawProgress())
        {
            var progressDisplay = Math.max(0, 0.95 - progress);

            context.fill(0, 0, (int)(this.getWidth() * progressDisplay), this.getHeight(), (int)(0x40 * progressDisplay) << 24 | 0x00FFFFFF);
        }

        postBackgroundDrawing(context, manager, startTime);

        // Draw text
        var textStartX = (int)getTextStartX();
        var textStartY = this.getHeight() / 2 - textRenderer.fontHeight;

        var matrices = context.getMatrices();

        // Always draw texts on the top
        matrices.push();
        matrices.translate(0, 0, 128);

        context.drawTextWithShadow(textRenderer, titleDisplay, textStartX, textStartY - 1, 0xffffffff);
        context.drawTextWithShadow(textRenderer, descDisplay, textStartX, textStartY + textRenderer.fontHeight + 1, 0xffffffff);
        matrices.pop();

        postTextDrawing(context, manager, startTime);

        // Draw CoverLine
        var lineWidth = outlineWidth.get();
        context.fill(0, 0, lineWidth, this.getHeight(), lineColor.getColor());

        //var borderColor = ColorUtils.fromHex("#222222");
        //DrawableHelper.drawBorder(matrices, 0, 0, this.getWidth(), this.getHeight(), borderColor.getColor());

        // Update visibility
        var visibility = progress >= 1 ? Visibility.HIDE : Visibility.SHOW;
        this.visibility.set(visibility);

        postDraw(context, manager, startTime);

        return visibility;
    }
}

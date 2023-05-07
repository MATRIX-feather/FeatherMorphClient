package xiamomc.morph.client.graphics.toasts;

import me.shedaniel.math.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.MorphClientObject;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;

public class LinedToast extends MorphClientObject implements Toast
{
    public LinedToast()
    {
    }

    private final Recorder<Integer> outlineWidth = Recorder.of(this.getWidth());

    @Initializer
    private void load()
    {
        visibility.onValueChanged((o, visible) ->
        {
            if (visible == Visibility.SHOW)
                Transformer.delay(250).then(() -> Transformer.transform(outlineWidth, 2, 600, Easing.OutQuint));
            else
                Transformer.transform(outlineWidth, this.getWidth(), 600, Easing.OutQuad);
        }, true);
    }

    public Text title;
    public Text description;
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

    protected final Bindable<Visibility> visibility = new Bindable<>(Visibility.HIDE);

    protected void postTextDrawing(MatrixStack matrices, ToastManager manager, long startTime)
    {
    }

    protected void postBackgroundDrawing(MatrixStack matrices, ToastManager manager, long startTime)
    {
    }

    protected void postDraw(MatrixStack matrices, ToastManager manager, long startTime)
    {
    }

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime)
    {
        // Draw background
        DrawableHelper.fill(matrices, 0, 0, this.getWidth(), this.getHeight(), 0xFF333333);

        // Draw progress bar
        var progress = Math.min(1, startTime / (5000.0 * manager.getNotificationDisplayTimeMultiplier()));
        var progressDisplay = Math.max(0, 0.95 - progress);

        DrawableHelper.fill(matrices, 0, 0, (int)(this.getWidth() * progressDisplay), this.getHeight(), (int)(0x40 * progressDisplay) << 24 | 0x00FFFFFF);

        postBackgroundDrawing(matrices, manager, startTime);

        // Draw text
        var textStartX = this.getWidth() * 0.25F - 4;
        var textStartY = this.getHeight() / 2 - textRenderer.fontHeight;

        // Always draw texts on the top
        matrices.push();
        matrices.translate(0, 0, 128);
        textRenderer.drawWithShadow(matrices, title == null ? Text.literal("") : title, textStartX, textStartY - 1, 0xffffffff);
        textRenderer.drawWithShadow(matrices, description == null ? Text.literal("") : description, textStartX, textStartY + textRenderer.fontHeight + 1, 0xffffffff);
        matrices.pop();

        postTextDrawing(matrices, manager, startTime);

        // Draw CoverLine
        var lineWidth = outlineWidth.get();
        DrawableHelper.fill(matrices, 0, 0, lineWidth, this.getHeight(), lineColor.getColor());

        // Update visibility
        var visibility = progress >= 1 ? Visibility.HIDE : Visibility.SHOW;
        this.visibility.set(visibility);

        postDraw(matrices, manager, startTime);

        return visibility;
    }
}

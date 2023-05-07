package xiamomc.morph.client.graphics.toasts;

import me.shedaniel.math.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import xiamomc.morph.client.MorphClientObject;
import xiamomc.morph.client.graphics.EntityDisplay;
import xiamomc.morph.client.graphics.color.ColorUtils;
import xiamomc.morph.client.graphics.color.MaterialColors;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;

public class DisguiseEntryToast extends MorphClientObject implements Toast
{
    private final String rawIdentifier;

    private final boolean isGrant;

    public DisguiseEntryToast(String rawIdentifier, boolean isGrant)
    {
        if (isGrant) grantInstance = this;
        else lostInstance = this;

        this.rawIdentifier = rawIdentifier;
        this.isGrant = isGrant;

        this.entityDisplay = new EntityDisplay(rawIdentifier);
        entityDisplay.x = 512;
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

        entityDisplay.x = 20;
        entityDisplay.y = this.getHeight() / 2 + 7;

        if (rawIdentifier.equals("minecraft:horse"))
        {
            entityDisplay.x -= 1;
            entityDisplay.y += 2;
        }
    }

    private final EntityDisplay entityDisplay;

    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    private static DisguiseEntryToast grantInstance;
    private static DisguiseEntryToast lostInstance;

    private final Bindable<Visibility> visibility = new Bindable<>(Visibility.HIDE);

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime)
    {
        // Draw background
        DrawableHelper.fill(matrices, 0, 0, this.getWidth(), this.getHeight(), 0xFF333333);

        // Draw progress
        var progress = Math.min(1, startTime / (3000.0 * manager.getNotificationDisplayTimeMultiplier()));
        var progressDisplay = Math.max(0, 0.95 - progress);

        DrawableHelper.fill(matrices, 0, 0, (int)(this.getWidth() * progressDisplay), this.getHeight(), (int)(0x40 * progressDisplay) << 24 | 0x00FFFFFF);

        // Draw entity
        entityDisplay.render(matrices, -30, 0);

        // Draw text
        var textStartX = this.getWidth() * 0.25F - 4;
        var textStartY = this.getHeight() / 2 - textRenderer.fontHeight;

        textRenderer.drawWithShadow(matrices, Text.translatable("text.morphclient.toast.disguise_%s".formatted(isGrant ? "grant" : "lost")), textStartX, textStartY - 1, 0xffffffff);
        textRenderer.drawWithShadow(matrices, entityDisplay.getDisplayName(), textStartX, textStartY + textRenderer.fontHeight + 1, 0xffffffff);

        // Draw CoverLine
        var color = isGrant ? MaterialColors.Green500 : MaterialColors.Amber500;
        var lineWidth = outlineWidth.get();
        DrawableHelper.fill(matrices, 0, 0, lineWidth, this.getHeight(), color.getColor());

/*
        var visibility = this.isGrant
                ? (grantInstance == this ? Visibility.SHOW : Visibility.HIDE)
                : (lostInstance == this ? Visibility.SHOW : Visibility.HIDE);
*/

        // Update visibility
        var visibility = progress >= 1 ? Visibility.HIDE : Visibility.SHOW;
        this.visibility.set(visibility);

        return visibility;
    }
}

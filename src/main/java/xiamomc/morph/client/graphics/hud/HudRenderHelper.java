package xiamomc.morph.client.graphics.hud;

import me.shedaniel.math.Color;
import net.minecraft.client.gui.DrawContext;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.MorphClientObject;
import xiamomc.morph.client.graphics.color.ColorUtils;
import xiamomc.morph.client.graphics.color.MaterialColors;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;

public class HudRenderHelper extends MorphClientObject
{
    @Resolved
    private ClientMorphManager manager;

    private final Bindable<Float> preferredWidth = new Bindable<>(0f);
    private final Recorder<Float> widthRecorder = new Recorder<>(0f);
    private final Recorder<Float> progressHeightRecorder = new Recorder<>(0f);
    private final Recorder<Float> barHeightRecorder = new Recorder<>(0f);

    private final Recorder<Color> colorRecord = new Recorder<>(Color.ofOpaque(0));
    private final Bindable<Color> preferredColor = new Bindable<>(Color.ofOpaque(0));

    private final Bindable<Color> preferredBgColor = new Bindable<>(Color.ofOpaque(0));
    private final Recorder<Color> bgColorRecord = new Recorder<>(Color.ofOpaque(0));

    private final Bindable<Boolean> visible = new Bindable<>(false);

    @Initializer
    private void load()
    {
        preferredWidth.bindTo(manager.revealingValue);
        preferredColor.onValueChanged((o, n) ->
        {
            if (!o.equals(n))
                Transformer.transform(colorRecord, n, 1500, Easing.OutQuint);
        });

        preferredWidth.onValueChanged((o, n) ->
        {
            Transformer.transform(widthRecorder, n, 2000, Easing.OutQuint);
        });

        preferredBgColor.onValueChanged((o, n) ->
        {
            if (!o.equals(n))
                Transformer.transform(bgColorRecord, n, 1500, Easing.OutQuint);
        });

        visible.onValueChanged((o, n) ->
        {
            Transformer.transform(progressHeightRecorder, n ? -5f : 0f, 650, Easing.InOutQuint);
            Transformer.transform(barHeightRecorder, n ? -12f : 0f, 650, Easing.OutBack);
        });
    }

    public void onRender(DrawContext context, float tickDelta)
    {
        if (manager == null) return;

        var matrices = context.getMatrices();

        try
        {
            matrices.push();

            var rev = widthRecorder.get();
            visible.set(rev < 0.1f);

            var targetClr = (rev >= 20)
                    ? (rev >= 80 ? MaterialColors.Red600 : MaterialColors.Amber500)
                    : MaterialColors.Green400;

            preferredColor.set(targetClr);
            preferredBgColor.set(targetClr.darker(3f));

            //matrices.push();
            renderBar(context, tickDelta);
            //matrices.pop();
            //renderProgress(context, tickDelta);
        }
        finally
        {
            matrices.pop();
        }
    }

    public void renderBar(DrawContext context, float tickDelta)
    {
        var width = 10;
        var height = 35;
        var padding = 1;

        var windowHeight = context.getScaledWindowHeight();
        var matrices = context.getMatrices();

        // 先位移到屏幕外面
        matrices.translate(barHeightRecorder.get(), windowHeight, 0);

        // 然后再位移到屏幕里面
        matrices.translate(2, -height - 2, 0);

        context.drawBorder(0, 0, width, height, 0xFF000000);

        context.fill(padding, padding, width - padding, height - padding, bgColorRecord.get().getColor());

        var barStart = padding + Math.round((height - padding * 2) * (widthRecorder.get() / 100));
        var barEnd = height - padding;
        context.fill(padding, barStart, width - padding, barEnd, colorRecord.get().getColor());
    }

    public void renderProgress(DrawContext context, float tickDelta)
    {
        var width = context.getScaledWindowWidth();

        var height = 2;
        var matrices = context.getMatrices();

        //logger.info("H " + heightRecorder.get());
        matrices.translate(0, progressHeightRecorder.get(), 0);

        //var width = context.getScaledWindowWidth();
        var scale = width - Math.round(width * (widthRecorder.get() / 100));
        // var scale30 = Math.round(width * 0.2f);
        // var scale80 = Math.round(width * 0.8f);

        // Base W
        context.fill(0, 0, context.getScaledWindowWidth(), height, ColorUtils.forOpacity(bgColorRecord.get(), 0.6f).getColor());

        // Progress
        context.fill(0, 0, scale, height, colorRecord.get().getColor());
    }
}

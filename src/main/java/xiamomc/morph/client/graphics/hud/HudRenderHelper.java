package xiamomc.morph.client.graphics.hud;

import com.mojang.blaze3d.systems.RenderSystem;
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

import java.util.Random;

public class HudRenderHelper extends MorphClientObject
{
    @Resolved
    private ClientMorphManager manager;

    private final Bindable<Float> revValueNext = new Bindable<>(0f);
    private final Recorder<Float> revDisplayRecorder = new Recorder<>(0f);
    private final Recorder<Float> progressHeightRecorder = new Recorder<>(0f);
    private final Recorder<Float> barHeightRecorder = new Recorder<>(0f);

    private final Recorder<Color> colorRecord = new Recorder<>(Color.ofOpaque(0));
    private final Bindable<Color> colorNext = new Bindable<>(Color.ofOpaque(0));

    private final Recorder<Float> drawAlpha = new Recorder<>(0f);

    private final Bindable<Color> preferredBgColor = new Bindable<>(Color.ofOpaque(0));
    private final Recorder<Color> bgColorRecord = new Recorder<>(Color.ofOpaque(0));

    private final Bindable<Boolean> visible = new Bindable<>(false);

    @Initializer
    private void load()
    {
        revValueNext.bindTo(manager.revealingValue);

        colorNext.onValueChanged((o, n) ->
        {
            if (o == null) o = MaterialColors.Indigo500;

            if (!n.equals(o))
                Transformer.transform(colorRecord, n, visible.get() ? 1500 : 0, Easing.OutQuint);
        }, true);

        revValueNext.onValueChanged((o, n) ->
        {
            Transformer.transform(revDisplayRecorder, n, visible.get() ? 2000 : 0, Easing.OutQuint);
        }, true);

        preferredBgColor.onValueChanged((o, n) ->
        {
            if (o == null) o = MaterialColors.Indigo500;

            if (!n.equals(o))
                Transformer.transform(bgColorRecord, n, visible.get() ? 1500 : 0, Easing.OutQuint);
        }, true);

        visible.onValueChanged((o, n) ->
        {
            // Bar进度
            Transformer.transform(progressHeightRecorder, n ? 0f : -2.5f, 650, Easing.InOutQuint);

            // Bar位置
            Transformer.transform(barHeightRecorder, n ? 0f : -6f, 650, Easing.OutBack);

            Transformer.transform(drawAlpha, n ? 1 : 0f, 650, Easing.OutQuint);
        }, true);

        this.addSchedule(this::update);
    }

    private void update()
    {
        this.addSchedule(this::update);

        //if (plugin.getCurrentTick() % 40 == 0)
        //    revValueNext.set(revValueNext.get() > 0f ? 0f : new Random().nextFloat(10, 100));

        var rev = revDisplayRecorder.get();

        var targetClr = (rev >= 20)
                ? (rev >= 80 ? MaterialColors.Red600 : MaterialColors.Orange500)
                : MaterialColors.Green400;

        colorNext.set(targetClr);
        preferredBgColor.set(targetClr.darker(3f));

        visible.set(rev > 0.1f);
    }

    public void onRender(DrawContext context, float tickDelta)
    {
        if (manager == null) return;

        var matrices = context.getMatrices();

        try
        {
            matrices.push();

            renderBar(context, tickDelta);
        }
        finally
        {
            matrices.pop();
        }
    }

    public void renderBar(DrawContext context, float tickDelta)
    {
        // 10 * 0.8
        var width = 8;

        // 35 * 0.8
        var height = 32;
        var padding = 1;

        var windowHeight = context.getScaledWindowHeight();
        var matrices = context.getMatrices();

        var shaderColor = RenderSystem.getShaderColor();
        shaderColor = new float[]
        {
            shaderColor[0],
            shaderColor[1],
            shaderColor[2],
            shaderColor[3]
        };

        context.setShaderColor(1, 1, 1, drawAlpha.get());

        // 先位移到屏幕外面
        // 然后再位移到屏幕里面
        matrices.translate(barHeightRecorder.get() + 2, windowHeight - height - 2, 0);

        context.drawBorder(0, 0, width, height, bgColorRecord.get().darker(1.3).getColor());

        // 填充背景
        context.fill(padding, padding, width - padding, height - padding, bgColorRecord.get().getColor());

        // 填充进度
        var barStart = padding + Math.round((height - padding * 2) * (revDisplayRecorder.get() / 100));
        var barEnd = height - padding;
        context.fill(padding, barStart, width - padding, barEnd, colorRecord.get().getColor());

        context.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
    }

    public void renderProgress(DrawContext context, float tickDelta)
    {
        var width = context.getScaledWindowWidth();

        var height = 2;
        var matrices = context.getMatrices();

        //logger.info("H " + heightRecorder.get());
        matrices.translate(0, progressHeightRecorder.get(), 0);

        //var width = context.getScaledWindowWidth();
        var scale = width - Math.round(width * (revDisplayRecorder.get() / 100));
        // var scale30 = Math.round(width * 0.2f);
        // var scale80 = Math.round(width * 0.8f);

        // Base W
        context.fill(0, 0, context.getScaledWindowWidth(), height, ColorUtils.forOpacity(bgColorRecord.get(), 0.6f).getColor());

        // Progress
        context.fill(0, 0, scale, height, colorRecord.get().getColor());
    }
}

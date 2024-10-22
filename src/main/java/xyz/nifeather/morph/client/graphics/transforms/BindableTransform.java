package xyz.nifeather.morph.client.graphics.transforms;

import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;
import xyz.nifeather.morph.client.utilties.TransformUtils;
import xiamomc.pluginbase.Bindables.Bindable;

public class BindableTransform<TValue> extends Transform<TValue>
{
    public final Bindable<TValue> bindable;

    protected BindableTransform(Bindable<TValue> bindable, long startTime, long duration, TValue endValue, Easing easing)
    {
        super(startTime, duration, bindable.get(), endValue, easing);

        this.bindable = bindable;
    }

    public void update(Bindable<TValue> bindable, long startTime, long duration, TValue endValue, Easing easing)
    {
        super.update(startTime, duration, bindable.get(), endValue, easing);
    }

    @Override
    public void applyProgress(double timeProgress)
    {
        TValue value;

        if (timeProgress >= 1) value = endValue;
        else if (timeProgress < 0) value = startValue;
        else value = TransformUtils.valueAt(timeProgress, startValue, endValue, easing);

        bindable.set(value);
    }
}

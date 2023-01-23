package xiamomc.morph.client.graphics.transforms;

import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.graphics.transforms.easings.Easing;

public abstract class Transform<TValue>
{
    protected Transform(long startTime, long duration, TValue startValue, TValue endValue, Easing easing)
    {
        this.update(startTime, duration, startValue, endValue, easing);
    }

    public void update(long startTime, long duration, TValue startValue, TValue endValue, Easing easing)
    {
        this.startTime = startTime;
        this.duration = duration;

        this.startValue = startValue;
        this.endValue = endValue;

        this.easing = easing;
    }

    public void abort()
    {
        this.aborted = true;
    }

    public long startTime;
    public long duration;

    public TValue startValue;
    public TValue endValue;

    @Nullable
    public Runnable onComplete;

    public Easing easing;

    public boolean aborted;

    public abstract void applyProgress(double timeProgress);
}

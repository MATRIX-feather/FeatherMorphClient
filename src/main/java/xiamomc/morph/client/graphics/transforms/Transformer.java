package xiamomc.morph.client.graphics.transforms;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.List;

public class Transformer
{
    private static long currentTime;

    public static void onClientRenderEnd(MinecraftClient client)
    {
        currentTime = System.currentTimeMillis();

        var transformList = new ObjectArrayList<>(transforms);
        for (Transform<?> t : transformList)
        {
            if (t.aborted)
            {
                transforms.remove(t);
            }
            else
            {
                double timeProgress = (currentTime - t.startTime) * 1d / t.duration;

                t.applyProgress(timeProgress);

                if (timeProgress >= 1)
                {
                    transforms.remove(t);

                    if (t.onComplete != null)
                    {
                        try
                        {
                            t.onComplete.run();
                        }
                        catch (Throwable throwable)
                        {
                            MorphClient.LOGGER.warn(throwable.getMessage());
                            throwable.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static final List<Transform<?>> transforms = new ObjectArrayList<>();

    public static synchronized void startTransform(Transform<?> info)
    {
        transforms.add(info);
    }

    public static <T> TransformBindable<T> transformBindable(Bindable<T> bindable, T endValue, long duration, Easing easing)
    {
        var prevTransform = transforms.stream()
                .filter(t -> (t instanceof TransformBindable<?> tB && tB.bindable == bindable))
                .findFirst().orElse(null);

        if (prevTransform != null)
        {
            ((TransformBindable) prevTransform).update(bindable, currentTime, duration, endValue, easing);

            return (TransformBindable<T>) prevTransform;
        }
        else
        {
            var info = new TransformBindable<>(bindable, currentTime, duration, endValue, easing);
            startTransform(info);
            return info;
        }
    }
}

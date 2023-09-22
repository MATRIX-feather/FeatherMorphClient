package xiamomc.morph.client.utilties;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import me.shedaniel.math.Color;
import org.apache.commons.lang3.NotImplementedException;
import xiamomc.morph.client.graphics.transforms.easings.Easing;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public class TransformUtils
{
    public static <TValue> TValue valueAt(double progress, TValue startVal, TValue endVal, Easing easing)
    {
        return ValueTransformer.create(progress, startVal, endVal, easing);
    }

    private static class ValueTransformer
    {
        public static Double valueAt(double progress, double startVal, double endVal, Easing easing)
        {
            if (startVal == endVal) return endVal;
            if (progress <= 0) return startVal;
            if (progress >= 1) return endVal;

            return startVal + (endVal - startVal) * easing.getImpl().apply(progress);
        }

        public static Float valueAt(double progress, float startVal, float endVal, Easing easing)
        {
            return valueAt(progress, (double) startVal, endVal, easing).floatValue();
        }

        public static Long valueAt(double progress, long startVal, long endVal, Easing easing)
        {
            return Math.round(valueAt(progress, (double) startVal, endVal, easing));
        }

        public static Integer valueAt(double progress, int startVal, int endVal, Easing easing)
        {
            return (int) Math.round(valueAt(progress, (double) startVal, endVal, easing));
        }

        public static Short valueAt(double progress, short startVal, short endVal, Easing easing)
        {
            return (short) Math.round(valueAt(progress, (double) startVal, endVal, easing));
        }

        public static Color valueAt(double progress, Color startVal, Color endVal, Easing easing)
        {
            if (startVal.equals(endVal)) return endVal;
            if (progress <= 0) return startVal;
            if (progress >= 1) return endVal;

            var r = valueAt(progress, startVal.getRed(), endVal.getRed(), easing);
            var g = valueAt(progress, startVal.getGreen(), endVal.getGreen(), easing);
            var b = valueAt(progress, startVal.getBlue(), endVal.getBlue(), easing);

            //var hueProgress = valueAt(progress, ColorUtils.GetHue(startVal), ColorUtils.GetHue(endVal), easing);
            //var satProgress = valueAt(progress, ColorUtils.getSaturation(startVal), ColorUtils.getSaturation(endVal), easing);
            //var brightnessProgress = valueAt(progress, ColorUtils.getBrightnessOrValue(startVal), ColorUtils.getBrightnessOrValue(endVal), easing);
            var alphaProgress = valueAt(progress, startVal.getAlpha(), endVal.getAlpha(), easing);

            var rawColor = Color.ofRGBA(r, g, b, alphaProgress);
            //var rawColor = Color.ofHSB(hueProgress / 360, satProgress, brightnessProgress);
            return Color.ofRGBA(rawColor.getRed(), rawColor.getGreen(), rawColor.getBlue(), alphaProgress);
        }

        private static final Map<Class<?>, Method> clazzMethodMap = new Object2ObjectArrayMap<>();

        public static <TValue> TValue create(double progress, TValue startVal, TValue endVal, Easing easing)
        {
            var valType = startVal.getClass();
            Method method = clazzMethodMap.get(valType);

            if (method == null)
            {
                var mm = Arrays.stream(ValueTransformer.class.getMethods())
                        .filter(m -> m.getReturnType() == valType && m.getName().equalsIgnoreCase("valueAt"))
                        .findFirst().orElse(null);

                clazzMethodMap.put(valType, mm);
                method = mm;
            }

            if (method == null)
                throw new NotImplementedException("No such transform method for type " + startVal.getClass());

            try
            {
                return (TValue) method.invoke(null, progress, startVal, endVal, easing);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}

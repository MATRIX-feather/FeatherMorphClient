package xiamomc.morph.client.graphics.color;

import me.shedaniel.math.Color;
import net.minecraft.client.gui.DrawContext;
import xiamomc.morph.client.utilties.MathUtils;

public class ColorUtils
{
    private static final int colorMask = 0xFF;

    public static Color fromIntRGBA(int color)
    {
        //    24 16 8  0
        //    R  G  B  A
        // 0x 90 AA BB CC
        var r = color >> 24 & colorMask;
        var g = color >> 16 & colorMask;
        var b = color >> 8 & colorMask;
        var a = color & colorMask;

        return Color.ofRGBA(r, g, b, a);
    }

    public static Color fromIntARGB(int color)
    {
        //    24 16 8  0
        //    A  R  G  B
        // 0x 90 AA BB CC
        var a = color >> 24 & colorMask;
        var r = color >> 16 & colorMask;
        var g = color >> 8 & colorMask;
        var b = color & colorMask;

        return Color.ofRGBA(r, g, b, a);
    }

    /**
     * Hex ARGB
     */
    public static Color fromHex(String hex)
    {
        if (!hex.startsWith("#"))
            hex = "#" + hex;

        boolean hasAlpha = hex.length() >= 8;

        //    16 8  0
        //    R  G  B
        // 0x CC BB AA
        int rawColor = Integer.decode(hex);

        var a = hasAlpha ? rawColor >> 24 & colorMask : 255;
        var r = rawColor >> 16 & colorMask;
        var g = rawColor >> 8 & colorMask;
        var b = rawColor & colorMask;

        return Color.ofRGBA(r, g, b, a);
    }

    public static Color forOpacity(Color color, float alpha)
    {
        alpha = MathUtils.clamp(0, 1, alpha);
        return Color.ofRGBA(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * alpha));
    }

    /**
     * @return The Hue value of the given color (0 ~ 360)
     */
    public static float GetHue(Color color)
    {
        var r1 = color.getRed() / 255f;
        var g1 = color.getGreen() / 255f;
        var b1 = color.getBlue() / 255f;

        var cMax = MathUtils.max(r1, g1, b1);
        var cMin = MathUtils.min(r1, g1, b1);
        var cDelta = cMax - cMin;

        if (cDelta == 0) return 0;
        else if (cMax == r1) return 60 * (((g1 - b1) / cDelta) % 6);
        else if (cMax == g1) return 60 * ((b1 - r1) / cDelta + 2);
        else return 60 * ((r1 - g1) / cDelta + 4);
    }

    /**
     * @return The Saturation value for the given color (0 ~ 1)
     */
    public static float getSaturation(Color color)
    {
        var r1 = color.getRed() / 255f;
        var g1 = color.getGreen() / 255f;
        var b1 = color.getBlue() / 255f;

        var cMax = MathUtils.max(r1, g1, b1);
        var cMin = MathUtils.min(r1, g1, b1);
        var cDelta = cMax - cMin;

        if (cDelta == 0) return 0;
        else return cDelta / cMax;
    }

    /**
     * @return The Brightness value for the given color (0 ~ 1)
     */
    public static float getBrightnessOrValue(Color color)
    {
        var r1 = color.getRed() / 255f;
        var g1 = color.getGreen() / 255f;
        var b1 = color.getBlue() / 255f;

        return MathUtils.max(r1, g1, b1);
    }
}

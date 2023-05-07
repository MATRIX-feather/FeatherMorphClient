package xiamomc.morph.client.graphics.color;

import me.shedaniel.math.Color;

public class ColorUtils
{
    private static final int colorMask = 0xFF;

    public static Color fromHex(String hex)
    {
        if (!hex.startsWith("#"))
            hex = "#" + hex;

        boolean hasAlpha = hex.length() >= 8;

        int rawColor = Integer.decode(hex);
        var r = rawColor >> 16 & colorMask;
        var g = rawColor >> 8 & colorMask;
        var b = rawColor & colorMask;
        var a = hasAlpha ? rawColor >> 24 & colorMask : 255;

        return Color.ofRGBA(r, g, b, a);
    }
}

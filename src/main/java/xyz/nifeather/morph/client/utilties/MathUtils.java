package xyz.nifeather.morph.client.utilties;

public class MathUtils
{
    public static float max(float v1, float v2, float v3)
    {
        var max1 = Math.max(v1, v2);
        var max2 = Math.max(v2, v3);

        return Math.max(max1, max2);
    }

    public static float min(float v1, float v2, float v3)
    {
        var max1 = Math.min(v1, v2);
        var max2 = Math.min(v2, v3);

        return Math.min(max1, max2);
    }

    public static int clamp(int min, int max, int val)
    {
        return val > max ? max : (val < min ? min : val);
    }

    public static float clamp(float min, float max, float val)
    {
        return val > max ? max : (val < min ? min : val);
    }

    public static double clamp(double min, double max, double val)
    {
        return val > max ? max : (val < min ? min : val);
    }
}

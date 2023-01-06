package xiamomc.morph.client;

import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Vec3dUtils
{
    public static Vec3d of(Vec3d other)
    {
        return of(other.x, other.y, other.z);
    }

    public static Vec3d of(double x, double y, double z)
    {
        return new Vec3d(x, y, z);
    }

    public static Vec3d of(double value)
    {
        return new Vec3d(value, value, value);
    }

    public static double horizontalSquaredDistance(Vec3d vec1, Vec3d vec2)
    {
        var xDiff = vec1.x - vec2.x;
        var zDiff = vec1.z - vec2.z;

        return xDiff * xDiff + zDiff * zDiff;
    }

    public static Vec3d ONE()
    {
        return of(1);
    }
}

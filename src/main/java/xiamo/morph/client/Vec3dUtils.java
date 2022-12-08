package xiamo.morph.client;

import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Vec3dUtils
{
    public static Vec3d of(double x, double y, double z)
    {
        return new Vec3d(x, y, z);
    }

    public static Vec3d of(double value)
    {
        return new Vec3d(value, value, value);
    }

    public static Vec3d ONE()
    {
        return of(1);
    }
}

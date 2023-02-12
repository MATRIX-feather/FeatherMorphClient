package xiamomc.morph.client.graphics;

import net.fabricmc.tinyremapper.extension.mixin.common.Logger;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.DisguiseSyncer;
import xiamomc.morph.client.MorphClient;

public class CameraHelper
{
    public float onEyeHeightCall(Entity instance, BlockView area)
    {
        if (instance == null) return 0f;

        var current = DisguiseSyncer.currentEntity.get();
        var client = MorphClient.getInstance();

        var pos = instance.getEyePos();

        if (current != null && client.morphManager.selfVisibleToggled.get() && client.getModConfigData().changeCameraHeight)
        {
            if (current.getStandingEyeHeight() <= instance.getStandingEyeHeight())
            {
                if (current.getStandingEyeHeight() <= instance.getY())
                {
                    var vehicle = instance.getVehicle();

                    return (vehicle == null ? 0.4f : vehicle.getStandingEyeHeight()) + 0.15f;
                }

                return current.getStandingEyeHeight();
            }

            var targetPos = pos.add(0, current.getStandingEyeHeight() - instance.getStandingEyeHeight(), 0);

            var rayCastContext = new RaycastContext(pos, targetPos,
                    RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, instance);

            var rayCast = area.raycast(rayCastContext);

            double distance = current.getStandingEyeHeight();
            if (rayCast.getType() != HitResult.Type.MISS)
                distance = instance.getStandingEyeHeight() + rayCast.getPos().distanceTo(pos) - 0.375f;

            //LoggerFactory.getLogger("dddd").info("type? " + (rayCast.getType()) + " :: distance? " + distance);

            return (float) distance;
        }

        return instance.getStandingEyeHeight();
    }

    public void onCameraUpdate(Camera camera)
    {
    }
}

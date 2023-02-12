package xiamomc.morph.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamomc.morph.client.Vec3dUtils;
import xiamomc.morph.client.graphics.CameraHelper;

@Mixin(Camera.class)
public abstract class CameraMixin
{
    @Shadow
    protected abstract void moveBy(double x, double y, double z);

    @Shadow
    protected abstract double clipToSpace(double desiredCameraDistance);

    @Shadow private boolean thirdPerson;

    @Shadow private Entity focusedEntity;
    @Shadow private float cameraY;
    @Shadow private BlockView area;
    @Shadow private Vec3d pos;
    private Camera featherMorph$cameraInstance;

    private final CameraHelper featherMorph$cameraHelper = new CameraHelper();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci)
    {
        featherMorph$cameraInstance = (Camera)(Object)this;
    }

    @Redirect(method = "updateEyeHeight", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getStandingEyeHeight()F"))
    private float featherMorph$onEntityEyeHeightCall(Entity instance)
    {
        return featherMorph$cameraHelper.onEyeHeightCall(instance, area);
    }
}

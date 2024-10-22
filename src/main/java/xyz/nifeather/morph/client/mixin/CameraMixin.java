package xyz.nifeather.morph.client.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.graphics.CameraHelper;

@Mixin(Camera.class)
public abstract class CameraMixin
{
    @Shadow private Entity focusedEntity;
    @Shadow private float cameraY;
    @Shadow private BlockView area;
    @Shadow private float lastCameraY;

    private Camera featherMorph$cameraInstance;
    private boolean featherMorph$sodiumExtraInstalled;

    private final CameraHelper featherMorph$cameraHelper = new CameraHelper();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci)
    {
        featherMorph$cameraInstance = (Camera)(Object)this;

        featherMorph$sodiumExtraInstalled = FabricLoader.getInstance().isModLoaded("sodium-extra");
    }

    private boolean featherMorph$isInstantSneak;

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci)
    {
        CameraHelper.isThirdPerson.set(thirdPerson);
    }

    @Redirect(method = "updateEyeHeight",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getStandingEyeHeight()F"))
    private float featherMorph$onEntityEyeHeightCall(Entity instance)
    {
        //Workaround for SodiumExtra's InstantSneak
        //https://github.com/FlashyReese/sodium-extra-fabric/blob/1.19.x/dev/src/main/java/me/flashyreese/mods/sodiumextra/mixin/instant_sneak/MixinCamera.java
        featherMorph$isInstantSneak = featherMorph$sodiumExtraInstalled && this.cameraY == instance.getStandingEyeHeight();

        if (featherMorph$isInstantSneak)
            return instance.getStandingEyeHeight();
        else
            return featherMorph$cameraHelper.onEyeHeightCall(instance, area);
    }

    @Inject(method = "updateEyeHeight",at = @At("RETURN"))
    private void featherMorph$endUpdateEyeHeight(CallbackInfo ci)
    {
        if (featherMorph$isInstantSneak)
            this.lastCameraY = this.cameraY = featherMorph$cameraHelper.onEyeHeightCall(this.focusedEntity, area);
    }
}

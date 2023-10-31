package xiamomc.morph.client.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiamomc.morph.client.EntityTickHandler;
import xiamomc.morph.client.DisguiseInstanceTracker;
import xiamomc.morph.client.syncers.ClientDisguiseSyncer;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin
{
    private PlayerEntity featherMorph$playerInstance;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void featherMorph$onInit(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci)
    {
        featherMorph$playerInstance = (PlayerEntity) (Object) this;
    }

    @Unique
    private DisguiseInstanceTracker tracker;

    @Unique
    private void ensureTrackerPresent()
    {
        if (tracker == null)
            tracker = DisguiseInstanceTracker.getInstance();
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void featherMorph$overrideDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir)
    {
        ensureTrackerPresent();
        var syncer = tracker.getSyncerFor(featherMorph$playerInstance.getId());

        if (syncer != null)
        {
            var entity = syncer.getDisguiseInstance();

            if (entity != null)
                cir.setReturnValue(entity.getDimensions(pose));
        }
    }

    /**
     * For {@link ClientDisguiseSyncer}
     * @param ci
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onTick(CallbackInfo ci)
    {
        EntityTickHandler.cancelIfIsDisguiseAndNotSyncing(ci, this);
    }
}

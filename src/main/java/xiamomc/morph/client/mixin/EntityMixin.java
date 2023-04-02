package xiamomc.morph.client.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiamomc.morph.client.DisguiseSyncer;
import xiamomc.morph.client.EntityCache;
import xiamomc.morph.client.ServerHandler;

@Mixin(Entity.class)
public abstract class EntityMixin
{
    @Shadow
    private int id;

    @Shadow
    private Vec3d pos;

    @Shadow public abstract EntityPose getPose();

    private Entity featherMorph$entityInstance;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void featherMorph$onInit(EntityType<?> type, World world, CallbackInfo ci)
    {
        featherMorph$entityInstance = (Entity) (Object) this;
    }

    @Inject(method = "getEyeY", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onGetEyeY(CallbackInfoReturnable<Double> cir)
    {
        if (featherMorph$entityInstance == MinecraftClient.getInstance().player && ServerHandler.modifyBoundingBox)
        {
            var syncerEntity = DisguiseSyncer.currentEntity.get();

            if (syncerEntity != null)
                cir.setReturnValue(MinecraftClient.getInstance().player.getY() + syncerEntity.getStandingEyeHeight());
        }
    }

    @Inject(method = "getEyeHeight(Lnet/minecraft/entity/EntityPose;)F", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onGetEyeHeight(EntityPose pose, CallbackInfoReturnable<Float> cir)
    {
        if (featherMorph$entityInstance == MinecraftClient.getInstance().player && ServerHandler.modifyBoundingBox)
        {
            var syncerEntity = DisguiseSyncer.currentEntity.get();

            if (syncerEntity != null)
                cir.setReturnValue(syncerEntity.getEyeHeight(pose));
        }
    }

    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onGetStandingEyeHeight(CallbackInfoReturnable<Float> cir)
    {
        if (featherMorph$entityInstance == MinecraftClient.getInstance().player && ServerHandler.modifyBoundingBox)
        {
            var syncerEntity = DisguiseSyncer.currentEntity.get();

            if (syncerEntity != null)
                cir.setReturnValue(syncerEntity.getStandingEyeHeight());
        }
    }

    @Inject(method = "calculateBoundingBox", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onCalcCall(CallbackInfoReturnable<Box> cir)
    {
        if (featherMorph$entityInstance == MinecraftClient.getInstance().player && ServerHandler.modifyBoundingBox)
        {
            var entity = DisguiseSyncer.currentEntity.get();

            if (entity != null)
                cir.setReturnValue(entity.getDimensions(getPose()).getBoxAt(this.pos));
        }
    }

    @Inject(method = "squaredDistanceTo(Lnet/minecraft/entity/Entity;)D", at = @At("HEAD"), cancellable = true)
    private void morphClient$onSquaredDistanceToCall(CallbackInfoReturnable<Double> cir)
    {
        if (EntityCache.containsId(id))
            cir.setReturnValue(1d);
    }
}

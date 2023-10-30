package xiamomc.morph.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiamomc.morph.client.syncers.ClientDisguiseSyncer;
import xiamomc.morph.client.EntityCache;
import xiamomc.morph.client.ServerHandler;
import xiamomc.morph.client.utilties.ClientSyncerUtils;

import java.util.function.Consumer;

@Mixin(Entity.class)
public abstract class EntityMixin
{
    @Shadow
    private int id;

    @Shadow
    private Vec3d pos;

    @Shadow public abstract EntityPose getPose();

    @Shadow public abstract void remove(Entity.RemovalReason reason);

    @Shadow public abstract int getPortalCooldown();

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
            runIfSyncerEntityNotNull(syncerEntity ->
                    cir.setReturnValue(MinecraftClient.getInstance().player.getY() + syncerEntity.getStandingEyeHeight()));
        }
    }

    @Inject(method = "getEyeHeight(Lnet/minecraft/entity/EntityPose;)F", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onGetEyeHeight(EntityPose pose, CallbackInfoReturnable<Float> cir)
    {
        if (featherMorph$entityInstance == MinecraftClient.getInstance().player && ServerHandler.modifyBoundingBox)
        {
            runIfSyncerEntityNotNull(syncerEntity ->
                    cir.setReturnValue(syncerEntity.getEyeHeight(pose)));
        }
    }

    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onGetStandingEyeHeight(CallbackInfoReturnable<Float> cir)
    {
        if (featherMorph$entityInstance == MinecraftClient.getInstance().player && ServerHandler.modifyBoundingBox)
        {
            runIfSyncerEntityNotNull(syncerEntity ->
                    cir.setReturnValue(syncerEntity.getStandingEyeHeight()));
        }
    }

    @Inject(method = "calculateBoundingBox", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onCalcCall(CallbackInfoReturnable<Box> cir)
    {
        featherMorph$onCalcCallMthod(cir);
    }

    private void featherMorph$onCalcCallMthod(CallbackInfoReturnable<Box> cir)
    {
        if (featherMorph$entityInstance == MinecraftClient.getInstance().player && ServerHandler.modifyBoundingBox)
        {
            runIfSyncerEntityNotNull(e ->
                    cir.setReturnValue(e.getDimensions(getPose()).getBoxAt(this.pos)));
        }
    }

    @Inject(method = "squaredDistanceTo(Lnet/minecraft/entity/Entity;)D", at = @At("HEAD"), cancellable = true)
    private void morphClient$onSquaredDistanceToCall(CallbackInfoReturnable<Double> cir)
    {
        if (EntityCache.getGlobalCache().containsId(id))
            cir.setReturnValue(1d);
    }

    @Unique
    private void runIfSyncerEntityNotNull(Consumer<Entity> consumerifNotNull)
    {
        ClientSyncerUtils.runIfSyncerEntityValid(consumerifNotNull::accept);
    }
}

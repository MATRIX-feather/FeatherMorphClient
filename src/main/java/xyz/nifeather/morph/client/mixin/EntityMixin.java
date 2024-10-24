package xyz.nifeather.morph.client.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nifeather.morph.client.EntityCache;
import xyz.nifeather.morph.client.ServerHandler;
import xyz.nifeather.morph.client.entities.IEntity;
import xyz.nifeather.morph.client.syncers.ClientDisguiseSyncer;
import xyz.nifeather.morph.client.utilties.ClientSyncerUtils;
import xyz.nifeather.morph.client.utilties.EntityCacheUtils;

import java.util.List;
import java.util.function.Consumer;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntity
{
    @Shadow
    private int id;

    @Shadow
    private Vec3d pos;

    @Shadow public abstract EntityPose getPose();

    @Shadow public abstract void remove(Entity.RemovalReason reason);

    @Shadow protected abstract void setFlag(int index, boolean value);

    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract void setPose(EntityPose pose);

    private Entity featherMorph$entityInstance;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void featherMorph$onInit(EntityType<?> type, World world, CallbackInfo ci)
    {
        featherMorph$entityInstance = (Entity) (Object) this;
    }

    @Inject(method = "setGlowing", at = @At("RETURN"))
    private void morphClient$onGlowingCall(boolean glowing, CallbackInfo ci)
    {
        var thisInstance = ((Entity)(Object)this);
        if (thisInstance.getCommandTags().contains(EntityCache.tag))
            this.setFlag(6, glowing);
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

    @Unique
    private void featherMorph$onCalcCallMthod(CallbackInfoReturnable<Box> cir)
    {
        if (featherMorph$entityInstance == MinecraftClient.getInstance().player && ServerHandler.modifyBoundingBox)
        {
            runIfSyncerEntityNotNull(e ->
                    cir.setReturnValue(e.getDimensions(getPose()).getBoxAt(this.pos)));
        }
    }

    @Unique
    private boolean featherMorph$isDisguiseInstance()
    {
        var currentClientSyncer = ClientDisguiseSyncer.getCurrentInstance();
        if (currentClientSyncer == null) return false;

        var disguise = currentClientSyncer.getDisguiseInstance();
        if (disguise == null) return false;

        return disguise.equals(this);
    }

    @Inject(method = "squaredDistanceTo(Lnet/minecraft/entity/Entity;)D", at = @At("HEAD"), cancellable = true)
    private void morphClient$onSquaredDistanceCallEntity(Entity entity, CallbackInfoReturnable<Double> cir)
    {
        if (featherMorph$isDisguiseInstance())
            cir.setReturnValue(1d);
    }

    @Inject(method = "squaredDistanceTo(Lnet/minecraft/util/math/Vec3d;)D", at = @At("HEAD"), cancellable = true)
    private void morphClient$onSquaredDistanceCallVec3d(Vec3d vector, CallbackInfoReturnable<Double> cir)
    {
        if (featherMorph$isDisguiseInstance())
            cir.setReturnValue(1d);
    }

    @Inject(method = "setRemoved", at = @At("RETURN"))
    private void morphClient$onRemoved(CallbackInfo ci)
    {
        EntityCacheUtils.postEntityRemove(featherMorph$entityInstance);
    }

    @Unique
    private void runIfSyncerEntityNotNull(Consumer<Entity> consumerifNotNull)
    {
        ClientSyncerUtils.runIfSyncerEntityValid(consumerifNotNull::accept);
    }

    @Unique
    private EntityPose morphClient$overridePose;

    @Override
    public void featherMorph$overridePose(@Nullable EntityPose newPose)
    {
        this.morphClient$overridePose = newPose;

        if (newPose != null)
            this.setPose(newPose);
    }

    @Inject(method = "getPose", at = @At("HEAD"), cancellable = true)
    private void morphClient$onPoseCall(CallbackInfoReturnable<EntityPose> cir)
    {
        if (morphClient$overridePose != null)
            cir.setReturnValue(morphClient$overridePose);
    }

    @Unique
    @Nullable
    private Boolean morphClient$isInvisible;

    @Override
    public void featherMorph$overrideInvisibility(boolean invisible)
    {
        if (invisible)
            this.morphClient$isInvisible = invisible;
        else
            this.morphClient$isInvisible = null;
    }

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void morphClient$onInvisibleCall(CallbackInfoReturnable<Boolean> cir)
    {
        if (this.morphClient$isInvisible != null)
            cir.setReturnValue(this.morphClient$isInvisible);
    }

    @Unique
    private boolean morphClient$noAcceptSetPose;

    @Override
    public void featherMorph$setNoAcceptSetPose(boolean noAccept)
    {
        this.morphClient$noAcceptSetPose = noAccept;
    }

    @Inject(method = "setPose", at = @At("HEAD"), cancellable = true)
    private void morphClient$onSetPose(EntityPose pose, CallbackInfo ci)
    {
        if (this.morphClient$noAcceptSetPose)
            ci.cancel();
    }

    @Unique
    private final List<Object> morphClient$bypassRequests = new ObjectArrayList<>();

    @Override
    public void featherMorph$requestBypassDispatcherRedirect(Object source, boolean bypass)
    {
        if (!bypass)
        {
            morphClient$bypassRequests.remove(source);
            return;
        }

        if (morphClient$bypassRequests.contains(source)) return;

        morphClient$bypassRequests.add(source);
    }

    @Override
    public boolean featherMorph$bypassesDispatcherRedirect()
    {
        return !morphClient$bypassRequests.isEmpty();
    }
}

package xyz.nifeather.morph.client.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nifeather.morph.client.DisguiseInstanceTracker;
import xyz.nifeather.morph.client.EntityTickHandler;
import xyz.nifeather.morph.client.ServerHandler;
import xyz.nifeather.morph.client.syncers.ClientDisguiseSyncer;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin
{
    @Unique
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

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void featherMorph$overrideDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir)
    {
        if (featherMorph$playerInstance.getWorld().isClient())
        {
            ensureTrackerPresent();
            var syncer = tracker.getSyncerFor(featherMorph$playerInstance.getId());

            if (syncer != null && !syncer.disposed())
            {
                // 如果是客户端伪装并且服务端没有开启碰撞箱修改，那么不要修改此方法的返回值
                if (syncer == ClientDisguiseSyncer.getCurrentInstance() && !ServerHandler.modifyBoundingBox)
                    return;

                // 否则，根据伪装实例来修改碰撞箱
                var entity = syncer.getDisguiseInstance();

                if (entity != null)
                {
                    // 坑: 我们要返回的是BaseDimension, 但获取了目标实体的Dimensions
                    //     目标实体的Dimensions会随着SCALE缩放
                    EntityDimensions dimensions = entity.getBaseDimensions(pose);

                    // 扩大其他玩家的碰撞箱来使其能被客户端玩家攻击到
                    if (syncer != ClientDisguiseSyncer.getCurrentInstance())
                    {
                        if (dimensions.fixed())
                            dimensions = EntityDimensions.fixed(dimensions.width() + 0.001f, dimensions.height() + 0.001f);
                        else
                            dimensions = EntityDimensions.changing(dimensions.width() + 0.001f, dimensions.height() + 0.001f);
                    }

                    cir.setReturnValue(dimensions);
                }
            }
        }
    }

    /**
     * For {@link ClientDisguiseSyncer}
     * @param ci
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void featherMorph$onTick(CallbackInfo ci)
    {
        if (featherMorph$playerInstance.getWorld().isClient())
            EntityTickHandler.cancelIfIsDisguiseAndNotSyncing(ci, this);
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void featherMorph$onAttack(Entity target, CallbackInfo ci)
    {
        if (featherMorph$playerInstance.getWorld().isClient())
        {
            var syncer = DisguiseInstanceTracker.getInstance().getSyncerFor(featherMorph$playerInstance.getId());
            if (syncer != null)
                syncer.playAttackAnimation();
        }
    }
}

package xiamomc.morph.client.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.MinecraftVersion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiamomc.morph.client.DisguiseSyncer;

@Mixin(PlayerEntity.class)
public class PlayerMixin
{
    private PlayerEntity featherMorph$playerInstance;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void featherMorph$onInit(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci)
    {
        featherMorph$playerInstance = (PlayerEntity) (Object) this;
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void featherMorph$overrideDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir)
    {
        if (featherMorph$playerInstance == MinecraftClient.getInstance().player && false)
        {
            var entity = DisguiseSyncer.currentEntity.get();

            if (entity != null)
                cir.setReturnValue(entity.getDimensions(pose));
        }
    }
}

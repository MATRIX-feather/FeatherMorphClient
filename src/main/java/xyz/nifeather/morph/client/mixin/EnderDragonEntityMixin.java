package xyz.nifeather.morph.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.FeatherMorphClient;
import xyz.nifeather.morph.client.entities.IMorphClientEntity;

import java.util.Random;

@Mixin(EnderDragonEntity.class)
public class EnderDragonEntityMixin
{
    @Unique
    private static final Random random = new Random();

    @Unique
    private EnderDragonEntity morphClient$entityInstance;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void morphClient$onInit(EntityType<?> entityType, World world, CallbackInfo ci)
    {
        this.morphClient$entityInstance = (EnderDragonEntity) (Object) this;
    }

    @Inject(method = "addFlapEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V"))
    private void morphClient$onFlapWings(CallbackInfo ci)
    {
        if (((IMorphClientEntity)this).featherMorph$isDisguiseEntity())
            morphClient$playSoundAtPlayer();
    }

    @Unique
    private void morphClient$playSoundAtPlayer()
    {
        var fmClient = FeatherMorphClient.getInstance();
        var allowClientView = fmClient.getModConfigData().allowClientView;
        if (!allowClientView && fmClient.morphManager.selfVisibleEnabled.get()) return;

        var playerLoc = MinecraftClient.getInstance().player.getPos();
        morphClient$entityInstance.getWorld().playSound(playerLoc.x, playerLoc.y, playerLoc.z,
                SoundEvents.ENTITY_ENDER_DRAGON_FLAP, morphClient$entityInstance.getSoundCategory(),
                5.0F, 0.8F + random.nextFloat() * 0.3F, false);
    }
}

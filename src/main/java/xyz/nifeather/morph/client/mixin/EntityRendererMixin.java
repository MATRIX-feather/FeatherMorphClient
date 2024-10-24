package xyz.nifeather.morph.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nifeather.morph.client.syncers.ClientDisguiseSyncer;

@Mixin(LivingEntityRenderer.class)
public class EntityRendererMixin
{
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void morphclient$hasLabel(LivingEntity livingEntity, double d, CallbackInfoReturnable<Boolean> cir)
    {
        if (livingEntity != MinecraftClient.getInstance().player)
        {
            var clientSyncer = ClientDisguiseSyncer.getCurrentInstance();
            if (clientSyncer == null) return;

            cir.setReturnValue(clientSyncer.getDisguiseInstance() != livingEntity);
        }
    }
}

package xyz.nifeather.morph.client.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.entities.MorphLocalPlayer;
import xyz.nifeather.morph.client.syncers.ClientDisguiseSyncer;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin
{
    @Inject(
            method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("RETURN")
    )
    public void onUpdateRenderState(AbstractClientPlayerEntity abstractClientPlayerEntity, PlayerEntityRenderState playerEntityRenderState, float f, CallbackInfo ci)
    {
        if (abstractClientPlayerEntity instanceof MorphLocalPlayer localPlayer
                && !localPlayer.shouldRenderName())
        {
            playerEntityRenderState.displayName = null;
        }
    }
}

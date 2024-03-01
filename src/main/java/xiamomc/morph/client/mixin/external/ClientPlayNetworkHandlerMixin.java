package xiamomc.morph.client.mixin.external;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamomc.morph.client.MorphClient;

import java.util.Arrays;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin
{
    @Unique
    private final double worldMax = 29999984 * 3d;

    @Inject(
            method = "onEntityPosition",
            at = @At("HEAD"),
            cancellable = true
    )
    public void onEntityPosPacket(EntityPositionS2CPacket packet, CallbackInfo ci)
    {
        if (isValueInvalid(worldMax, packet.getX(), packet.getY(), packet.getZ()))
        {
            cancelPacket(packet, ci, "Invalid position");
        }
    }

    @Inject(
            method = "onPlayerPositionLook",
            at = @At("HEAD"),
            cancellable = true
    )
    public void onPosAndLookPacket(PlayerPositionLookS2CPacket packet, CallbackInfo ci)
    {
        if (isValueInvalid(worldMax, packet.getX(), packet.getY(), packet.getZ(), packet.getPitch(), packet.getYaw(), packet.getTeleportId()))
        {
            cancelPacket(packet, ci, "Position or Yaw/Pitch/TeleportId invalid");
        }
    }

    @Inject(
            method = "onExplosion",
            at = @At("HEAD"),
            cancellable = true
    )
    public void onExplosion(ExplosionS2CPacket packet, CallbackInfo ci)
    {
        if (isValueInvalid(worldMax,
                packet.getX(), packet.getY(), packet.getZ(),
                packet.getPlayerVelocityX(), packet.getPlayerVelocityY(), packet.getPlayerVelocityZ()))
        {
            cancelPacket(packet, ci, "Position or Velocity too large");
        }
    }

    @Inject(
            method = "onParticle",
            at = @At("HEAD"),
            cancellable = true
    )
    public void onParticle(ParticleS2CPacket packet, CallbackInfo ci)
    {
        if (isValueInvalid(worldMax,
                packet.getX(), packet.getY(), packet.getZ(),
                packet.getOffsetX(), packet.getOffsetY(), packet.getOffsetZ(),
                packet.getSpeed()))
        {
            cancelPacket(packet, ci, "Invalid position, speed, or offset data");
        }

        if (packet.getCount() >= 30000)
            cancelPacket(packet, ci, "Particle count larger than 30000");
    }

    @Unique
    private void cancelPacket(Packet<?> packet, CallbackInfo ci)
    {
        cancelPacket(packet, ci, null);
    }

    @Unique
    private void cancelPacket(Packet<?> packet, CallbackInfo ci, @Nullable String reason)
    {
        if (reason == null)
            MorphClient.LOGGER.info("Cancelling invalid %s packet from server!".formatted(packet));
        else
            MorphClient.LOGGER.info("Cancelling invalid %s packet from server: %s".formatted(packet, reason));
        ci.cancel();
    }

    @Unique
    private boolean isValueInvalid(double maxPos, double... values)
    {
        return Arrays.stream(values).anyMatch(v -> Math.abs(v) > maxPos);
    }
}

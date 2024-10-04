package xiamomc.morph.client.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiamomc.morph.client.ServerHandler;
import xiamomc.morph.client.utilties.ClientSyncerUtils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayerEntity
{
    @Shadow
    @Nullable
    public Input input;

    @Shadow @Final private ClientRecipeBook recipeBook;
    @Nullable
    private Boolean inputLastValue;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile)
    {
        super(world, profile);
    }

    @Unique
    private void morphclient$runIfSyncerEntityNotNull(Consumer<Entity> consumerifNotNull)
    {
        ClientSyncerUtils.runIfSyncerEntityValid(consumerifNotNull);
    }

    @Override
    public double getEyeY()
    {
        if (ServerHandler.modifyBoundingBox)
            return morphclient$onGetEyeY();

        return super.getEyeY();
    }

    @Unique
    private double morphclient$onGetEyeY()
    {
        AtomicReference<Double> disguiseEyeY = new AtomicReference<>(0d);
        morphclient$runIfSyncerEntityNotNull(syncerEntity ->
        {
            disguiseEyeY.set(MinecraftClient.getInstance().player.getY() + syncerEntity.getStandingEyeHeight());
        });

        return disguiseEyeY.get();
    }

    @Override
    protected Box calculateBoundingBox()
    {
        return super.calculateBoundingBox();
    }

    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void morphclient$onSneakingCall(CallbackInfoReturnable<Boolean> cir)
    {
        var serverSideSneaking = ServerHandler.serverSideSneaking;

        //如果input的下蹲状态发生变化，则重置服务器状态并返回input的当前状态
        if (input != null && (inputLastValue == null || input.sneaking != inputLastValue))
        {
            inputLastValue = input.sneaking;

            cir.setReturnValue(input.sneaking);
            ServerHandler.serverSideSneaking = serverSideSneaking = null;
            return;
        }

        //否则返回服务器状态
        if (serverSideSneaking != null)
            cir.setReturnValue(serverSideSneaking);
    }
}

package xiamomc.morph.client.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamomc.morph.client.DisguiseInstanceTracker;

@Mixin(ClientWorld.class)
public class ClientWorldMixin
{
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void fm$onAddEntity(Entity entity, CallbackInfo ci)
    {
        var fm$instanceTracker = DisguiseInstanceTracker.getInstance();
        fm$instanceTracker.addSyncerIfNotExist(entity);
    }
}

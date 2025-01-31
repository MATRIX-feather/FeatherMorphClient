package xyz.nifeather.morph.client.mixin;

import net.minecraft.entity.data.DataTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.FeatherMorphClient;

import java.util.List;

@Mixin(DataTracker.class)
public class DataTrackerMixin
{
    @Shadow @Final private DataTracker.Entry<?>[] entries;

    @Inject(
            method = "writeUpdatedEntries",
            at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z")
    )
    public void onEntries(List<DataTracker.SerializedEntry<?>> newEntries, CallbackInfo ci)
    {
        if (newEntries.stream().anyMatch(entry -> entry.id() >= this.entries.length))
        {
            FeatherMorphClient.LOGGER.error("Server sent a metadata packet with mismatched entry id!");
            this.morphclient$dumpEntries(newEntries);
        }
    }

    @Unique
    private void morphclient$dumpEntries(List<DataTracker.SerializedEntry<?>> entries)
    {
        FeatherMorphClient.LOGGER.info("- x - x - x - Entries - x - x - x -");
        for (DataTracker.SerializedEntry<?> entry : entries)
            FeatherMorphClient.LOGGER.info("ID '%s' -> VALUE '%s'".formatted(entry.id(), entry.value()));
        FeatherMorphClient.LOGGER.info("- x - x - x - Entries - x - x - x -");
    }
}

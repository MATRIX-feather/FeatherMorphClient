package xiamomc.morph.client.mixin;

import net.minecraft.entity.data.DataTracker;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiamomc.morph.client.MorphClient;

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
            MorphClient.LOGGER.error("Server sent a metadata packet with mismatched entry id!");
            this.morphclient$dumpEntries(newEntries);
        }
    }

    @Unique
    private void morphclient$dumpEntries(List<DataTracker.SerializedEntry<?>> entries)
    {
        MorphClient.LOGGER.info("- x - x - x - Entries - x - x - x -");
        for (DataTracker.SerializedEntry<?> entry : entries)
            MorphClient.LOGGER.info("ID '%s' -> VALUE '%s'".formatted(entry.id(), entry.value()));
        MorphClient.LOGGER.info("- x - x - x - Entries - x - x - x -");
    }
}

package xiamomc.morph.client.mixin.accessors;

import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.ExecutorService;

@Mixin(Util.class)
public interface UtilAccessor
{
    @Invoker
    ExecutorService callCreateWorker(String name);
}

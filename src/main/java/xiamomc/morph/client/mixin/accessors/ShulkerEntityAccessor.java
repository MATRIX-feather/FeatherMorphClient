package xiamomc.morph.client.mixin.accessors;

import net.minecraft.entity.mob.ShulkerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ShulkerEntity.class)
public interface ShulkerEntityAccessor
{
    @Invoker
    public void callSetPeekAmount(int peek);
}

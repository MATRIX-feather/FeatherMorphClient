package xiamomc.morph.client.mixin.accessors;

import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorStandEntity.class)
public interface ArmorStandEntityAccessor
{
    @Invoker
    void callSetShowArms(boolean showArms);
}

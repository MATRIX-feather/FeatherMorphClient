package xiamomc.morph.client.mixin.accessors;

import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HorseEntity.class)
public interface HorseEntityMixin
{
    @Invoker
    void callEquipArmor(ItemStack stack);
}

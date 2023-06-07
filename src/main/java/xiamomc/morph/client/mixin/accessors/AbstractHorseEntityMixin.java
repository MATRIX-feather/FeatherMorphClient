package xiamomc.morph.client.mixin.accessors;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractHorseEntity.class)
public interface AbstractHorseEntityMixin
{
    @Invoker
    void callSetHorseFlag(int bitmask, boolean flag);

    @Accessor
    public SimpleInventory getItems();
}

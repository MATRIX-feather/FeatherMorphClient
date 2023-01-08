package xiamomc.morph.network.commands.S2C;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import xiamomc.morph.misc.ClientItemUtils;

public class S2CSetEquipCommand extends S2CSetCommand<ItemStack>
{
    public S2CSetEquipCommand(ItemStack item, EquipmentSlot slot)
    {
        super(item);
        this.slot = slot;
    }

    private final EquipmentSlot slot;

    @Override
    public String getBaseName()
    {
        return "equip";
    }

    @Override
    public String buildCommand()
    {
        var slotName = switch (slot)
                {
                    case MAINHAND -> "mainhand";
                    case HEAD -> "helmet";
                    case CHEST -> "chestplate";
                    case LEGS -> "leggings";
                    case FEET -> "boots";
                    default -> slot.name().toLowerCase();
                };

        var item = ClientItemUtils.itemOrAir(this.getArgumentAt(0));

        return super.buildCommand() + " " + slotName + " " + ClientItemUtils.itemToStr(item);
    }
}

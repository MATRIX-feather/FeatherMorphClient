package xiamomc.morph.client.network.commands.S2C.set;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.DisguiseSyncer;

public class S2CSetEquipCommand extends AbstractSetCommand<ItemStack>
{
    public S2CSetEquipCommand(ClientMorphManager morphManager)
    {
        super(morphManager);
    }

    @Override
    public String getBaseName()
    {
        return "equip";
    }

    @Override
    public void onCommand(String arguments)
    {
        var dat = arguments.split(" ", 2);

        if (dat.length != 2) return;
        var currentMob = DisguiseSyncer.currentEntity.get();

        if (currentMob == null) return;

        var stack = jsonToStack(dat[1]);

        if (stack == null) return;

        switch (dat[0])
        {
            case "mainhand" -> morphManager.setEquip(EquipmentSlot.MAINHAND, stack);
            case "off_hand" -> morphManager.setEquip(EquipmentSlot.OFFHAND, stack);

            case "helmet" -> morphManager.setEquip(EquipmentSlot.HEAD, stack);
            case "chestplate" -> morphManager.setEquip(EquipmentSlot.CHEST, stack);
            case "leggings" -> morphManager.setEquip(EquipmentSlot.LEGS, stack);
            case "boots" -> morphManager.setEquip(EquipmentSlot.FEET, stack);
        }
    }

    @Nullable
    private ItemStack jsonToStack(String rawJson)
    {
        var item = ItemStack.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseString(rawJson));

        if (item.result().isPresent())
            return item.result().get().getFirst();

        return null;
    }
}

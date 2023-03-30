package xiamomc.morph.client.network.commands;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.commands.S2C.set.S2CSetFakeEquipCommand;

import java.util.Arrays;

public class ClientSetEquipCommand extends S2CSetFakeEquipCommand<ItemStack>
{
    public ClientSetEquipCommand(ItemStack item, ProtocolEquipmentSlot slot)
    {
        super(item, slot);
    }

    public static ClientSetEquipCommand from(String rawArguments)
    {
        //temp to array
        var dat = rawArguments.split(" ", 2);

        if (dat.length != 2) return null;

        var stack = jsonToStack(dat[1]);
        if (stack == null) return null;

        var slot = ProtocolEquipmentSlot.valueOf(dat[0].toUpperCase());

        return new ClientSetEquipCommand(stack, slot);
    }

    @Nullable
    private static ItemStack jsonToStack(String rawJson)
    {
        var item = ItemStack.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseString(rawJson));

        if (item.result().isPresent())
            return item.result().get().getFirst();

        return null;
    }
}

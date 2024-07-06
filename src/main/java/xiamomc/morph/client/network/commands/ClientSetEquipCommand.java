package xiamomc.morph.client.network.commands;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.morph.network.commands.S2C.set.S2CSetFakeEquipCommand;

public class ClientSetEquipCommand extends S2CSetFakeEquipCommand<ItemStack>
{
    private static final Logger log = LoggerFactory.getLogger(ClientSetEquipCommand.class);

    public ClientSetEquipCommand(ItemStack item, ProtocolEquipmentSlot slot)
    {
        super(item, slot);
    }

    public static ClientSetEquipCommand from(String rawArguments)
    {
        log.info("~RAW IS " + rawArguments);

        //temp to array
        var dat = rawArguments.split(" ", 2);

        if (dat.length != 2) return null;

        log.info("~DECODING: " + dat[1]);
        var stack = jsonToStack(dat[1]);
        if (stack == null) return null;

        var slot = ProtocolEquipmentSlot.valueOf(dat[0].toUpperCase());

        return new ClientSetEquipCommand(stack, slot);
    }

    @Nullable
    private static ItemStack jsonToStack(String rawJson)
    {
        var world = MinecraftClient.getInstance().world;
        if (world == null)
            throw new NullPointerException("Called jsonToStack but client world is null?!");

        var registry = MinecraftClient.getInstance().world.getRegistryManager();

        var item = ItemStack.CODEC.decode(registry.getOps(JsonOps.INSTANCE), JsonParser.parseString(rawJson));

        if (item.result().isPresent())
            return item.result().get().getFirst();

        return null;
    }
}

package xyz.nifeather.morph.client.utilties;

import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xyz.nifeather.morph.client.MorphClient;

public class NbtUtils
{
    private static final Logger logger = MorphClient.LOGGER;

    public static NbtCompound parseOrThrow(@Nullable String snbt) throws Throwable
    {
        //MinecraftClient.getInstance().world.getRegistryManager()

        var ops = MinecraftClient.getInstance().world.getRegistryManager().getOps(NbtOps.INSTANCE);

        if (StringNbtReader.read(ops, snbt) instanceof NbtCompound compound)
            return compound;

        throw new RuntimeException("Unable to read compound! The return value of StringNbtReader.read was not an NbtCompound");
    }

    @Nullable
    public static NbtCompound parseSNbt(@Nullable String snbt)
    {
        if (snbt == null || snbt.isEmpty())
            return null;

        try
        {
            return parseOrThrow(snbt.replace("\\u003d", "="));
        }
        catch (Throwable t)
        {
            logger.warn("Unable to parse SNBT (%s): %s".formatted(t.getMessage(), snbt));
            t.printStackTrace();
        }

        return null;
    }
}

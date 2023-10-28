package xiamomc.morph.client.utilties;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xiamomc.morph.client.MorphClient;

public class NbtUtils
{
    private static final Logger logger = MorphClient.LOGGER;

    @Nullable
    public static NbtCompound parseSNbt(@Nullable String snbt)
    {
        if (snbt == null || snbt.isEmpty())
            return null;

        try
        {
            return StringNbtReader.parse(snbt.replace("\\u003d", "="));
        }
        catch (Throwable t)
        {
            logger.warn("Unable to parse SNBT (%s): %s".formatted(t.getMessage(), snbt));
            t.printStackTrace();
        }

        return null;
    }
}

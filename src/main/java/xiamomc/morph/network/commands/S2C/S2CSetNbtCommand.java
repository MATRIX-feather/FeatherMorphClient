package xiamomc.morph.network.commands.S2C;

import net.minecraft.nbt.NbtCompound;
import xiamomc.morph.misc.ClientNbtUtils;

public class S2CSetNbtCommand extends S2CSetCommand<NbtCompound>
{
    public S2CSetNbtCommand(NbtCompound tag)
    {
        super(tag);
    }

    @Override
    public String getBaseName()
    {
        return "nbt";
    }

    @Override
    public String buildCommand()
    {
        var nbt = this.getArgumentAt(0, new NbtCompound());
        return super.buildCommand() + " " + ClientNbtUtils.getCompoundString(nbt);
    }
}

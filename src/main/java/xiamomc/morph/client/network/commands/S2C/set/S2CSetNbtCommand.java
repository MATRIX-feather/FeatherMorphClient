package xiamomc.morph.client.network.commands.S2C.set;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.misc.ClientNbtUtils;

public class S2CSetNbtCommand extends AbstractSetCommand<NbtCompound>
{
    public S2CSetNbtCommand(ClientMorphManager morphManager) {
        super(morphManager);
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

    @Override
    public void onCommand(String arguments)
    {
        try
        {
            var nbt = StringNbtReader.parse(arguments.replace("\\u003d", "="));

            morphManager.currentNbtCompound.set(nbt);
        }
        catch (CommandSyntaxException e)
        {
            //todo
        }
    }
}

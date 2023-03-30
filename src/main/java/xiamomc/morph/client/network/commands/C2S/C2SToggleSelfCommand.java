package xiamomc.morph.client.network.commands.C2S;

import net.minecraft.entity.player.PlayerEntity;
import xiamomc.morph.network.commands.C2S.AbstractC2SCommand;

public class C2SToggleSelfCommand extends AbstractC2SCommand<PlayerEntity, String>
{
    public C2SToggleSelfCommand(String argument)
    {
        super(argument);
    }

    public C2SToggleSelfCommand(String... arguments)
    {
        super(arguments);
    }

    @Override
    public String getBaseName()
    {
        return "toggleself";
    }

    @Override
    public String buildCommand()
    {
        return super.buildCommand() + " " + getArgumentAt(this.arguments, 0) + " " + getArgumentAt(this.arguments, 1, "");
    }
}

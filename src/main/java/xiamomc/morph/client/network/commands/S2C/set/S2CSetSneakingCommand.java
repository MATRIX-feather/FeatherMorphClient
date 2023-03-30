package xiamomc.morph.client.network.commands.S2C.set;

import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.ServerHandler;

public class S2CSetSneakingCommand extends AbstractSetCommand<Boolean>
{
    public S2CSetSneakingCommand(ClientMorphManager morphManager)
    {
        super(morphManager);
    }

    @Override
    public String getBaseName()
    {
        return "sneaking";
    }

    @Override
    public String buildCommand()
    {
        return super.buildCommand() + " " + this.getArgumentAt(0, false);
    }

    @Override
    public void onCommand(String arguments)
    {
        ServerHandler.serverSideSneaking = Boolean.valueOf(arguments);
    }
}

package xiamomc.morph.client.network.commands.S2C.set;

import xiamomc.morph.client.ClientMorphManager;

public class S2CSetSelfViewCommand extends AbstractSetCommand<String>
{
    public S2CSetSelfViewCommand(ClientMorphManager morphManager)
    {
        super(morphManager);
    }

    @Override
    public String getBaseName()
    {
        return "selfview";
    }

    @Override
    public String buildCommand()
    {
        return super.buildCommand() + " " + this.getArgumentAt(0, "");
    }

    @Override
    public void onCommand(String arguments)
    {
        morphManager.selfViewIdentifier.set(arguments);
    }
}

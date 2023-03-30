package xiamomc.morph.client.network.commands.S2C.set;

import xiamomc.morph.client.ClientMorphManager;

public class S2CSetToggleSelfCommand extends AbstractSetCommand<Boolean>
{
    public S2CSetToggleSelfCommand(ClientMorphManager morphManager)
    {
        super(morphManager);
    }

    @Override
    public String getBaseName()
    {
        return "toggleself";
    }

    @Override
    public void onCommand(String arguments)
    {
        var val = Boolean.parseBoolean(arguments);

        morphManager.selfVisibleToggled.set(val);
    }
}

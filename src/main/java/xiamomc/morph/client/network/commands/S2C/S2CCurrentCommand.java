package xiamomc.morph.client.network.commands.S2C;

import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

public class S2CCurrentCommand extends AbstractS2CCommand<String>
{
    public S2CCurrentCommand(ClientMorphManager morphManager)
    {
        this.morphManager = morphManager;
    }

    private final ClientMorphManager morphManager;

    @Override
    public String getBaseName()
    {
        return "current";
    }

    @Override
    public void onCommand(String arguments)
    {
        morphManager.setCurrent(arguments.equals("null") ? null : arguments);
    }
}

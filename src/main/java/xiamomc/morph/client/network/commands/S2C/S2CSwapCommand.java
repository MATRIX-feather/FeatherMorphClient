package xiamomc.morph.client.network.commands.S2C;

import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

public class S2CSwapCommand extends AbstractS2CCommand<Object>
{
    public S2CSwapCommand(ClientMorphManager morphManager)
    {
        this.morphManager = morphManager;
    }

    private final ClientMorphManager morphManager;

    @Override
    public String getBaseName()
    {
        return "swap";
    }

    @Override
    public void onCommand(String rawArgument)
    {
        morphManager.swapHand();
    }
}

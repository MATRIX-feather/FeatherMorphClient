package xiamomc.morph.client.network.commands.S2C.set;

import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

public abstract class AbstractSetCommand<T> extends AbstractS2CCommand<T>
{
    public AbstractSetCommand(ClientMorphManager morphManager)
    {
        this.morphManager = morphManager;
    }

    protected ClientMorphManager morphManager;
}

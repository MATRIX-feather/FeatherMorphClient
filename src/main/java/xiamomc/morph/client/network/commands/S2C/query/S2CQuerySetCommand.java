package xiamomc.morph.client.network.commands.S2C.query;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

public class S2CQuerySetCommand extends AbstractS2CCommand<String>
{
    public S2CQuerySetCommand(ClientMorphManager morphManager)
    {
        this.morphManager = morphManager;
    }

    private ClientMorphManager morphManager;

    @Override
    public String getBaseName()
    {
        return "set";
    }

    @Override
    public void onCommand(String arguments)
    {
        var diff = new ObjectArrayList<>(arguments.split(" "));
        morphManager.setDisguises(diff);
    }
}

package xiamomc.morph.client.network.commands.S2C.query;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

public class S2CQueryRemoveCommand extends AbstractS2CCommand<String>
{
    public S2CQueryRemoveCommand(ClientMorphManager morphManager)
    {
        this.morphManager = morphManager;
    }

    private ClientMorphManager morphManager;

    @Override
    public String getBaseName()
    {
        return "remove";
    }

    @Override
    public void onCommand(String arguments)
    {
        var diff = new ObjectArrayList<>(arguments.split(" "));
        morphManager.removeDisguises(diff);
    }
}

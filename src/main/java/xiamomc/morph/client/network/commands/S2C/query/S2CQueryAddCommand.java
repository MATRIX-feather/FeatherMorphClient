package xiamomc.morph.client.network.commands.S2C.query;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

public class S2CQueryAddCommand extends AbstractS2CCommand<String>
{
    public S2CQueryAddCommand(ClientMorphManager morphManager)
    {
        this.morphManager = morphManager;
    }

    private final ClientMorphManager morphManager;

    @Override
    public String getBaseName()
    {
        return "add";
    }

    @Override
    public void onCommand(String arguments)
    {
        var diff = new ObjectArrayList<>(arguments.split(" "));
        morphManager.addDisguises(diff);
    }
}

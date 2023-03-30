package xiamomc.morph.client.network.commands.S2C;

import xiamomc.morph.client.ServerHandler;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

public class S2CUnAuthCommand extends AbstractS2CCommand<String>
{
    public S2CUnAuthCommand(ServerHandler serverHandler)
    {
        this.serverHandler = serverHandler;
    }

    private ServerHandler serverHandler;

    @Override
    public String getBaseName()
    {
        return "unauth";
    }

    @Override
    public void onCommand(String arguments)
    {
        serverHandler.disconnect();
    }
}

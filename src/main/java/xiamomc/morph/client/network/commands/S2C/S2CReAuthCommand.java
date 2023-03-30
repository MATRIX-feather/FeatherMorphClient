package xiamomc.morph.client.network.commands.S2C;

import xiamomc.morph.client.ServerHandler;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

public class S2CReAuthCommand extends AbstractS2CCommand<String>
{
    public S2CReAuthCommand(ServerHandler serverHandler)
    {
        this.serverHandler = serverHandler;
    }

    private final ServerHandler serverHandler;

    @Override
    public String getBaseName()
    {
        return "reauth";
    }

    @Override
    public void onCommand(String arguments)
    {
        serverHandler.disconnect();
        serverHandler.connect();
    }
}

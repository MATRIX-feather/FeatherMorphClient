package xiamomc.morph.client;

import xiamomc.pluginbase.ClientPluginObject;

public class MorphClientObject extends ClientPluginObject<MorphClient>
{
    @Override
    protected String getPluginNamespace()
    {
        return MorphClient.getClientNameSpace();
    }
}

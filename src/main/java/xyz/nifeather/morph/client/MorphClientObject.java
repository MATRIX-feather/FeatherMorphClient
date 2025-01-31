package xyz.nifeather.morph.client;

import xiamomc.pluginbase.PluginObject;

public class MorphClientObject extends PluginObject<FeatherMorphClient>
{
    @Override
    protected String getPluginNamespace()
    {
        return FeatherMorphClient.getClientNameSpace();
    }
}

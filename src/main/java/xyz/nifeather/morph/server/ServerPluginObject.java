package xyz.nifeather.morph.server;

import xiamomc.pluginbase.PluginObject;

public class ServerPluginObject extends PluginObject<FeatherMorphFabricMain>
{
    @Override
    protected String getPluginNamespace()
    {
        return FeatherMorphFabricMain.pluginNamespace();
    }
}

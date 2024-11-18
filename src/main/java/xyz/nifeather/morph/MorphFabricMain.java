package xyz.nifeather.morph;

import net.fabricmc.api.ModInitializer;
import xyz.nifeather.morph.server.MorphServer;

public class MorphFabricMain implements ModInitializer
{
    private final MorphServer morphServer = new MorphServer();

    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize()
    {
        morphServer.init();
    }
}

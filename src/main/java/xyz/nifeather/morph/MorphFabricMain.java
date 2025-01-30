package xyz.nifeather.morph;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import xyz.nifeather.morph.server.MorphServer;
import xyz.nifeather.morph.shared.payload.*;

public class MorphFabricMain implements ModInitializer
{
    private final MorphServer morphServer = new MorphServer();

    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize()
    {
        PayloadTypeRegistry.playS2C().register(MorphInitChannelPayload.id, MorphInitChannelPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(MorphVersionChannelPayload.id, MorphVersionChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MorphCommandPayload.id, MorphCommandPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(LegacyMorphVersionChannelPayload.id, LegacyMorphVersionChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LegacyMorphCommandPayload.id, LegacyMorphCommandPayload.CODEC);

        morphServer.onModLoad();
    }
}

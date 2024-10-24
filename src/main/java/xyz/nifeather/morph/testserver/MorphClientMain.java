package xyz.nifeather.morph.testserver;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import xyz.nifeather.morph.client.network.payload.MorphCommandPayload;
import xyz.nifeather.morph.client.network.payload.MorphInitChannelPayload;
import xyz.nifeather.morph.client.network.payload.MorphVersionChannelPayload;

public class MorphClientMain implements ModInitializer
{
    private final VirtualServer virtualServer = new VirtualServer();

    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize()
    {
        PayloadTypeRegistry.playC2S().register(MorphInitChannelPayload.id, MorphInitChannelPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MorphVersionChannelPayload.id, MorphVersionChannelPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MorphCommandPayload.id, MorphCommandPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(MorphInitChannelPayload.id, MorphInitChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MorphVersionChannelPayload.id, MorphVersionChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MorphCommandPayload.id, MorphCommandPayload.CODEC);

        virtualServer.init();
    }
}

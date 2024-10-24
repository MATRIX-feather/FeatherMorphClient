package xyz.nifeather.morph.testserver;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.morph.network.Constants;
import xyz.nifeather.morph.client.network.payload.MorphCommandPayload;
import xyz.nifeather.morph.client.network.payload.MorphInitChannelPayload;
import xyz.nifeather.morph.client.network.payload.MorphVersionChannelPayload;
import xyz.nifeather.morph.misc.SharedValues;

public class VirtualServer
{
    public static VirtualServer instance;

    public VirtualServer()
    {
        instance = this;
    }

    public static final Logger LOGGER = LoggerFactory.getLogger("FeatherMorph$TestServer");

    public void init()
    {
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStart);
    }

    @Nullable
    public static MinecraftServer server;

    private final FabricClientHandler clientHandler = new FabricClientHandler();

    public final FabricMorphManager morphManager = new FabricMorphManager();

    private void onServerStart(MinecraftServer startingServer)
    {
        if (!SharedValues.allowSinglePlayerDebugging)
        {
            LOGGER.error("SinglePlayer debug is disabled.");
            return;
        }

        ServerPlayNetworking.registerGlobalReceiver(MorphInitChannelPayload.id, this::onInitPayload);
        ServerPlayNetworking.registerGlobalReceiver(MorphVersionChannelPayload.id, this::onApiPayload);
        ServerPlayNetworking.registerGlobalReceiver(MorphCommandPayload.id, this::onPlayCommandPayload);

        server = startingServer;
    }

    private void onServerStop(MinecraftServer server)
    {
        server = null;
        morphManager.dispose();

        ServerPlayNetworking.unregisterGlobalReceiver(MorphInitChannelPayload.id.id());
        ServerPlayNetworking.unregisterGlobalReceiver(MorphVersionChannelPayload.id.id());
        ServerPlayNetworking.unregisterGlobalReceiver(MorphCommandPayload.id.id());
    }

    private void onPlayCommandPayload(MorphCommandPayload morphCommandPayload, ServerPlayNetworking.Context context)
    {
        clientHandler.onCommandPayload(morphCommandPayload, context);
    }

    private void onInitPayload(MorphInitChannelPayload packet, ServerPlayNetworking.Context context)
    {
        var player = context.player();
        LOGGER.info("On init payload! from " + player);

        var payload = new MorphInitChannelPayload("Hello");

        ServerPlayNetworking.send(player, payload);
    }

    private void onApiPayload(MorphVersionChannelPayload morphVersionChannelPayload, ServerPlayNetworking.Context context)
    {
        var player = context.player();
        LOGGER.info("%s logged in with api version %s!".formatted(player.getName(), morphVersionChannelPayload.getProtocolVersion()));

        var payload = new MorphVersionChannelPayload(Constants.PROTOCOL_VERSION);
        ServerPlayNetworking.send(player, payload);
    }
}

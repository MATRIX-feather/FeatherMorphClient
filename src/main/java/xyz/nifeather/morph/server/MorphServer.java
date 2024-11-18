package xyz.nifeather.morph.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.morph.network.Constants;
import xyz.nifeather.morph.shared.payload.MorphCommandPayload;
import xyz.nifeather.morph.shared.payload.MorphInitChannelPayload;
import xyz.nifeather.morph.shared.SharedValues;
import xyz.nifeather.morph.shared.payload.MorphVersionChannelPayload;

public class MorphServer
{
    public static MorphServer instance;

    public MorphServer()
    {
        instance = this;
    }

    public static final Logger LOGGER = LoggerFactory.getLogger("FeatherMorph$TestServer");

    public void init()
    {
        PayloadTypeRegistry.playS2C().register(MorphInitChannelPayload.id, MorphInitChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MorphVersionChannelPayload.id, MorphVersionChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MorphCommandPayload.id, MorphCommandPayload.CODEC);

        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStart);
    }

    @Nullable
    public static MinecraftServer server;

    public final FabricClientHandler clientHandler = new FabricClientHandler();

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

    private void onServerStop(MinecraftServer mcServer)
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

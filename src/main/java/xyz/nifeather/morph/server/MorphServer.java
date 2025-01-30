package xyz.nifeather.morph.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.morph.network.Constants;
import xyz.nifeather.morph.shared.payload.*;
import xyz.nifeather.morph.shared.SharedValues;

public class MorphServer
{
    public static MorphServer instance;

    public MorphServer()
    {
        instance = this;
    }

    public static final Logger LOGGER = LoggerFactory.getLogger("FeatherMorph$TestServer");

    public void onModLoad()
    {
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStart);

        CommandRegistrationCallback.EVENT.register(this::onCommandRegister);
    }

    private void onCommandRegister(CommandDispatcher<ServerCommandSource> dispatcher,
                                   CommandRegistryAccess registryAccess,
                                   CommandManager.RegistrationEnvironment environment)
    {
        dispatcher.register(
                CommandManager.literal("morph")
                        .then(
                                CommandManager.argument("id", StringArgumentType.greedyString())
                                        .executes(ctx ->
                                        {
                                            if (!ctx.getSource().isExecutedByPlayer())
                                            {
                                                ctx.getSource().sendError(Text.literal("You must be a player to use this command"));
                                                return 0;
                                            }

                                            var executor = ctx.getSource().getPlayerOrThrow();

                                            String id = StringArgumentType.getString(ctx, "id");

                                            morphManager.morph(executor, id);

                                            return 1;
                                        })
                        )
        );

        dispatcher.register(
                CommandManager.literal("unmorph")
                        .executes(ctx ->
                        {
                            if (!ctx.getSource().isExecutedByPlayer())
                            {
                                ctx.getSource().sendError(Text.literal("You must be a player to use this command"));
                                return 0;
                            }

                            var executor = ctx.getSource().getPlayerOrThrow();

                            morphManager.unMorph(executor);

                            return 1;
                        })
        );
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

        var payload = new MorphInitChannelPayload(SharedValues.newProtocolIdentify);

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

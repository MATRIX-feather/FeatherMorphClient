package xyz.nifeather.morph.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.morph.network.Constants;
import xiamomc.pluginbase.XiaMoJavaPlugin;
import xyz.nifeather.morph.shared.SharedValues;
import xyz.nifeather.morph.shared.payload.MorphCommandPayload;
import xyz.nifeather.morph.shared.payload.MorphInitChannelPayload;
import xyz.nifeather.morph.shared.payload.MorphVersionChannelPayload;

public class FeatherMorphFabricMain extends XiaMoJavaPlugin
{
    public static String pluginNamespace()
    {
        return "feathermorph_fabric_main";
    }

    @Override
    public String namespace()
    {
        return pluginNamespace();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("FeatherMorph$FabricMain");

    @Override
    protected Logger getSLF4JLogger()
    {
        return LOGGER;
    }

    @Nullable
    private Runnable mainLoop;

    @Override
    public void startMainLoop(Runnable r)
    {
        logger.info("START MAIN LOOP!");
        this.mainLoop = r;
    }

    @Override
    public void runAsync(Runnable r)
    {
        Util.getMainWorkerExecutor().execute(r);
    }

    public FabricClientHandler clientHandler;
    public FabricMorphManager morphManager;

    @Override
    protected void enable()
    {
        ServerPlayNetworking.registerGlobalReceiver(MorphInitChannelPayload.id, this::onInitPayload);
        ServerPlayNetworking.registerGlobalReceiver(MorphVersionChannelPayload.id, this::onApiPayload);
        ServerPlayNetworking.registerGlobalReceiver(MorphCommandPayload.id, this::onPlayCommandPayload);

        dependencyManager.cache(morphManager = new FabricMorphManager());
        dependencyManager.cache(clientHandler = new FabricClientHandler());
    }

    @Override
    protected void disable()
    {
        ServerPlayNetworking.unregisterGlobalReceiver(MorphInitChannelPayload.id.id());
        ServerPlayNetworking.unregisterGlobalReceiver(MorphVersionChannelPayload.id.id());
        ServerPlayNetworking.unregisterGlobalReceiver(MorphCommandPayload.id.id());

        morphManager.dispose();
    }

    //region Command register

    public void onCommandRegister(CommandDispatcher<ServerCommandSource> dispatcher,
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


    //endregion Command register

    //region Payload handle

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

    //endregion Payload handle

    public void tick(MinecraftServer tickingServer)
    {
        if (mainLoop != null)
        {
            mainLoop.run();

            //for (ServerPlayerEntity serverPlayerEntity : tickingServer.getPlayerManager().getPlayerList())
            //    serverPlayerEntity.sendMessage(Text.literal("" + currentTick + " :: " + this.schedules.size() + " :: cancel? " + cancelSchedules), true);
        }
    }
}

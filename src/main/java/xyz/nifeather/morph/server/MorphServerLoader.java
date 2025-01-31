package xyz.nifeather.morph.server;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.shared.SharedValues;

public class MorphServerLoader
{
    public static final Logger LOGGER = LoggerFactory.getLogger("FeatherMorph$MorphServerLoader");

    public void onModLoad()
    {
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStart);

        CommandRegistrationCallback.EVENT.register(this::onCommandRegister);

        ServerTickEvents.START_SERVER_TICK.register(Identifier.of("feathermorph_fabric_server", "server_tick") ,this::onServerTick);
    }

    @Nullable
    private CommandRegistrationContext registrationContext;

    public void onCommandRegister(CommandDispatcher<ServerCommandSource> dispatcher,
                                  CommandRegistryAccess registryAccess,
                                  CommandManager.RegistrationEnvironment environment)
    {
        LOGGER.info("Caching CommandRegistrationContext as we register commands later.");

        this.registrationContext = new CommandRegistrationContext(dispatcher, registryAccess, environment);
    }

    @Nullable
    public static MinecraftServer mcserver;

    @Nullable
    private FeatherMorphFabricMain fabricMain;

    private void onServerStart(MinecraftServer startingServer)
    {
        if (!SharedValues.allowSinglePlayerDebugging)
        {
            LOGGER.error("SinglePlayer debug is disabled.");
            return;
        }

        var newInstance = new FeatherMorphFabricMain();
        newInstance.enablePlugin();

        if (this.registrationContext != null)
        {
            newInstance.onCommandRegister(registrationContext.dispatcher(),
                    registrationContext.registryAccess(),
                    registrationContext.environment());
        }

        this.registrationContext = null;
        this.fabricMain = newInstance;
        mcserver = startingServer;
    }

    private void onServerTick(MinecraftServer minecraftServer)
    {
        if (fabricMain != null)
            fabricMain.tick(minecraftServer);
    }

    private void onServerStop(MinecraftServer mcServer)
    {
        if (fabricMain != null)
            fabricMain.disablePlugin();

        fabricMain = null;
        mcserver = null;
    }
}

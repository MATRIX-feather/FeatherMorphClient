package xyz.nifeather.morph.testserver;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xiamomc.morph.network.BasicClientHandler;
import xiamomc.morph.network.InitializeState;
import xiamomc.morph.network.PlayerOptions;
import xiamomc.morph.network.commands.C2S.*;
import xiamomc.morph.network.commands.CommandRegistries;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;
import xiamomc.morph.network.commands.S2C.S2CAnimationCommand;
import xiamomc.morph.network.commands.S2C.S2CCurrentCommand;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapAddCommand;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapRemoveCommand;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapSyncCommand;
import xiamomc.morph.network.commands.S2C.map.S2CPartialMapCommand;
import xiamomc.morph.network.commands.S2C.query.QueryType;
import xiamomc.morph.network.commands.S2C.query.S2CQueryCommand;
import xiamomc.morph.network.commands.S2C.set.S2CSetAvailableAnimationsCommand;
import xiamomc.morph.network.commands.S2C.set.S2CSetSelfViewingCommand;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.client.AnimationNames;
import xyz.nifeather.morph.client.network.payload.MorphCommandPayload;

import java.util.List;
import java.util.Map;

public class FabricClientHandler implements BasicClientHandler<ServerPlayerEntity>
{
    private final CommandRegistries registries = new CommandRegistries();

    public FabricClientHandler()
    {
        registries.registerC2S(C2SCommandNames.Initial, a -> new C2SInitialCommand())
                .registerC2S(C2SCommandNames.Morph, C2SMorphCommand::new)
                .registerC2S(C2SCommandNames.Skill, a -> new C2SSkillCommand())
                .registerC2S(C2SCommandNames.Option, C2SOptionCommand::fromString)
                .registerC2S(C2SCommandNames.ToggleSelf, a -> new C2SToggleSelfCommand(C2SToggleSelfCommand.SelfViewMode.fromString(a)))
                .registerC2S(C2SCommandNames.Unmorph, a -> new C2SUnmorphCommand())
                .registerC2S(C2SCommandNames.Request, C2SRequestCommand::new)
                .registerC2S("animation", C2SAnimationCommand::new);
    }

    private final Logger logger = VirtualServer.LOGGER;

    private final Bindable<Boolean> allowClient = new Bindable<>(true);
    private final Bindable<Boolean> logInComingPackets = new Bindable<>(true);

    private void logPacket(boolean isOutGoingPacket, ServerPlayerEntity player, String channel, String data, int size)
    {
        var arrow = isOutGoingPacket ? " -> " : " <- ";

        String builder = channel + arrow
                + player.getName().getLiteralString()
                + " :: "
                + "'%s'".formatted(data)
                + " (≈ %s bytes)".formatted(size);

        logger.info(builder);
    }

    public void onCommandPayload(MorphCommandPayload morphCommandPayload, ServerPlayNetworking.Context context)
    {
        if (!allowClient.get()) return;

        var player = context.player();
        var input = morphCommandPayload.content();
/*
        //在API检查完成之前忽略客户端的所有指令
        if (this.getPlayerConnectionState(player).worseThan(InitializeState.API_CHECKED))
        {
            return;
        }
*/
        if (logInComingPackets.get())
            logPacket(false, player, morphCommandPayload.getId().id().toString(), input, input.length());

        var str = input.split(" ", 2);

        if (str.length < 1)
        {
            logger.warn("Incomplete server command: " + input);
            return;
        }

        var baseCommand = str[0];
        var c2sCommand = registries.createC2SCommand(baseCommand, str.length == 2 ? str[1] : "");

        if (c2sCommand != null)
        {
            c2sCommand.setOwner(player);
            c2sCommand.onCommand(this);
        }
        else
        {
            logger.warn("Unknown server command: " + baseCommand);
        }
    }

    protected boolean clientConnected(ServerPlayerEntity player)
    {
        return true;
    }

    private boolean sendCommand(ServerPlayerEntity player, AbstractS2CCommand<?> command, boolean forceSend)
    {
        var cmd = command.buildCommand();
        if (cmd == null || cmd.isEmpty() || cmd.isBlank()) return false;

        if ((!allowClient.get() || !this.clientConnected(player)) && !forceSend) return false;

        logPacket(true, player, MorphCommandPayload.id.id().toString(), cmd, cmd.length());

        var payload = new MorphCommandPayload(cmd);

        ServerPlayNetworking.send(player, payload);
        return true;
    }

    @Override
    public boolean sendCommand(ServerPlayerEntity player, AbstractS2CCommand<?> basicS2CCommand)
    {
        return this.sendCommand(player, basicS2CCommand, false);
    }

    /**
     * 获取某一玩家的客户端版本
     *
     * @param ServerPlayerEntity 目标玩家
     * @return 此玩家的客户端版本
     */
    @Override
    public int getPlayerVersion(ServerPlayerEntity ServerPlayerEntity)
    {
        return 0;
    }

    /**
     * 获取所有已连接的玩家
     *
     * @return 一个包含所有已连接玩家的列表
     * @apiNote 此列表可能包含已连接但未初始化的玩家
     */
    @Override
    public List<ServerPlayerEntity> getConnectedPlayers()
    {
        return List.of();
    }

    /**
     * 获取某一玩家的连接状态
     *
     * @param ServerPlayerEntity 目标玩家
     * @return 此玩家的连接状态
     */
    @Override
    public InitializeState getInitializeState(ServerPlayerEntity ServerPlayerEntity)
    {
        return null;
    }

    /**
     * 检查玩家的客户端是否已连接并初始化
     *
     * @param ServerPlayerEntity 目标玩家
     * @return 此玩家是否已经初始化
     */
    @Override
    public boolean isPlayerInitialized(ServerPlayerEntity ServerPlayerEntity)
    {
        return false;
    }

    /**
     * 检查玩家的连接状态
     *
     * @param ServerPlayerEntity 目标玩家
     * @return 此玩家的连接状态
     */
    @Override
    public boolean isPlayerConnected(ServerPlayerEntity ServerPlayerEntity)
    {
        return false;
    }

    /**
     * 断开与玩家的初始化连接
     *
     * @param ServerPlayerEntity 目标玩家
     */
    @Override
    public void disconnect(ServerPlayerEntity ServerPlayerEntity)
    {
    }

    /**
     * 获取玩家的某个配置
     *
     * @param ServerPlayerEntity 目标玩家
     */
    @Override
    public @Nullable PlayerOptions<ServerPlayerEntity> getPlayerOption(ServerPlayerEntity ServerPlayerEntity)
    {
        VirtualServer.LOGGER.warn("getPlayerOption is not implemented yet.");
        return null;
    }

    @Override
    public void onInitialCommand(C2SInitialCommand command)
    {
        ServerPlayerEntity player = command.getOwner();
        var morphManager = VirtualServer.instance.morphManager;

        var unlocked = morphManager.getUnlockedDisguises(player);
        var cmd = new S2CQueryCommand(QueryType.SET, unlocked.toArray(new String[0]));

        this.sendCommand(player, cmd);
        this.sendCommand(player, new S2CSetSelfViewingCommand(true));

        Map<Integer, String> renderMap = new Object2ObjectOpenHashMap<>();

        for (DisguiseSession session : morphManager.listAllSession())
            renderMap.put(session.player().getId(), session.disguiseIdentifier());

        this.sendCommand(player, new S2CRenderMapSyncCommand(renderMap));

        Map<Integer, String> revealMap = new Object2ObjectOpenHashMap<>();

        for (DisguiseSession session : morphManager.listAllSession())
            revealMap.put(session.player().getId(), session.player().getName().getLiteralString());

        this.sendCommand(player, new S2CPartialMapCommand(revealMap));
    }

    @Override
    public void onMorphCommand(C2SMorphCommand command)
    {
        ServerPlayerEntity player = command.getOwner();
        String disguiseId = command.getArgumentAt(0, "");

        var morphManager = VirtualServer.instance.morphManager;
        morphManager.morph(player, disguiseId);
    }

    @Override
    public void onOptionCommand(C2SOptionCommand command)
    {
    }

    @Override
    public void onSkillCommand(C2SSkillCommand command)
    {
    }

    @Override
    public void onToggleSelfCommand(C2SToggleSelfCommand command)
    {
        ServerPlayerEntity player = command.getOwner();
        var val = command.getSelfViewMode();

        switch (val)
        {
            case ON, CLIENT_ON -> sendCommand(player, new S2CSetSelfViewingCommand(true));
            default -> sendCommand(player, new S2CSetSelfViewingCommand(false));
        }
    }

    @Override
    public void onUnmorphCommand(C2SUnmorphCommand command)
    {
        ServerPlayerEntity player = command.getOwner();

        var morphManager = VirtualServer.instance.morphManager;
        morphManager.unMorph(player);
    }

    @Override
    public void onRequestCommand(C2SRequestCommand command)
    {
    }

    @Override
    public void onAnimationCommand(C2SAnimationCommand command)
    {
        ServerPlayerEntity player = command.getOwner();
        this.sendCommand(player, new S2CAnimationCommand(command.getAnimationId()));
    }
}

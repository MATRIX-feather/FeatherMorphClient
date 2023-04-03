package xiamomc.morph.client;

import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.network.commands.ClientSetEquipCommand;
import xiamomc.morph.network.BasicServerHandler;
import xiamomc.morph.network.Constants;
import xiamomc.morph.network.commands.C2S.*;
import xiamomc.morph.network.commands.CommandRegistries;
import xiamomc.morph.network.commands.S2C.*;
import xiamomc.morph.network.commands.S2C.query.S2CQueryCommand;
import xiamomc.morph.network.commands.S2C.set.*;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.morph.client.config.ModConfigData;

import java.nio.charset.StandardCharsets;

public class ServerHandler extends MorphClientObject implements BasicServerHandler<PlayerEntity>
{
    private final MorphClient client;

    private final CommandRegistries registries = new CommandRegistries();

    public ServerHandler(MorphClient client)
    {
        this.client = client;
    }

    private final S2CSetCommandsAgent agent = new S2CSetCommandsAgent();

    @Initializer
    private void load()
    {
        agent.register(S2CCommandNames.SetFakeEquip, ClientSetEquipCommand::from);

        registries.registerS2C(S2CCommandNames.Current, S2CCurrentCommand::new)
                .registerS2C(S2CCommandNames.Query, S2CQueryCommand::from)
                .registerS2C(S2CCommandNames.ReAuth, a -> new S2CReAuthCommand())
                .registerS2C(S2CCommandNames.UnAuth, a -> new S2CUnAuthCommand())
                .registerS2C(S2CCommandNames.SwapHands, a -> new S2CSwapCommand())
                .registerS2C(S2CCommandNames.BaseSet, agent::getCommand);
    }

    //region Common

    private static final String morphNameSpace = "morphplugin";

    public static Identifier initializeChannelIdentifier = new Identifier(morphNameSpace, "init");
    public static Identifier versionChannelIdentifier = new Identifier(morphNameSpace, "version");
    public static Identifier commandChannelIdentifier = new Identifier(morphNameSpace, "commands");

    @Resolved
    private ClientMorphManager morphManager;

    @Resolved
    private DisguiseSyncer syncer;

    @Resolved
    private ModConfigData config;

    @Resolved
    private ClientSkillHandler skillHandler;

    //endregion

    //region Network

    public boolean serverReady()
    {
        return serverReady.get();
    }

    private int serverVersion = -1;

    public int getServerVersion()
    {
        return serverVersion;
    }

    public boolean serverApiMatch()
    {
        return this.getServerVersion() == getImplmentingApiVersion();
    }

    private String readStringfromByte(ByteBuf buf)
    {
        return buf.resetReaderIndex().readCharSequence(buf.readableBytes(), StandardCharsets.UTF_8).toString();
    }

    private PacketByteBuf fromString(String str)
    {
        var packet = PacketByteBufs.create();

        packet.writeCharSequence(str, StandardCharsets.UTF_8);
        return packet;
    }

    @Override
    public void connect()
    {
        this.resetServerStatus();

        ClientPlayNetworking.send(initializeChannelIdentifier, PacketByteBufs.create());
    }

    @Override
    public void disconnect()
    {
        resetServerStatus();
    }

    public boolean sendCommand(AbstractC2SCommand<?> command)
    {
        var cmd = command.buildCommand();
        if (cmd == null || cmd.isEmpty() || cmd.isBlank()) return false;

        cmd = cmd.trim();

        ClientPlayNetworking.send(commandChannelIdentifier, fromString(cmd));

        return true;
    }

    @Override
    public int getServerApiVersion()
    {
        return serverVersion;
    }

    @Override
    public int getImplmentingApiVersion()
    {
        return Constants.PROTOCOL_VERSION;
    }

    @Override
    public void onCurrentCommand(xiamomc.morph.network.commands.S2C.S2CCurrentCommand s2CCurrentCommand)
    {
        var id = s2CCurrentCommand.getDisguiseIdentifier();
        morphManager.setCurrent(id.equals("null") ? null : id);
    }

    @Override
    public void onReAuthCommand(xiamomc.morph.network.commands.S2C.S2CReAuthCommand s2CReAuthCommand)
    {
        this.disconnect();
        this.connect();
    }

    @Override
    public void onUnAuthCommand(xiamomc.morph.network.commands.S2C.S2CUnAuthCommand s2CUnAuthCommand)
    {
        this.disconnect();
    }

    @Override
    public void onSwapCommand(xiamomc.morph.network.commands.S2C.S2CSwapCommand s2CSwapCommand)
    {
        morphManager.swapHand();
    }

    @Override
    public void onQueryCommand(xiamomc.morph.network.commands.S2C.query.S2CQueryCommand s2CQueryCommand)
    {
        var diff = s2CQueryCommand.getDiff();
        switch (s2CQueryCommand.queryType())
        {
            case ADD -> morphManager.addDisguises(diff);
            case REMOVE -> morphManager.removeDisguises(diff);
            case SET -> morphManager.setDisguises(diff);
        }
    }

    @Override
    public void onSetAggressiveCommand(S2CSetAggressiveCommand s2CSetAggressiveCommand)
    {
        var aggressive = s2CSetAggressiveCommand.getArgumentAt(0, false);

        if (syncer.entity instanceof GhastEntity ghastEntity)
            ghastEntity.setShooting(aggressive);
    }

    @Override
    public void onSetFakeEquipCommand(S2CSetFakeEquipCommand<?> s2CSetEquipCommand)
    {
        if (!(s2CSetEquipCommand.getItemStack() instanceof ItemStack stack)) return;

        switch (s2CSetEquipCommand.getSlot())
        {
            case MAINHAND -> morphManager.setEquip(EquipmentSlot.MAINHAND, stack);
            case OFF_HAND -> morphManager.setEquip(EquipmentSlot.OFFHAND, stack);

            case HELMET -> morphManager.setEquip(EquipmentSlot.HEAD, stack);
            case CHESTPLATE -> morphManager.setEquip(EquipmentSlot.CHEST, stack);
            case LEGGINGS -> morphManager.setEquip(EquipmentSlot.LEGS, stack);
            case BOOTS -> morphManager.setEquip(EquipmentSlot.FEET, stack);
        }
    }

    @Override
    public void onSetDisplayingFakeEquipCommand(S2CSetDisplayingFakeEquipCommand s2CSetFakeEquipCommand)
    {
        morphManager.equipOverriden.set(s2CSetFakeEquipCommand.getArgumentAt(0, false));
    }

    @Override
    public void onSetSNbtCommand(S2CSetSNbtCommand s2CSetSNbtCommand)
    {
        try
        {
            var nbt = StringNbtReader.parse(s2CSetSNbtCommand.serializeArguments().replace("\\u003d", "="));

            morphManager.currentNbtCompound.set(nbt);
        }
        catch (CommandSyntaxException e)
        {
            //todo
        }
    }

    @Override
    public void onSetProfileCommand(S2CSetProfileCommand s2CSetProfileCommand)
    {
        try
        {
            var nbt = StringNbtReader.parse(s2CSetProfileCommand.serializeArguments());
            var profile = NbtHelper.toGameProfile(nbt);

            if (profile != null)
                this.client.schedule(() -> syncer.updateSkin(profile));
        }
        catch (Exception e)
        {
            //todo
        }
    }

    @Override
    public void onSetSelfViewIdentifierCommand(S2CSetSelfViewIdentifierCommand s2CSetSelfViewCommand)
    {
        morphManager.selfViewIdentifier.set(s2CSetSelfViewCommand.serializeArguments());
    }

    @Override
    public void onSetSkillCooldownCommand(S2CSetSkillCooldownCommand s2CSetSkillCooldownCommand)
    {
        skillHandler.setSkillCooldown(s2CSetSkillCooldownCommand.getArgumentAt(0, 0L));
    }

    @Override
    public void onSetSneakingCommand(S2CSetSneakingCommand s2CSetSneakingCommand)
    {
        serverSideSneaking = s2CSetSneakingCommand.getArgumentAt(0);
    }

    @Override
    public void onSetSelfViewingCommand(S2CSetSelfViewingCommand s2CSetToggleSelfCommand)
    {
        morphManager.selfVisibleToggled.set(s2CSetToggleSelfCommand.getArgumentAt(0));
    }

    @Override
    public void onSetModifyBoundingBox(S2CSetModifyBoundingBoxCommand s2CSetModifyBoundingBoxCommand)
    {
        modifyBoundingBox = s2CSetModifyBoundingBoxCommand.getModifyBoundingBox();

        var clientPlayer = MinecraftClient.getInstance().player;
        if (clientPlayer != null)
            clientPlayer.calculateDimensions();
    }

    public static boolean modifyBoundingBox = false;

    @Override
    public void onSetReach(S2CSetReachCommand s2CSetReachCommand)
    {
        reach = (float) (s2CSetReachCommand.getReach() / 10);
    }

    public static float reach = -1;

    public final Bindable<Boolean> serverReady = new Bindable<>(false);
    private boolean handshakeReceived;
    private boolean apiVersionChecked;

    public void resetServerStatus()
    {
        handshakeReceived = false;
        apiVersionChecked = false;

        morphManager.reset();
        updateServerStatus();
    }

    private void updateServerStatus()
    {
        serverReady.set(handshakeReceived && apiVersionChecked);
    }

    private boolean networkInitialized;

    public void initializeNetwork()
    {
        if (networkInitialized)
            throw new RuntimeException("The network has been initialized once!");

        ClientPlayConnectionEvents.INIT.register((handler, client) ->
        {
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
        {
            connect();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            disconnect();
        });

        //初始化网络
        ClientPlayNetworking.registerGlobalReceiver(initializeChannelIdentifier, (client, handler, buf, responseSender) ->
        {
            if (this.readStringfromByte(buf).equalsIgnoreCase("no"))
            {
                logger.error("Initialize failed: Denied by server");
                return;
            }

            handshakeReceived = true;
            updateServerStatus();

            ClientPlayNetworking.send(versionChannelIdentifier, fromString("" + getImplmentingApiVersion()));
            sendCommand(new C2SInitialCommand());
            sendCommand(new C2SOptionCommand(C2SOptionCommand.ClientOptions.CLIENTVIEW).setValue(config.allowClientView));
            sendCommand(new C2SOptionCommand(C2SOptionCommand.ClientOptions.HUD).setValue(config.displayDisguiseOnHud));
        });

        ClientPlayNetworking.registerGlobalReceiver(versionChannelIdentifier, (client, handler, buf, responseSender) ->
        {
            try
            {
                serverVersion = buf.readInt();
                apiVersionChecked = true;
                updateServerStatus();
            }
            catch (Exception e)
            {
                logger.error("Unable to get server API version: " + e.getMessage());
                e.printStackTrace();
            }

            logger.info("Server API version: " + serverVersion);
        });

        ClientPlayNetworking.registerGlobalReceiver(commandChannelIdentifier, (client, handler, buf, responseSender) ->
        {
            var str = readStringfromByte(buf).split(" ", 2);

            if (!serverReady.get() && !str[0].equals("reauth"))
            {
                if (config.verbosePackets)
                    logger.warn("Received command before initialize complete, not processing... ('%s')".formatted(readStringfromByte(buf)));

                return;
            }

            try
            {
                if (config.verbosePackets)
                    logger.info("Received client command: " + readStringfromByte(buf));

                if (str.length < 1) return;

                var baseName = str[0];
                var cmd = registries.createS2CCommand(baseName, str.length == 2 ? str[1] : "");

                if (cmd != null)
                    cmd.onCommand(this);
                else
                    logger.warn("Unknown client command: " + baseName);
            }
            catch (Exception e)
            {
                logger.error("发生异常：" + e.getMessage());
                e.printStackTrace();
            }
        });

        networkInitialized = true;
    }

    public static Boolean serverSideSneaking;

    //endregion Network
}

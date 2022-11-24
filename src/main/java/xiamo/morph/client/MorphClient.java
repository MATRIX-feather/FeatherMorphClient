package xiamo.morph.client;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.bindables.Bindable;
import xiamo.morph.client.config.ModConfigData;
import xiamo.morph.client.screens.disguise.DisguiseScreen;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class MorphClient implements ClientModInitializer
{
    private static final String morphNameSpace = "morphplugin";

    public static Identifier initializeChannelIdentifier = new Identifier(morphNameSpace, "init");
    public static Identifier versionChannelIdentifier = new Identifier(morphNameSpace, "version");
    public static Identifier commandChannelIdentifier = new Identifier(morphNameSpace, "commands");

    private KeyBinding toggleselfKeyBind;
    private KeyBinding executeSkillKeyBind;
    private KeyBinding unMorphKeyBind;
    private KeyBinding morphKeyBind;

    private static MorphClient instance;

    public static MorphClient getInstance()
    {
        return instance;
    }

    public MorphClient()
    {
        instance = this;
    }

    public static final Bindable<String> selectedIdentifier = new Bindable<>(null);

    public static final Bindable<String> currentIdentifier = new Bindable<>(null);

    public static final DisguiseSyncer DISGUISE_SYNCER = new DisguiseSyncer();

    private final Logger logger = LoggerFactory.getLogger("MorphClient");

    @Override
    public void onInitializeClient()
    {
        //初始化按键
        executeSkillKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.morphclient.skill", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V, "category.morphclient.keybind"
        ));

        unMorphKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.morphclient.unmorph", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN, "category.morphclient.keybind"
        ));

        morphKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.morphclient.morph", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N, "category.morphclient.keybind"
        ));

        toggleselfKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.morphclient.toggle", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT, "category.morphclient.keybind"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::updateKeys);

        //初始化配置
        if (modConfigData == null)
        {
            AutoConfig.register(ModConfigData.class, GsonConfigSerializer::new);

            configHolder = AutoConfig.getConfigHolder(ModConfigData.class);
            configHolder.load();

            modConfigData = configHolder.getConfig();
        }

        initializeNetwork();
    }

    public final Bindable<Boolean> selfVisibleToggled = new Bindable<>(false);

    private final List<String> avaliableMorphs = new ObjectArrayList<>();

    public List<String> getAvaliableMorphs()
    {
        return new ObjectArrayList<>(avaliableMorphs);
    }

    private final List<Function<List<String>, Boolean>> onGrantConsumers = new ObjectArrayList<>();
    public void onMorphGrant(Function<List<String>, Boolean> consumer)
    {
        onGrantConsumers.add(consumer);
    }

    private void invokeRevoke(List<String> diff)
    {
        var tobeRemoved = new ObjectArrayList<Function<List<String>, Boolean>>();

        onRevokeConsumers.forEach(f ->
        {
            if (!f.apply(diff)) tobeRemoved.add(f);
        });

        onRevokeConsumers.removeAll(tobeRemoved);
    }

    private void invokeGrant(List<String> diff)
    {
        var tobeRemoved = new ObjectArrayList<Function<List<String>, Boolean>>();

        onGrantConsumers.forEach(f ->
        {
            if (!f.apply(diff)) tobeRemoved.add(f);
        });

        onGrantConsumers.removeAll(tobeRemoved);
    }

    private final List<Function<List<String>, Boolean>> onRevokeConsumers = new ObjectArrayList<>();
    public void onMorphRevoke(Function<List<String>, Boolean> consumer)
    {
        onRevokeConsumers.add(consumer);
    }

    private void updateKeys(MinecraftClient client)
    {
        if (executeSkillKeyBind.wasPressed())
            sendCommand("skill");

        if (unMorphKeyBind.wasPressed())
            sendCommand("unmorph");

        if (toggleselfKeyBind.wasPressed())
        {
            var config = getModConfigData();

            boolean val = !selfVisibleToggled.get();

            updateClientView(config.allowClientView, val);
        }

        if (morphKeyBind.wasPressed())
        {
            if (client.currentScreen == null)
            {
                client.setScreen(new DisguiseScreen());
            }
        }
    }

    private boolean lastClientView;

    public void updateClientView(boolean clientViewEnabled, boolean selfViewVisible)
    {
        if (clientViewEnabled != lastClientView)
        {
            sendCommand("toggleself client " + clientViewEnabled);
            lastClientView = clientViewEnabled;
        }

        sendCommand("toggleself " + selfViewVisible);

        modConfigData.allowClientView = clientViewEnabled;
    }

    public void sendMorphCommand(String id)
    {
        if (id == null) id = "morph:unmorph";

        if ("morph:unmorph".equals(id))
            sendCommand("unmorph");
        else
            sendCommand("morph " + id);
    }


    //region Config
    private ModConfigData modConfigData;
    private ConfigHolder<ModConfigData> configHolder;

    private void onConfigSave()
    {
        configHolder.save();
    }

    public ModConfigData getModConfigData()
    {
        return modConfigData;
    }

    public ConfigBuilder getFactory(Screen parent)
    {
        ConfigBuilder builder = ConfigBuilder.create();
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory categoryGeneral = builder.getOrCreateCategory(Text.translatable("stat.generalButton"));

        categoryGeneral.addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.previewInInventory.name"), modConfigData.alwaysShowPreviewInInventory)
                        .setTooltip(Text.translatable("option.morphclient.previewInInventory.description"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> modConfigData.alwaysShowPreviewInInventory = v)
                        .build()
        );

        categoryGeneral.addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.allowClientView.name"), modConfigData.allowClientView)
                        .setTooltip(Text.translatable("option.morphclient.allowClientView.description"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.allowClientView = v;
                            updateClientView(v, selfVisibleToggled.get());
                        })
                        .build()
        );

        builder.setParentScreen(parent)
                .setTitle(Text.translatable("title.morphclient.config"))
                .transparentBackground();

        builder.setSavingRunnable(this::onConfigSave);

        return builder;
    }
    //endregion Config

    //region Network
    private int serverVersion = -1;
    private final int clientVersion = 1;

    public int getServerVersion()
    {
        return serverVersion;
    }

    public boolean serverApiMatch()
    {
        return this.getServerVersion() == clientVersion;
    }

    public int getClientVersion()
    {
        return clientVersion;
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

    public void sendCommand(String command)
    {
        if (command == null || command.isEmpty() || command.isBlank()) return;

        ClientPlayNetworking.send(commandChannelIdentifier, fromString(command));
    }

    public static final Bindable<Boolean> serverReady = new Bindable<>(false);
    private boolean handshakeReceived;
    private boolean apiVersionChecked;
    private boolean morphListReceived;

    public void resetServerStatus()
    {
        handshakeReceived = false;
        apiVersionChecked = false;
        morphListReceived = false;

        var list = new ObjectArrayList<>(avaliableMorphs);
        this.avaliableMorphs.clear();
        selectedIdentifier.set(null);
        currentIdentifier.set(null);

        updateServerStatus();

        invokeGrant(list);
    }

    private void updateServerStatus()
    {
        serverReady.set(handshakeReceived && apiVersionChecked && morphListReceived);
    }

    public void initializeClientData()
    {
        this.resetServerStatus();

        ClientPlayNetworking.send(initializeChannelIdentifier, PacketByteBufs.create());
    }

    private void initializeNetwork()
    {
        //初始化网络
        ClientPlayNetworking.registerGlobalReceiver(initializeChannelIdentifier, (client, handler, buf, responseSender) ->
        {
            if (this.readStringfromByte(buf).equalsIgnoreCase("no"))
            {
                logger.error("初始化失败：被服务器拒绝");
                return;
            }

            handshakeReceived = true;
            updateServerStatus();

            ClientPlayNetworking.send(versionChannelIdentifier, PacketByteBufs.create());
            sendCommand("initial");
            sendCommand("option clientview " + modConfigData.allowClientView);
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
                logger.error("未能获取服务器API版本：" + e.getMessage());
                e.printStackTrace();
            }

            logger.info("服务器API版本：" + serverVersion);
        });

        ClientPlayNetworking.registerGlobalReceiver(commandChannelIdentifier, (client, handler, buf, responseSender) ->
        {
            try
            {
                logger.info("收到了客户端指令：" + readStringfromByte(buf));

                var str = readStringfromByte(buf).split(" ", 3);

                if (str.length < 1) return;

                var baseName = str[0];

                switch (baseName)
                {
                    case "query" ->
                    {
                        if (str.length < 2) return;

                        var subCmdName = str[1];

                        var diff = new ObjectArrayList<>(str[2].split(" "));
                        diff.removeIf(String::isEmpty);

                        switch (subCmdName) {
                            case "add" ->
                            {
                                diff.removeIf(avaliableMorphs::contains);
                                avaliableMorphs.addAll(diff);
                                invokeGrant(diff);
                            }
                            case "remove" ->
                            {
                                avaliableMorphs.removeAll(diff);
                                invokeRevoke(diff);
                            }
                            case "set" ->
                            {
                                invokeRevoke(diff);

                                this.avaliableMorphs.clear();
                                this.avaliableMorphs.addAll(diff);

                                morphListReceived = true;
                                updateServerStatus();

                                invokeGrant(diff);
                            }
                            default -> logger.warn("未知的Query指令：" + subCmdName);
                        }
                    }
                    case "set" ->
                    {
                        if (str.length < 2) return;

                        var subCmdName = str[1];

                        switch (subCmdName)
                        {
                            case "toggleself" ->
                            {
                                if (str.length < 3) return;

                                var val = Boolean.parseBoolean(str[2]);

                                selfVisibleToggled.set(val);
                            }
                        }
                    }
                    case "reauth" ->
                    {
                        initializeClientData();
                    }
                    case "unauth" ->
                    {
                        resetServerStatus();
                    }
                    case "current" ->
                    {
                        currentIdentifier.set(str.length == 2 ? str[1] : null);
                    }
                    case "deny" ->
                    {
                        if (str.length < 2) return;

                        var subCmdName = str[1];

                        if (subCmdName.equals("morph"))
                        {
                            selectedIdentifier.triggerChange();
                            currentIdentifier.triggerChange();
                        }
                        else
                            logger.warn("未知的Deny指令：" + subCmdName);
                    }
                    default -> logger.warn("未知的客户端指令：" + baseName);
                }
            }
            catch (Exception e)
            {
                logger.error("发生异常：" + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    //endregion
}

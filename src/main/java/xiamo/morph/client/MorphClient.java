package xiamo.morph.client;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.bindables.Bindable;
import xiamo.morph.client.config.ModConfigData;
import xiamo.morph.client.graphics.MorphLocalPlayer;
import xiamo.morph.client.screens.disguise.DisguiseScreen;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
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

    public static final Bindable<String> selfViewIdentifier = new Bindable<>(null);

    public static final Bindable<NbtCompound> currentNbtCompound = new Bindable<>(null);

    public static final Bindable<Boolean> equipOverriden = new Bindable<>(false);

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

        //初始化配置
        if (modConfigData == null)
        {
            AutoConfig.register(ModConfigData.class, GsonConfigSerializer::new);

            configHolder = AutoConfig.getConfigHolder(ModConfigData.class);
            configHolder.load();

            modConfigData = configHolder.getConfig();
        }

        initializeNetwork();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientTickEvents.END_WORLD_TICK.register(this::postWorldTick);
    }

    private void postWorldTick(ClientWorld clientWorld)
    {
        DISGUISE_SYNCER.onGameTick();
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

    @Nullable
    private Boolean lastClientView = null;

    public void updateClientView(boolean clientViewEnabled, boolean selfViewVisible)
    {
        if (lastClientView == null || clientViewEnabled != lastClientView)
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
                        .setDefaultValue(true)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.allowClientView = v;

                            if (serverReady.get())
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
        selfViewIdentifier.set(null);

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
        ClientPlayConnectionEvents.INIT.register((handler, client) ->
        {
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
        {
            initializeClientData();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            resetServerStatus();
        });

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
                            case "selfview" ->
                            {
                                if (str.length < 3) return;

                                var identifier = str[2];

                                selfViewIdentifier.set(identifier);
                            }
                            case "fake_equip" ->
                            {
                                if (str.length < 3) return;

                                var value = Boolean.valueOf(str[2]);

                                equipOverriden.set(value);
                            }
                            case "equip" ->
                            {
                                if (str.length < 3) return;

                                var dat = str[2].split(" ", 2);

                                logger.info("get dat: " + dat.length);
                                if (dat.length != 2) return;
                                var currentMob = EntityCache.getEntity(selfViewIdentifier.get());

                                if (currentMob == null) return;

                                var stack = jsonToStack(dat[1]);

                                if (stack == null) return;

                                logger.info("updating equip!");

                                switch (dat[0])
                                {
                                    case "mainhand" -> equipmentSlotItemStackMap.put(EquipmentSlot.MAINHAND, stack);
                                    case "offhand" -> equipmentSlotItemStackMap.put(EquipmentSlot.OFFHAND, stack);

                                    case "helmet" -> equipmentSlotItemStackMap.put(EquipmentSlot.HEAD, stack);
                                    case "chestplate" -> equipmentSlotItemStackMap.put(EquipmentSlot.CHEST, stack);
                                    case "leggings" -> equipmentSlotItemStackMap.put(EquipmentSlot.LEGS, stack);
                                    case "boots" -> equipmentSlotItemStackMap.put(EquipmentSlot.FEET, stack);
                                }
                            }
                            case "nbt" ->
                            {
                                if (str.length < 3) return;

                                var nbt = StringNbtReader.parse(str[2].replace("\\u003d", "="));

                                currentNbtCompound.set(nbt);
                            }
                            case "profile" ->
                            {
                                if (str.length < 3) return;

                                var nbt = StringNbtReader.parse(str[2]);
                                var profile = NbtHelper.toGameProfile(nbt);

                                if (profile != null)
                                {
                                    if (DISGUISE_SYNCER.entity instanceof MorphLocalPlayer disguise)
                                        this.schedule(c -> disguise.updateSkin(profile));
                                    else
                                        logger.warn("Received a GameProfile while current disguise is not a player! : " + profile);
                                }
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
                        var val = str.length == 2 ? str[1] : null;
                        currentIdentifier.set(val);

                        equipOverriden.set(false);
                        selfViewIdentifier.set(null);
                        equipmentSlotItemStackMap.clear();
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

    private final ItemStack air = ItemStack.EMPTY;

    private final Map<EquipmentSlot, ItemStack> equipmentSlotItemStackMap = new Object2ObjectOpenHashMap<>();

    public ItemStack getOverridedItemStackOn(EquipmentSlot slot)
    {
        return equipmentSlotItemStackMap.getOrDefault(slot, air);
    }

    @Nullable
    private ItemStack jsonToStack(String rawJson)
    {
        var item = ItemStack.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseString(rawJson));

        if (item.result().isPresent())
            return item.result().get().getFirst();

        return null;
    }
    //endregion

    //region tick相关

    private long currentTick = 0;

    private void tick(MinecraftClient client)
    {
        currentTick += 1;

        if (shouldAbortTicking) return;

        var schedules = new ArrayList<>(this.schedules);
        schedules.forEach(c ->
        {
            if (currentTick - c.TickScheduled >= c.Delay)
            {
                this.schedules.remove(c);

                if (c.isCanceled()) return;

                //logger.info("执行：" + c + "，当前TICK：" + currentTick);\
                if (c.isAsync)
                    throw new NotImplementedException();
                else
                    runFunction(c);
            }
        });

        schedules.clear();

        this.updateKeys(client);
    }

    private void runFunction(ScheduleInfo c)
    {
        try
        {
            c.Function.accept(null);
        }
        catch (Exception e)
        {
            this.onExceptionCaught(e, c);
        }
    }

    //region tick异常捕捉与处理

    //一秒内最多能接受多少异常
    protected final int exceptionLimit = 5;

    //已经捕获的异常
    private int exceptionCaught = 0;

    //是否应该中断tick
    private boolean shouldAbortTicking = false;

    private void onExceptionCaught(Exception exception, ScheduleInfo scheduleInfo)
    {
        if (exception == null) return;

        exceptionCaught += 1;

        logger.warn("执行" + scheduleInfo + "时捕获到未处理的异常：");
        exception.printStackTrace();

        if (exceptionCaught >= exceptionLimit)
        {
            logger.error("可接受异常已到达最大限制");
            this.shouldAbortTicking = true;
        }
    }

    private void processExceptionCount()
    {
        exceptionCaught -= 1;

        this.schedule(c -> processExceptionCount(), 5);
    }

    //endregion tick异常捕捉与处理

    //endregion tick相关

    //region Schedules

    private final List<ScheduleInfo> schedules = new ObjectArrayList<>();

    public ScheduleInfo schedule(Consumer<?> runnable)
    {
        return this.schedule(runnable, 1);
    }

    public ScheduleInfo schedule(Consumer<?> function, int delay)
    {
        return this.schedule(function, delay, false);
    }

    public ScheduleInfo schedule(Consumer<?> function, int delay, boolean async)
    {
        var si = new ScheduleInfo(function, delay, currentTick, async);

        synchronized (schedules)
        {
            //Logger.info("添加：" + si + "，当前TICK：" + currentTick);
            schedules.add(si);
        }

        return si;
    }

    public long getCurrentTick()
    {
        return currentTick;
    }
    //endregion Schedules
}

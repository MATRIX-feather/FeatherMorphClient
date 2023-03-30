package xiamomc.morph.client;

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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.config.ModConfigData;
import xiamomc.morph.client.graphics.ModelWorkarounds;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.morph.client.network.commands.C2S.*;
import xiamomc.morph.client.screens.disguise.WaitingForServerScreen;
import xiamomc.morph.network.Constants;
import xiamomc.pluginbase.AbstractSchedulablePlugin;
import xiamomc.pluginbase.ScheduleInfo;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class MorphClient extends AbstractSchedulablePlugin implements ClientModInitializer
{
    private static MorphClient instance;

    public static MorphClient getInstance()
    {
        return instance;
    }

    public DisguiseSyncer disguiseSyncer;

    public static final Logger LOGGER = LoggerFactory.getLogger("MorphClient");

    private KeyBinding toggleselfKeyBind;
    private KeyBinding executeSkillKeyBind;
    private KeyBinding unMorphKeyBind;
    private KeyBinding morphKeyBind;
    private KeyBinding resetCacheKeybind;

    @Override
    public String getNameSpace()
    {
        return getClientNameSpace();
    }

    public static String getClientNameSpace()
    {
        return "morphclient";
    }

    @Override
    public Logger getSLF4JLogger()
    {
        return LOGGER;
    }

    public MorphClient()
    {
        instance = this;
    }

    public ClientMorphManager morphManager;
    public ServerHandler serverHandler;
    private ClientSkillHandler skillHandler;

    @Override
    public void onInitializeClient()
    {
        Constants.initialize(false);

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

        resetCacheKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.morphclient.reset_cache", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, "category.morphclient.keybind"
        ));

        //初始化配置
        if (modConfigData == null)
        {
            AutoConfig.register(ModConfigData.class, GsonConfigSerializer::new);

            configHolder = AutoConfig.getConfigHolder(ModConfigData.class);
            configHolder.load();

            modConfigData = configHolder.getConfig();
        }

        dependencyManager.cache(this);
        dependencyManager.cache(morphManager = new ClientMorphManager());
        dependencyManager.cache(serverHandler = new ServerHandler(this));
        dependencyManager.cache(disguiseSyncer = new DisguiseSyncer());
        dependencyManager.cache(skillHandler = new ClientSkillHandler());
        dependencyManager.cache(modConfigData);

        serverHandler.initializeNetwork();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientTickEvents.END_WORLD_TICK.register(this::postWorldTick);

        modelWorkarounds = ModelWorkarounds.getInstance();
    }

    private ModelWorkarounds modelWorkarounds;

    private void postWorldTick(ClientWorld clientWorld)
    {
        disguiseSyncer.onGameTick();
    }

    private void updateKeys(MinecraftClient client)
    {
        if (executeSkillKeyBind.wasPressed() && skillHandler.getCurrentCooldown() == 0)
        {
            skillHandler.setSkillCooldown(skillHandler.getSkillCooldown());
            serverHandler.sendCommand(new C2SSkillCommand());
        }

        if (unMorphKeyBind.wasPressed())
            serverHandler.sendCommand(new C2SUnmorphCommand());

        if (toggleselfKeyBind.wasPressed())
        {
            var config = getModConfigData();

            boolean val = !morphManager.selfVisibleToggled.get();

            updateClientView(config.allowClientView, val);
        }

        if (morphKeyBind.wasPressed())
        {
            var player = client.player;

            if (player != null && player.input != null && player.input.sneaking)
            {
                serverHandler.sendCommand(new C2SMorphCommand());
            }
            else if (client.currentScreen == null)
            {
                client.setScreen(new WaitingForServerScreen());
            }
        }

        if (resetCacheKeybind.wasPressed())
        {
            EntityCache.clearCache();
            modelWorkarounds.initWorkarounds();
        }
    }

    @Nullable
    private Boolean lastClientView = null;

    public void updateClientView(boolean clientViewEnabled, boolean selfViewVisible)
    {
        if (lastClientView == null || clientViewEnabled != lastClientView)
        {
            serverHandler.sendCommand(new C2SToggleSelfCommand("client", clientViewEnabled + ""));
            lastClientView = clientViewEnabled;
        }

        serverHandler.sendCommand(new C2SToggleSelfCommand(selfViewVisible + ""));

        modConfigData.allowClientView = clientViewEnabled;
    }

    public void sendMorphCommand(String id)
    {
        if (id == null) id = "morph:unmorph";

        if ("morph:unmorph".equals(id))
            serverHandler.sendCommand(new C2SUnmorphCommand());
        else
            serverHandler.sendCommand(new C2SMorphCommand(id));
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
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.changeCameraHeight.name"), modConfigData.changeCameraHeight)
                        .setTooltip(Text.translatable("option.morphclient.changeCameraHeight.description"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.changeCameraHeight = v;
                        })
                        .build()
        );

        categoryGeneral.addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.allowClientView.name"), modConfigData.allowClientView)
                        .setTooltip(Text.translatable("option.morphclient.allowClientView.description"))
                        .setDefaultValue(true)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.allowClientView = v;

                            if (serverHandler.serverReady())
                                updateClientView(v, morphManager.selfVisibleToggled.get());
                        })
                        .build()
        );

        categoryGeneral.addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.displayDisguiseOnHud.name"), modConfigData.displayDisguiseOnHud)
                        .setTooltip(Text.translatable("option.morphclient.displayDisguiseOnHud.description"))
                        .setDefaultValue(true)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.displayDisguiseOnHud = v;

                            if (serverHandler.serverReady())
                                serverHandler.sendCommand(new C2SOptionCommand(C2SOptionCommand.ClientOptions.HUD).setValue(v));
                        })
                        .build()
        );

        categoryGeneral.addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.verbosePackets.name"), modConfigData.verbosePackets)
                        .setTooltip(Text.translatable("option.morphclient.verbosePackets.description"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.verbosePackets = v;
                        })
                        .build()
        );

        categoryGeneral.addEntry(
                entryBuilder.startEnumSelector(Text.translatable("option.morphclient.easing.name"), Easing.class, modConfigData.easing)
                        .setTooltip(Text.translatable("option.morphclient.easing.description"))
                        .setDefaultValue(Easing.OutQuint)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.easing = v;
                        })
                        .build()
        );

        categoryGeneral.addEntry(
                entryBuilder.startIntField(Text.translatable("option.morphclient.animationTime.name"), modConfigData.duration)
                        .setTooltip(Text.translatable("option.morphclient.animationTime.description"))
                        .setDefaultValue(450)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.duration = v;
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

                //LOGGER.info("执行：" + c + "，当前TICK：" + currentTick);\
                if (c.isAsync)
                    CompletableFuture.runAsync(() -> runFunction(c));
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
            c.Function.run();
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

    private synchronized void onExceptionCaught(Exception exception, ScheduleInfo scheduleInfo)
    {
        if (exception == null) return;

        exceptionCaught += 1;

        LOGGER.warn("执行" + scheduleInfo + "时捕获到未处理的异常：");
        exception.printStackTrace();

        if (exceptionCaught >= exceptionLimit)
        {
            LOGGER.error("可接受异常已到达最大限制");
            this.shouldAbortTicking = true;
        }
    }

    private void processExceptionCount()
    {
        exceptionCaught -= 1;

        this.schedule(this::processExceptionCount, 5);
    }

    //endregion tick异常捕捉与处理

    //endregion tick相关

    //region Schedules

    public long getCurrentTick()
    {
        return currentTick;
    }

    @Override
    public boolean acceptSchedules()
    {
        return true;
    }

    //endregion Schedules
}

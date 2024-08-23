package xiamomc.morph.client;

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
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.config.ModConfigData;
import xiamomc.morph.client.graphics.EntityRendererHelper;
import xiamomc.morph.client.graphics.ModelWorkarounds;
import xiamomc.morph.client.graphics.hud.HudRenderHelper;
import xiamomc.morph.client.graphics.toasts.DisguiseEntryToast;
import xiamomc.morph.client.graphics.toasts.RequestToast;
import xiamomc.morph.client.screens.WaitingForServerScreen;
import xiamomc.morph.client.screens.disguise.DisguiseScreen;
import xiamomc.morph.client.screens.emote.EmoteScreen;
import xiamomc.morph.client.syncers.DisguiseSyncer;
import xiamomc.morph.client.syncers.animations.AnimHandlerIndex;
import xiamomc.morph.network.Constants;
import xiamomc.morph.network.commands.C2S.*;
import xiamomc.morph.network.commands.S2C.S2CRequestCommand;
import xiamomc.pluginbase.AbstractSchedulablePlugin;
import xiamomc.pluginbase.ScheduleInfo;

import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class MorphClient extends AbstractSchedulablePlugin implements ClientModInitializer
{
    private static MorphClient instance;

    public static MorphClient getInstance()
    {
        return instance;
    }

    public static final String UNMORPH_STIRNG = "morph:unmorph";

    public static final Logger LOGGER = LoggerFactory.getLogger("FeatherMorph");

    private KeyBinding toggleselfKeyBind;
    private KeyBinding executeSkillKeyBind;
    private KeyBinding unMorphKeyBind;
    private KeyBinding morphKeyBind;
    private KeyBinding resetCacheKeybind;
    private KeyBinding displayOwnerBind;
    private KeyBinding emoteKeyBind;

    private KeyBinding testKeyBindGrant;
    private KeyBinding testKeyBindLost;

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
    private DisguiseInstanceTracker disguiseTracker;

    private final AnimHandlerIndex animHandlerIndex = new AnimHandlerIndex();

    private final boolean debugToasts = false;

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

        if (debugToasts)
        {
            testKeyBindGrant = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.morphclient.testToastGrant", InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_Z, "category.morphclient.keybind"
            ));

            testKeyBindLost = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.morphclient.testToastLost", InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_X, "category.morphclient.keybind"
            ));
        }

        morphKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.morphclient.morph", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N, "category.morphclient.keybind"
        ));

        toggleselfKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.morphclient.toggle", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT, "category.morphclient.keybind"
        ));

        emoteKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.morphclient.emote", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G, "category.morphclient.keybind"
        ));

        resetCacheKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.morphclient.reset_cache", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, "category.morphclient.keybind"
        ));

        displayOwnerBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.morphclient.display_name", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8, "category.morphclient.keybind"
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
        dependencyManager.cache(disguiseTracker = new DisguiseInstanceTracker());
        dependencyManager.cache(morphManager = new ClientMorphManager());
        dependencyManager.cache(serverHandler = new ServerHandler(this));
        dependencyManager.cache(skillHandler = new ClientSkillHandler());
        dependencyManager.cache(modConfigData);
        dependencyManager.cache(new ClientRequestManager());
        dependencyManager.cache(new EntityRendererHelper());
        dependencyManager.cache(animHandlerIndex);

        serverHandler.initializeNetwork();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientTickEvents.END_WORLD_TICK.register(this::postWorldTick);
        HudRenderCallback.EVENT.register(hudRenderHelper::onRender);

        modelWorkarounds = ModelWorkarounds.getInstance();
    }

    private final HudRenderHelper hudRenderHelper = new HudRenderHelper();

    private ModelWorkarounds modelWorkarounds;

    private void postWorldTick(ClientWorld clientWorld)
    {
        var syncersToRemove = new ObjectArrayList<DisguiseSyncer>();

        disguiseTracker.getAllSyncer().forEach(syncer ->
        {
            if (syncer.disposed()) syncersToRemove.add(syncer);
            else syncer.onGameTick();
        });

        syncersToRemove.forEach(syncer -> disguiseTracker.removeSyncer(syncer));
    }

    @Nullable
    private Boolean attackPressedDown = null;

    private void updateKeys(MinecraftClient client)
    {
        while (executeSkillKeyBind.wasPressed() && skillHandler.getCurrentCooldown() <= 0)
        {
            skillHandler.setSkillCooldown(skillHandler.getSkillCooldown());
            serverHandler.sendCommand(new C2SSkillCommand());
        }

        while (unMorphKeyBind.wasPressed())
            serverHandler.sendCommand(new C2SUnmorphCommand());

        var attackPressed = MinecraftClient.getInstance().options.attackKey.isPressed();

        if (attackPressed != Boolean.TRUE.equals(this.attackPressedDown))
        {
            var clientPlayer = MinecraftClient.getInstance().player;
            if (clientPlayer != null && attackPressed)
            {
                var syncer = DisguiseInstanceTracker.getInstance().getSyncerFor(clientPlayer);

                if (syncer != null)
                    syncer.playAttackAnimation();
            }

            this.attackPressedDown = attackPressed;
        }

        if (displayOwnerBind.wasPressed())
        {
            var doRender = !EntityRendererHelper.doRenderRealName;
            EntityRendererHelper.doRenderRealName = doRender;

            var clientPlayer = client.player;
            if (clientPlayer != null)
                clientPlayer.sendMessage(Text.translatable("text.morphclient." + (doRender ? "display" : "hide") + "_real_names"));
        }

        if (debugToasts)
        {
            if (testKeyBindGrant.wasPressed())
            {
                var toasts = client.getToastManager();
                var morphs = morphManager.getAvailableMorphs();
                var random = Random.create();
                var id = morphs.get(random.nextBetween(0, morphs.size() - 1));

                toasts.add(new DisguiseEntryToast(id, true));
            }

            if (testKeyBindLost.wasPressed())
            {
                var toasts = client.getToastManager();
                toasts.add(new RequestToast(S2CRequestCommand.Type.RequestSend, "Very_Loooong_Nammmmmme"));
            }

        }

        while (toggleselfKeyBind.wasPressed())
        {
            var config = getModConfigData();

            boolean val = !morphManager.selfVisibleEnabled.get();

            updateClientView(config.allowClientView, val);
        }

        while (morphKeyBind.wasPressed())
        {
            var player = client.player;

            if (player != null && player.input != null && player.input.sneaking)
            {
                serverHandler.sendCommand(new C2SMorphCommand(null));
            }
            else if (client.currentScreen == null)
            {
                client.setScreen(new WaitingForServerScreen(new DisguiseScreen()));
            }
        }

        while (resetCacheKeybind.wasPressed())
        {
            EntityCache.getGlobalCache().clearCache();
            modelWorkarounds.initWorkarounds();
        }

        while (emoteKeyBind.wasPressed())
            MinecraftClient.getInstance().setScreen(new WaitingForServerScreen(new EmoteScreen()));
    }

    @Nullable
    private Boolean lastClientView = null;

    public void updateClientView(boolean clientViewEnabled, boolean selfViewVisible)
    {
        if (lastClientView == null || clientViewEnabled != lastClientView)
        {
            serverHandler.sendCommand(new C2SToggleSelfCommand(clientViewEnabled ? C2SToggleSelfCommand.SelfViewMode.CLIENT_ON : C2SToggleSelfCommand.SelfViewMode.CLIENT_OFF));
            lastClientView = clientViewEnabled;
        }

        serverHandler.sendCommand(new C2SToggleSelfCommand(C2SToggleSelfCommand.SelfViewMode.fromBoolean(selfViewVisible)));

        modConfigData.allowClientView = clientViewEnabled;
    }

    public void sendMorphCommand(String id)
    {
        if (id == null) id = UNMORPH_STIRNG;

        if (UNMORPH_STIRNG.equals(id))
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
        ).addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.changeCameraHeight.name"), modConfigData.changeCameraHeight)
                        .setTooltip(Text.translatable("option.morphclient.changeCameraHeight.description"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> modConfigData.changeCameraHeight = v)
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.allowClientView.name"), modConfigData.allowClientView)
                        .setTooltip(Text.translatable("option.morphclient.allowClientView.description"))
                        .setDefaultValue(true)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.allowClientView = v;

                            if (serverHandler.serverReady())
                                updateClientView(v, morphManager.selfVisibleEnabled.get());
                        })
                        .build()
        ).addEntry(
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
        ).addEntry(
                entryBuilder.startFloatField(Text.translatable("option.morphclient.scrollSpeed.name"), modConfigData.scrollSpeed)
                        .setTooltip(Text.translatable("option.morphclient.scrollSpeed.description"))
                        .setMax(4F)
                        .setMin(0.5F)
                        .setDefaultValue(1)
                        .setSaveConsumer(v -> modConfigData.scrollSpeed = v)
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.verbosePackets.name"), modConfigData.verbosePackets)
                        .setTooltip(Text.translatable("option.morphclient.verbosePackets.description"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> modConfigData.verbosePackets = v)
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.nametag_scaling"), modConfigData.scaleNameTag)
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> modConfigData.scaleNameTag = v)
                        .build()
        );

        ConfigCategory categoryToast = builder.getOrCreateCategory(Text.translatable("category.morphclient.config_toasts"));

        categoryToast.addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.grant_revoke_toasts"), modConfigData.displayGrantRevokeToast)
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> modConfigData.displayGrantRevokeToast = v)
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.query_set_toasts"), modConfigData.displayQuerySetToast)
                        .setTooltip(Text.translatable("option.morphclient.query_set_toasts.desc"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> modConfigData.displayQuerySetToast = v)
                        .build())
                .addEntry(
                        entryBuilder.startBooleanToggle(Text.translatable("option.morphclient.toast_progress"), modConfigData.displayToastProgress)
                                .setDefaultValue(false)
                                .setSaveConsumer(v -> modConfigData.displayToastProgress = v)
                                .build());

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

        var schedules = new ObjectArrayList<>(this.schedules);
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

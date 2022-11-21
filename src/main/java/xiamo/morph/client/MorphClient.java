package xiamo.morph.client;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.bindables.Bindable;
import xiamo.morph.client.bindables.BindableList;
import xiamo.morph.client.screens.disguise.DisguiseScreen;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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

        //初始化网络
        ClientPlayNetworking.registerGlobalReceiver(initializeChannelIdentifier, (client, handler, buf, responseSender) ->
        {
            if (this.readStringfromByte(buf).equalsIgnoreCase("no"))
            {
                logger.error("初始化失败：被服务器拒绝");
                return;
            }

            this.avaliableMorphs.clear();
            selectedIdentifier.set(null);

            var packetBuf = PacketByteBufs.create();
            ClientPlayNetworking.send(versionChannelIdentifier, packetBuf);
            ClientPlayNetworking.send(initializeChannelIdentifier, PacketByteBufs.create());
        });

        ClientPlayNetworking.registerGlobalReceiver(versionChannelIdentifier, (client, handler, buf, responseSender) ->
        {
            try
            {
                version = buf.readInt();
            }
            catch (Exception e)
            {
                logger.error("未能获取服务器API版本：" + e.getMessage());
                e.printStackTrace();
            }

            logger.info("服务器API版本：" + version);
        });


        ClientPlayNetworking.registerGlobalReceiver(commandChannelIdentifier, (client, handler, buf, responseSender) ->
        {
            try
            {
                var str = readStringfromByte(buf).split(" ", 3);

                if (str.length < 2) return;

                var baseName = str[0];

                if (Objects.equals(baseName, "query"))
                {
                    var subCmdName = str[1];

                    var diff =  Arrays.asList(str[2].split(" "));

                    switch (subCmdName) {
                        case "add" ->
                        {
                            avaliableMorphs.addAll(diff);
                            onGrantConsumers.forEach(c -> c.accept(diff));
                        }
                        case "remove" ->
                        {
                            avaliableMorphs.removeAll(diff);
                            onRevokeConsumers.forEach(c -> c.accept(diff));
                        }
                        case "set" ->
                        {
                            onRevokeConsumers.forEach(c -> c.accept(new ObjectArrayList<>(avaliableMorphs)));

                            this.avaliableMorphs.clear();
                            this.avaliableMorphs.addAll(diff);

                            onGrantConsumers.forEach(c -> c.accept(diff));
                        }
                        default -> logger.warn("未知的客户端指令：" + subCmdName);
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("发生异常：" + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private final List<String> avaliableMorphs = new ObjectArrayList<>();

    public List<String> getAvaliableMorphs()
    {
        return new ObjectArrayList<>(avaliableMorphs);
    }

    private final List<Consumer<List<String>>> onGrantConsumers = new ObjectArrayList<>();
    public void onMorphGrant(Consumer<List<String>> consumer)
    {
        onGrantConsumers.add(consumer);
    }

    private final List<Consumer<List<String>>> onRevokeConsumers = new ObjectArrayList<>();
    public void onMorphRevoke(Consumer<List<String>> consumer)
    {
        onRevokeConsumers.add(consumer);
    }

    private void updateKeys(MinecraftClient client)
    {
        if (executeSkillKeyBind.wasPressed())
            ClientPlayNetworking.send(commandChannelIdentifier, fromString("skill"));

        if (unMorphKeyBind.wasPressed())
            ClientPlayNetworking.send(commandChannelIdentifier, fromString("unmorph"));

        if (toggleselfKeyBind.wasPressed())
            ClientPlayNetworking.send(commandChannelIdentifier, fromString("toggleself"));

        if (morphKeyBind.wasPressed())
        {
            if (client.currentScreen == null)
                client.setScreen(new DisguiseScreen());
        }
    }

    public void sendMorphCommand(String id)
    {
        ClientPlayNetworking.send(commandChannelIdentifier, fromString("morph" + (id == null ? "" : " " + id)));
    }

    private PacketByteBuf fromString(String str)
    {
        var packet = PacketByteBufs.create();

        packet.writeCharSequence(str, StandardCharsets.UTF_8);
        return packet;
    }

    private int version = -1;

    public int getApiVersion()
    {
        return version;
    }

    private String readStringfromByte(ByteBuf buf)
    {
        return buf.resetReaderIndex().readCharSequence(buf.readableBytes(), StandardCharsets.UTF_8).toString();
    }
}

package xiamomc.morph.client.screens.disguise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.EntityCache;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.ServerHandler;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.morph.client.graphics.DrawableText;
import xiamomc.morph.client.graphics.ToggleSelfButton;

public class DisguiseScreen extends Screen
{
    public DisguiseScreen()
    {
        super(Text.literal("选择界面"));

        var morphClient = MorphClient.getInstance();

        this.serverHandler = morphClient.serverHandler;
        this.manager = morphClient.morphManager;

        manager.onMorphGrant(c ->
        {
            if (!this.isCurrent()) return false;

            var availableMorphs = manager.getAvailableMorphs();
            c.forEach(s -> list.children().add(availableMorphs.indexOf(s), new StringWidget(s)));

            return true;
        });

        manager.onMorphRevoke(c ->
        {
            if (!this.isCurrent()) return false;

            c.forEach(s -> list.children().removeIf(w -> w.getIdentifier().equals(s)));

            return true;
        });

        list.children().add(new StringWidget("morph:unmorph"));

        manager.getAvailableMorphs().forEach(s -> list.children().add(new StringWidget(s)));

        selectedIdentifier.bindTo(manager.selectedIdentifier);
        selectedIdentifier.set(manager.currentIdentifier.get());

        serverHandler.serverReady.onValueChanged((o, n) ->
        {
            MorphClient.getInstance().schedule(this::clearAndInit);
        });

        //初始化文本
        manager.currentIdentifier.onValueChanged((o, n) ->
        {
            Text display = null;

            if (n != null)
            {
                var cachedEntity = EntityCache.getEntity(n);

                if (cachedEntity != null)
                    display = cachedEntity.getName();
                else
                    display = Text.literal(n);
            }

            var text = Text.translatable("gui.morphclient.current_disguise",
                    display == null ? Text.translatable("gui.morphclient.no_disguise") : display);

            selectedIdentifierText.setText(text);
        }, true);

        serverAPIText.setText("Client " + serverHandler.getClientVersion() + " :: " + "Server " + serverHandler.getServerVersion());
        serverAPIText.setColor(0x99ffffff);

        titleText.setWidth(200);
        titleText.setHeight(20);

        //初始化按钮
        closeButton = this.buildWidget(0, 0, 150, 20, Text.translatable("gui.back"), (button) ->
        {
            this.close();
        });

        configMenuButton = this.buildWidget(0, 0, 20, 20, Text.literal("C"), (button ->
        {
            var screen = morphClient.getFactory(this).build();

            MinecraftClient.getInstance().setScreen(screen);
        }));

        selfVisibleToggle = new ToggleSelfButton(0, 0, 20, 20, manager.selfVisibleToggled.get(), this);
    }

    private ButtonWidget buildWidget(int x, int y, int width, int height, Text text, ButtonWidget.PressAction action)
    {
        var builder = ButtonWidget.builder(text, action);

        builder.dimensions(x, y, width, height);

        return builder.build();
    }

    private final Bindable<String> selectedIdentifier = new Bindable<>();

    private final ButtonWidget closeButton;
    private final ButtonWidget configMenuButton;
    private final ToggleSelfButton selfVisibleToggle;

    private final ClientMorphManager manager;
    private final ServerHandler serverHandler;

    private final IdentifierDrawableList list = new IdentifierDrawableList(client, 200, 0, 20, 0, 22);
    private final DrawableText titleText = new DrawableText(Text.translatable("gui.morphclient.select_disguise"));
    private final DrawableText selectedIdentifierText = new DrawableText();
    private final DrawableText serverAPIText = new DrawableText();
    private final DrawableText notReadyText = new DrawableText(Text.translatable("gui.morphclient.waiting_for_server"));
    private final DrawableText outdatedText = new DrawableText(Text.translatable("gui.morphclient.version_mismatch").formatted(Formatting.GOLD).formatted(Formatting.BOLD));

    private boolean isCurrent()
    {
        return MinecraftClient.getInstance().currentScreen == this;
    }

    @Override
    public void close()
    {
        //workaround: Bindable在界面关闭后还是会保持引用，得手动把字段设置为null
        list.clearChildren();

        super.close();
    }

    private boolean isInitialCall = true;

    @Override
    protected void init()
    {
        int fontMargin = 4;

        super.init();
        assert this.client != null;

        if (serverHandler.serverReady.get())
        {
            //列表
            list.updateSize(width, this.height, textRenderer.fontHeight * 2 + fontMargin * 2, this.height - 30);

            if (isInitialCall)
            {
                //第一次打开时滚动到当前伪装
                var current = manager.currentIdentifier.get();

                if (current != null)
                {
                    list.scrollTo(list.children().stream()
                            .filter(w -> current.equals(w.getIdentifier())).findFirst().orElse(null));
                }

                isInitialCall = false;
            }

            this.addDrawableChild(list);

            //侧边显示
            this.addDrawable(titleText);
            this.addDrawable(selectedIdentifierText);
            this.addDrawable(serverAPIText);

            if (!MorphClient.getInstance().serverHandler.serverApiMatch())
                this.addDrawable(outdatedText);

            //顶端文本
            var screenX = 30;

            serverAPIText.setScreenX(screenX);
            serverAPIText.setScreenY(this.height - textRenderer.fontHeight - fontMargin);

            outdatedText.setScreenX(screenX);
            outdatedText.setScreenY(this.height - textRenderer.fontHeight * 2 - fontMargin - 2);

            titleText.setScreenX(screenX);
            titleText.setScreenY(fontMargin);
            selectedIdentifierText.setScreenX(screenX);
            selectedIdentifierText.setScreenY(fontMargin + 2 + textRenderer.fontHeight);

            //按钮
            var baseX = this.width - closeButton.getWidth() - 20;

            this.addDrawableChild(closeButton);
            closeButton.setX(baseX);

            this.addDrawableChild(selfVisibleToggle);
            selfVisibleToggle.setX(baseX - selfVisibleToggle.getWidth() - 5);

            this.addDrawableChild(configMenuButton);
            configMenuButton.setX(baseX - selfVisibleToggle.getWidth() - 5 - configMenuButton.getWidth() - 5);

            var bottomY = this.height - 25;
            selfVisibleToggle.setY(bottomY);
            closeButton.setY(bottomY);
            configMenuButton.setY(bottomY);
        }
        else
        {
            this.addDrawable(notReadyText);

            notReadyText.setScreenY(this.height / 2);
            notReadyText.setScreenX(this.width / 2 - 32);

            this.addDrawableChild(this.buildWidget(this.width / 2 - 75, this.height - 29, 150, 20, Text.translatable("gui.back"), (button) ->
            {
                this.close();
            }));
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(MatrixStack matrices, int vOffset)
    {
        super.renderBackground(matrices, vOffset);
    }
}

package xiamo.morph.client.screens.disguise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xiamo.morph.client.MorphClient;
import xiamo.morph.client.bindables.Bindable;
import xiamo.morph.client.graphics.DrawableText;
import xiamo.morph.client.graphics.ToggleSelfButton;

public class DisguiseScreen extends Screen
{
    public DisguiseScreen()
    {
        super(Text.literal("选择界面"));

        var morphClient = MorphClient.getInstance();
        morphClient.onMorphGrant(c ->
        {
            if (!this.isCurrent()) return false;

            c.forEach(s -> list.children().add(new StringWidget(s)));

            return true;
        });

        morphClient.onMorphRevoke(c ->
        {
            if (!this.isCurrent()) return false;

            c.forEach(s -> list.children().removeIf(w -> w.getIdentifier().equals(s)));

            return true;
        });

        list.children().add(new StringWidget("morph:unmorph"));

        morphClient.getAvaliableMorphs().forEach(s -> list.children().add(new StringWidget(s)));

        selectedIdentifier.bindTo(MorphClient.selectedIdentifier);
        selectedIdentifier.set(MorphClient.currentIdentifier.get());

        MorphClient.serverReady.onValueChanged((o, n) ->
        {
            MorphClient.getInstance().schedule(c -> this.clearAndInit());
        });

        //初始化文本
        MorphClient.currentIdentifier.onValueChanged((o, n) ->
        {
            selectedIdentifierText.setText("当前伪装：" + (n == null ? "暂无" : n));
        }, true);

        serverAPIText.setText("Client " + morphClient.getClientVersion() + " :: " + "Server " + morphClient.getServerVersion());
        serverAPIText.setColor(0x99ffffff);

        titleText.setWidth(200);
        titleText.setHeight(20);

        //初始化按钮
        closeButton = this.buildWidget(0, 0, 150, 20, Text.literal("关闭"), (button) ->
        {
            this.close();
        });

        configMenuButton = this.buildWidget(0, 0, 20, 20, Text.literal("C"), (button ->
        {
            var screen = morphClient.getFactory(this).build();

            MinecraftClient.getInstance().setScreen(screen);
        }));

        selfVisibleToggle = new ToggleSelfButton(0, 0, 20, 20, morphClient.selfVisibleToggled.get(), this);
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

    private final IdentifierDrawableList list = new IdentifierDrawableList(client, 200, 0, 20, 0, 22);
    private final DrawableText titleText = new DrawableText("选择伪装");
    private final DrawableText selectedIdentifierText = new DrawableText();
    private final DrawableText serverAPIText = new DrawableText();
    private final DrawableText notReadyText = new DrawableText("等待服务器响应...");
    private final DrawableText outdatedText = new DrawableText(Text.literal("版本不匹配，可能存在兼容性问题！").formatted(Formatting.BOLD));

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

        if (MorphClient.serverReady.get())
        {
            //列表
            list.updateSize(width, this.height, textRenderer.fontHeight * 2 + fontMargin * 2, this.height - 30);

            if (isInitialCall)
            {
                //第一次打开时滚动到当前伪装
                var current = MorphClient.currentIdentifier.get();

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

            if (!MorphClient.getInstance().serverApiMatch())
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

            this.addDrawableChild(this.buildWidget(this.width / 2 - 75, this.height - 29, 150, 20, Text.literal("关闭"), (button) ->
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

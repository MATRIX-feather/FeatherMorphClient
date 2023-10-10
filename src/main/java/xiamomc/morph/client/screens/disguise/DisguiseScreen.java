package xiamomc.morph.client.screens.disguise;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.math.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.EntityCache;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.ServerHandler;
import xiamomc.morph.client.graphics.*;
import xiamomc.morph.client.graphics.container.Container;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.morph.client.screens.FeatherScreen;
import xiamomc.pluginbase.Bindables.Bindable;

public class DisguiseScreen extends FeatherScreen
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

            morphClient.schedule(() ->
            {
                var availableMorphs = manager.getAvailableMorphs();
                c.forEach(s -> list.children().add(availableMorphs.indexOf(s) + 1, new EntityDisplayWidget(s)));
            });

            return true;
        });

        manager.onMorphRevoke(c ->
        {
            if (!this.isCurrent()) return false;

            morphClient.schedule(() ->
                    c.forEach(s -> list.children().removeIf(w -> w.getIdentifier().equals(s))));

            return true;
        });

        selectedIdentifier.bindTo(manager.selectedIdentifier);
        selectedIdentifier.set(manager.currentIdentifier.get());

        serverHandler.serverReady.onValueChanged((o, n) ->
        {
            MorphClient.getInstance().schedule(() ->
            {
                if (this.isCurrent() && !n)
                    this.push(new WaitingForServerScreen());
            });
        }, true);

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

        serverAPIText.setColor(0x99ffffff);

        titleText.setWidth(200);
        titleText.setHeight(20);

        //初始化按钮
        closeButton = this.buildButtonWidget(0, 0, 150, 20, Text.translatable("gui.back"), (button) ->
        {
            this.close();
        });

        configMenuButton = this.buildButtonWidget(0, 0, 20, 20, Text.literal("C"), (button ->
        {
            var screen = morphClient.getFactory(this).build();

            MinecraftClient.getInstance().setScreen(screen);
        }));

        //testButton = this.buildButtonWidget(0, 0, 20, 20, Text.literal("T"), button ->
        //{
        //    this.push(new DisguiseScreenNew(Text.literal("d")));
        //});

        selfVisibleToggle = new ToggleSelfButton(0, 0, 20, 20, manager.selfVisibleEnabled.get(), this);
    }

    private final Bindable<String> selectedIdentifier = new Bindable<>();

    private final MButtonWidget closeButton;
    private final MButtonWidget configMenuButton;
    private final MButtonWidget testButton;
    private final ToggleSelfButton selfVisibleToggle;

    private final ClientMorphManager manager;
    private final ServerHandler serverHandler;

    private final Container topTextContainer = new Container();
    private final Container bottomTextContainer = new Container();

    private final DisguiseList list = new DisguiseList(MinecraftClient.getInstance(), 200, 0, 20, 0, 22);
    private final DrawableText titleText = new DrawableText(Text.translatable("gui.morphclient.select_disguise"));
    private final DrawableText selectedIdentifierText = new DrawableText();
    private final DrawableText serverAPIText = new DrawableText();
    private final DrawableText outdatedText = new DrawableText(Text.translatable("gui.morphclient.version_mismatch").formatted(Formatting.GOLD).formatted(Formatting.BOLD));

    private final int fontMargin = 4;

    @Override
    protected void onScreenExit(Screen next)
    {
        super.onScreenExit(next);

        if (next == null)
        {
            //workaround: Bindable在界面关闭后还是会保持引用，得手动把字段设置为null
            list.clearChildren();
        }
    }

    private final Bindable<Integer> topHeight = new Bindable<>(0);
    private final Bindable<Integer> bottomHeight = new Bindable<>(0);
    private final Recorder<Float> backgroundDim = new Recorder<>(0f);

    public float getBackgroundDim()
    {
        return backgroundDim.get();
    }

    @Override
    protected void onScreenEnter(Screen last)
    {
        super.onScreenEnter(last);

        list.updateSize(width, this.height, 0, 0);

        if (last == null || last instanceof WaitingForServerScreen)
        {
            list.children().add(new EntityDisplayWidget(MorphClient.UNMORPH_STIRNG));

            manager.getAvailableMorphs().forEach(s -> list.children().add(new EntityDisplayWidget(s)));

            //第一次打开时滚动到当前伪装
            var current = manager.currentIdentifier.get();

            if (current != null)
            {
                list.scrollTo(list.children().stream()
                        .filter(w -> current.equals(w.getIdentifier())).findFirst().orElse(null));
            }
        }

        if (last instanceof WaitingForServerScreen waitingForServerScreen)
            backgroundDim.set(waitingForServerScreen.getCurrentDim());

        var duration = 450; //MorphClient.getInstance().getModConfigData().duration;
        var easing = Easing.OutQuint; //MorphClient.getInstance().getModConfigData().easing;

        topHeight.onValueChanged((o, n) ->
        {
            list.setTopPadding(n);
            list.setHeaderHeight(textRenderer.fontHeight * 2 + fontMargin * 2 - n);
        }, true);

        bottomHeight.onValueChanged((o, n) ->
        {
            list.setBottomPadding(this.height - n);
        });

        Transformer.transform(topHeight, textRenderer.fontHeight * 2 + fontMargin * 2, duration, easing);
        Transformer.transform(bottomHeight, 30, duration, easing);
        Transformer.transform(backgroundDim, 0.7f, duration, easing);

        topTextContainer.addRange(titleText, selectedIdentifierText);
        topTextContainer.setY(-40);
        topTextContainer.setPadding(new Margin(0, 0, fontMargin - 1, 0));

        if (!MorphClient.getInstance().serverHandler.serverApiMatch())
            bottomTextContainer.add(outdatedText);

        bottomTextContainer.add(serverAPIText);
        bottomTextContainer.setAnchor(Anchor.BottomLeft);
        bottomTextContainer.setY(40);
        bottomTextContainer.setPadding(new Margin(0, 0, fontMargin + 1, 0));

        // Apply transforms
        topTextContainer.moveToY(0, duration, easing);
        bottomTextContainer.moveToY(0, duration, easing);

        var fontHeight = textRenderer.fontHeight;
        serverAPIText.setMargin(new Margin(0, 0, fontHeight + 2, 0));
        selectedIdentifierText.setMargin(new Margin(0, 0, fontHeight + 2, 0));

        var containerHeight = 30;
        bottomTextContainer.setHeight(containerHeight);
        topTextContainer.setHeight(containerHeight);

        this.addRange(new IMDrawable[]
        {
            list,
            topTextContainer,
            bottomTextContainer,
            closeButton,
            selfVisibleToggle,
            configMenuButton,
            //testButton
        });

        //顶端文本
        var screenX = 30;

        topTextContainer.setX(screenX);
        bottomTextContainer.setX(screenX);

        serverAPIText.setText("Client " + serverHandler.getImplmentingApiVersion() + " :: " + "Server " + serverHandler.getServerVersion());
    }

    @Override
    protected void onScreenResize()
    {
        assert this.client != null;

        //列表
        list.updateSize(width, this.height, list.getTopPadding(), this.height - bottomHeight.get());

        bottomTextContainer.invalidatePosition();
        topTextContainer.invalidatePosition();

        //按钮
        var baseX = this.width - closeButton.getWidth() - 20;

        closeButton.setX(baseX);
        selfVisibleToggle.setX(baseX - selfVisibleToggle.getWidth() - 5);
        configMenuButton.setX(baseX - selfVisibleToggle.getWidth() - 5 - configMenuButton.getWidth() - 5);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        if (!RenderSystem.isOnRenderThread())
            throw new RuntimeException("Not on render thread");

        var dim = (int) (255 * backgroundDim.get());
        var color = Color.ofRGBA(0, 0, 0, dim);

        var bottomMargin = (30 - this.bottomHeight.get());

        var bottomY = this.height - 25 + bottomMargin;
        selfVisibleToggle.setY(bottomY);
        closeButton.setY(bottomY);
        configMenuButton.setY(bottomY);

        context.fillGradient(0, 0, this.width, this.height, color.getColor(), color.getColor());
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        
    }
}

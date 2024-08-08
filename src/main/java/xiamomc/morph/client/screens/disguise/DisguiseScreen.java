package xiamomc.morph.client.screens.disguise;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.shedaniel.math.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import xiamomc.morph.client.screens.WaitingForServerScreen;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.List;

public class DisguiseScreen extends FeatherScreen
{
    private static final Logger log = LoggerFactory.getLogger(DisguiseScreen.class);

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
                    c.forEach(s -> list.children().removeIf(w -> w.getIdentifierString().equals(s))));

            return true;
        });

        this.selectedIdentifier.bindTo(manager.selectedIdentifier);
        this.selectedIdentifier.set(manager.currentIdentifier.get());

        this.serverReady.bindTo(serverHandler.serverReady);

        this.serverReady.onValueChanged((o, n) ->
        {
            MorphClient.getInstance().schedule(() ->
            {
                if (this.isCurrent() && !n)
                    this.push(new WaitingForServerScreen(new DisguiseScreen()));
            });
        }, true);

        this.currentIdentifier.bindTo(manager.currentIdentifier);

        //初始化文本
        this.currentIdentifier.onValueChanged((o, n) ->
        {
            Text display = null;

            if (n != null)
            {
                var cachedEntity = EntityCache.getGlobalCache().getEntity(n, null);

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

        textBox = new MTextBoxWidget(MinecraftClient.getInstance().textRenderer, 120, 17, Text.literal("Search disguise..."));
        textBox.setChangedListener(this::applySearch);

        selfVisibleToggle = new ToggleSelfButton(0, 0, 20, 20, manager.selfVisibleEnabled.get(), this);
    }

    private final Bindable<String> selectedIdentifier = new Bindable<>();
    private final Bindable<Boolean> serverReady = new Bindable<>();
    private final Bindable<String> currentIdentifier = new Bindable<>();

    private final MButtonWidget closeButton;
    private final MTextBoxWidget textBox;
    private final MButtonWidget configMenuButton;
    //private final MButtonWidget testButton;
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
            if (fullList != null)
            {
                list.clearChildren(false);
                list.children().addAll(fullList);
                fullList = null;
            }

            //workaround: Bindable在界面关闭后还是会保持引用，得手动把字段设置为null
            list.clearChildren();

            this.serverReady.unBindFromTarget();
            this.selectedIdentifier.unBindFromTarget();
            this.currentIdentifier.unBindFromTarget();
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

        list.setWidth(width);
        list.setHeight(this.height - 25);

        if (last == null || last instanceof WaitingForServerScreen)
        {
            list.children().add(new EntityDisplayWidget(MorphClient.UNMORPH_STIRNG));

            manager.getAvailableMorphs().forEach(s -> list.children().add(new EntityDisplayWidget(s)));

            //第一次打开时滚动到当前伪装
            scrollToCurrentOrLast(false);
        }

        if (last instanceof WaitingForServerScreen waitingForServerScreen)
            backgroundDim.set(waitingForServerScreen.getCurrentDim());

        var duration = 450; //MorphClient.getInstance().getModConfigData().duration;
        var easing = Easing.OutQuint; //MorphClient.getInstance().getModConfigData().easing;

        topHeight.onValueChanged((o, n) ->
        {
            list.setY(n);
            list.setHeaderHeight(textRenderer.fontHeight * 2 + fontMargin * 2 - n);
        }, true);

        bottomHeight.onValueChanged((o, n) ->
        {
            list.setHeight(this.height - topHeight.get() - n);
        });

        Transformer.transform(topHeight, textRenderer.fontHeight * 2 + fontMargin * 2, duration, easing);
        Transformer.transform(bottomHeight, 30, duration, easing);
        Transformer.transform(backgroundDim, 0.3f, duration, easing);

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
            textBox
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
        list.setWidth(width);
        list.setHeight(this.height - topHeight.get() - bottomHeight.get());

        bottomTextContainer.invalidatePosition();
        topTextContainer.invalidatePosition();

        //按钮
        var baseX = this.width - closeButton.getWidth() - 20;

        textBox.setX(baseX);

        closeButton.setX(baseX);
        selfVisibleToggle.setX(baseX - selfVisibleToggle.getWidth() - 5);
        configMenuButton.setX(baseX - selfVisibleToggle.getWidth() - 5 - configMenuButton.getWidth() - 5);
    }

    private void scrollToCurrentOrLast(boolean scrollToLastIfNoCurrent)
    {
        var filter = list.children();

        var current = manager.currentIdentifier.get();

        if (current != null)
        {
            var widget = list.children().stream()
                    .filter(w -> current.equals(w.getIdentifierString())).findFirst().orElse(null);

            if (widget != null)
            {
                list.scrollTo(widget);

                return;
            }
        }

        if (!scrollToLastIfNoCurrent) return;

        EntityDisplayWidget last = null;
        if (filter.size() >= 1)
            last = filter.get(filter.size() - 1);

        if (last != null)
            list.scrollTo(last);
    }

    private void applySearch(String str)
    {
        if (str.isEmpty())
        {
            if (fullList != null)
            {
                list.clearChildren(false);
                list.children().addAll(fullList);

                fullList = null;
            }

            return;
        }

        if (fullList == null)
            this.fullList = new ObjectArrayList<>(list.children());

        //搜索id和已加载伪装的实体名称
        var finalStr = str.toLowerCase().trim();
        var filter = fullList.stream().filter(w ->
                        w.getIdentifier().getPath().contains(finalStr)
                                || w.getEntityName().contains(finalStr))
                .toList();

        list.clearChildren(false);
        list.children().addAll(filter);

        if (list.getScrollAmount() > list.getMaxScroll())
            scrollToCurrentOrLast(true);
    }

    /**
     * 搜索前伪装列表中的所有元素
     */
    private List<EntityDisplayWidget> fullList;

    @Override
    protected void init()
    {
        super.init();
        this.setFocused(textBox);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        var dim = (int) (255 * backgroundDim.get());
        var color = Color.ofRGBA(0, 0, 0, dim);

        var bottomMargin = (30 - this.bottomHeight.get());

        var bottomY = this.height - 25 + bottomMargin;
        selfVisibleToggle.setY(bottomY);
        closeButton.setY(bottomY);
        configMenuButton.setY(bottomY);

        textBox.setY(this.topHeight.get() - textBox.getHeight() - 5);

        context.fillGradient(0, 0, this.width, this.height, color.getColor(), color.getColor());

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void applyBlur(float delta)
    {
        super.applyBlur(delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.renderBackground(context, mouseX, mouseY, delta);

        context.setShaderColor(0.2F, 0.2F, 0.2F, 1.0F);

        RenderSystem.enableBlend();
        context.drawTexture(Screen.MENU_BACKGROUND_TEXTURE,
                0, 0,
                0, -topHeight.get(),
                this.width, this.topHeight.get(), 32, 32);

        context.drawTexture(Screen.MENU_BACKGROUND_TEXTURE,
                0, this.height - bottomHeight.get(),
                0, 0,
                this.width, this.height, 32, 32);

        context.setShaderColor(1F, 1F, 1F, 1F);
    }

    @Override
    public void renderInGameBackground(DrawContext context)
    {
    }

    @Override
    protected void renderDarkening(DrawContext context)
    {
    }

    @Override
    protected void renderDarkening(DrawContext context, int x, int y, int width, int height)
    {
    }
}

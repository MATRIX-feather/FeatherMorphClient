package xyz.nifeather.morph.client.screens.disguise;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.shedaniel.math.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.NavigationAxis;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.client.ClientMorphManager;
import xyz.nifeather.morph.client.EntityCache;
import xyz.nifeather.morph.client.MorphClient;
import xyz.nifeather.morph.client.ServerHandler;
import xyz.nifeather.morph.client.graphics.*;
import xyz.nifeather.morph.client.graphics.container.Container;
import xyz.nifeather.morph.client.graphics.transforms.Recorder;
import xyz.nifeather.morph.client.screens.FeatherScreen;
import xyz.nifeather.morph.client.screens.WaitingForServerScreen;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.client.screens.disguise.preview.DisguisePreviewDisplay;

import java.util.List;

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
                c.forEach(s -> disguiseList.children().add(availableMorphs.indexOf(s) + 1, new EntityDisplayEntry(s)));
            });

            return true;
        });

        manager.onMorphRevoke(c ->
        {
            if (!this.isCurrent()) return false;

            morphClient.schedule(() ->
                    c.forEach(s -> disguiseList.children().removeIf(w -> w.getIdentifierString().equals(s))));

            return true;
        });

        this.selectedIdentifier.bindTo(manager.selectedIdentifier);
        this.selectedIdentifier.onValueChanged((o, n) ->
        {
            this.refreshEntityPreview(n);
        }, true);

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

            var text = Text.stringifiedTranslatable("gui.morphclient.current_disguise",
                    display == null ? Text.translatable("gui.none") : display);

            selectedIdentifierText.setText(text);
            refreshEntityPreview(n);
        }, true);

        serverAPIText.setColor(0x99ffffff);

        titleText.setWidth(200);
        titleText.setHeight(20);

        //初始化按钮
        closeButton = this.buildButtonWidget(0, 0, 112, 20, Text.translatable("gui.back"), (button) ->
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

    @Nullable
    private EntityDisplay playerDisplay;

    private void refreshEntityPreview(String newId)
    {
        String identifier = newId == null
                            ? this.currentIdentifier.get() == null
                                ? MorphClient.UNMORPH_STIRNG
                                : this.currentIdentifier.get()
                            : newId;

        if (playerDisplay != null)
        {
            this.remove(playerDisplay);
            playerDisplay.dispose();
            playerDisplay = null;
        }

        var newDisplay = new DisguisePreviewDisplay(identifier, true, EntityDisplay.InitialSetupMethod.SYNC);
        newDisplay.setMasking(true);

        newDisplay.setParentScreenSpace(new ScreenRect(ScreenPos.of(NavigationAxis.HORIZONTAL, 0, 0), this.width, this.height));
        newDisplay.setRelativeSizeAxes(Axes.Both);
        newDisplay.setAnchor(Anchor.CentreRight);
        newDisplay.setSize(new Vector2f(0.4f, 0.7f));

        playerDisplay = newDisplay;

        this.add(newDisplay);
    }

    private final Bindable<String> selectedIdentifier = new Bindable<>(MorphClient.UNMORPH_STIRNG);
    private final Bindable<Boolean> serverReady = new Bindable<>(false);
    private final Bindable<String> currentIdentifier = new Bindable<>(MorphClient.UNMORPH_STIRNG);

    private final MButtonWidget closeButton;
    private final MTextBoxWidget textBox;
    private final MButtonWidget configMenuButton;
    //private final MButtonWidget quickDisguiseButton;
    //private final MButtonWidget testButton;
    private final ToggleSelfButton selfVisibleToggle;

    private final ClientMorphManager manager;
    private final ServerHandler serverHandler;

    private final Container topTextContainer = new Container();
    private final Container bottomTextContainer = new Container();

    private final DisguiseList disguiseList = new DisguiseList(MinecraftClient.getInstance(), 200, 0, 20, 0, 22);
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
                disguiseList.clearChildren(false);
                disguiseList.children().addAll(fullList);
                fullList = null;
            }

            //workaround: Bindable在界面关闭后还是会保持引用，得手动把字段设置为null
            disguiseList.clearChildren();

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

        resizeDisguiseList();

        if (last == null || last instanceof WaitingForServerScreen)
        {
            disguiseList.addChild(new EntityDisplayEntry(MorphClient.UNMORPH_STIRNG));

            manager.getAvailableMorphs().forEach(s -> disguiseList.addChild(new EntityDisplayEntry(s)));

            //第一次打开时滚动到当前伪装
            scrollToCurrentOrLast(false);
        }

        if (last instanceof WaitingForServerScreen waitingForServerScreen)
            backgroundDim.set(waitingForServerScreen.getCurrentDim());

        //var duration = 450; //MorphClient.getInstance().getModConfigData().duration;
        //var easing = Easing.OutQuint; //MorphClient.getInstance().getModConfigData().easing;

        int headerTargetHeight = textRenderer.fontHeight * 2 + fontMargin * 2;
        int footerTargetHeight = 30;

        topHeight.set(headerTargetHeight);
        bottomHeight.set(30);
        backgroundDim.set(0.3f);

        //Transformer.transform(topHeight, headerTargetHeight, duration, easing);
        //Transformer.transform(bottomHeight, footerTargetHeight, duration, easing);
        //Transformer.transform(backgroundDim, 0.3f, duration, easing);

        topTextContainer.addRange(titleText, selectedIdentifierText);
        //topTextContainer.setY(-40);
        topTextContainer.setPadding(new Margin(0, 0, fontMargin - 1, 0));

        if (!MorphClient.getInstance().serverHandler.serverApiMatch())
            bottomTextContainer.add(outdatedText);

        bottomTextContainer.add(serverAPIText);
        bottomTextContainer.setAnchor(Anchor.BottomLeft);
        //bottomTextContainer.setY(40);
        bottomTextContainer.setPadding(new Margin(0, 0, fontMargin + 1, 0));

        // Apply transforms
        //topTextContainer.moveToY(0, duration, easing);
        //bottomTextContainer.moveToY(0, duration, easing);

        var fontHeight = textRenderer.fontHeight;
        serverAPIText.setMargin(new Margin(0, 0, fontHeight + 2, 0));
        selectedIdentifierText.setMargin(new Margin(0, 0, fontHeight + 2, 0));

        var containerHeight = 30;
        bottomTextContainer.setHeight(containerHeight);
        topTextContainer.setHeight(containerHeight);

        this.addRange(new IMDrawable[]
        {
            disguiseList,
            topTextContainer,
            bottomTextContainer,
            closeButton,
            selfVisibleToggle,
            configMenuButton,
            //quickDisguiseButton,
            textBox
            //testButton
        });

        //顶端文本
        var screenX = 30;

        topTextContainer.setX(screenX);
        bottomTextContainer.setX(screenX);

        serverAPIText.setText("C %s :: S %s".formatted(serverHandler.getImplmentingApiVersion(), serverHandler.getServerApiVersion()));
    }

    private void resizeDisguiseList()
    {
        int headerTargetHeight = textRenderer.fontHeight * 2 + fontMargin * 2;
        int footerTargetHeight = 30;

        int disguiseListWidth = Math.round(this.width * 0.6f);
        disguiseList.setWidth(disguiseListWidth);
        disguiseList.setHeight(this.height - headerTargetHeight - footerTargetHeight);
        disguiseList.setY(headerTargetHeight);
    }

    @Override
    protected void onScreenResize()
    {
        assert this.client != null;

        //列表
        resizeDisguiseList();

        bottomTextContainer.invalidatePosition();
        topTextContainer.invalidatePosition();

        //按钮
        var baseX = this.width - closeButton.getWidth() - 20;

        textBox.setX(baseX);

        closeButton.setX(baseX);
        selfVisibleToggle.setX(baseX - selfVisibleToggle.getWidth() - 5);
        configMenuButton.setX(baseX - selfVisibleToggle.getWidth() - 5 - configMenuButton.getWidth() - 5);
        //quickDisguiseButton.setX(baseX - selfVisibleToggle.getWidth() - 5 - configMenuButton.getWidth() - 5 - quickDisguiseButton.getWidth());
    }

    private void scrollToCurrentOrLast(boolean scrollToLastIfNoCurrent)
    {
        var filter = disguiseList.children();

        var current = manager.currentIdentifier.get();

        if (current != null)
        {
            var widget = disguiseList.children().stream()
                    .filter(w -> current.equals(w.getIdentifierString())).findFirst().orElse(null);

            if (widget != null)
            {
                disguiseList.scrollTo(widget);

                return;
            }
        }

        if (!scrollToLastIfNoCurrent) return;

        EntityDisplayEntry last = null;
        if (!filter.isEmpty())
            last = filter.getLast();

        if (last != null)
            disguiseList.scrollTo(last);
    }

    //todo: Refactor this to use another list instance.
    private void applySearch(String str)
    {
        if (str.isEmpty())
        {
            if (fullList != null)
            {
                disguiseList.clearChildren(false);
                disguiseList.addChildrenRange(fullList);

                fullList = null;
            }

            return;
        }

        if (fullList == null)
            this.fullList = new ObjectArrayList<>(disguiseList.children());

        //搜索id和已加载伪装的实体名称
        var finalStr = str.toLowerCase().trim();
        var filter = fullList.stream().filter(w ->
                        w.getIdentifier().getPath().contains(finalStr)
                                || w.getEntityName().contains(finalStr))
                .toList();

        disguiseList.clearChildren(false);
        disguiseList.addChildrenRange(filter);

        if (disguiseList.getScrollY() > disguiseList.getMaxScrollY())
            scrollToCurrentOrLast(true);
    }

    /**
     * 搜索前伪装列表中的所有元素
     */
    private List<EntityDisplayEntry> fullList;

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

        // Separator
        Identifier identifierHeader = Screen.INWORLD_HEADER_SEPARATOR_TEXTURE;
        Identifier identifierFooter = Screen.INWORLD_FOOTER_SEPARATOR_TEXTURE;
        context.drawTexture(RenderLayer::getGuiTextured, identifierHeader,
                0, this.topHeight.get() - 2,
                0, 0,
                this.width, 2,
                32, 2);

        context.drawTexture(RenderLayer::getGuiTextured, identifierFooter,
                0, this.height - bottomHeight.get(),
                0, 0,
                this.width, 2,
                32, 2);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.renderBackground(context, mouseX, mouseY, delta);

        RenderSystem.enableBlend();

        context.fill(0, 0, this.width, this.height, Color.ofRGBA(0, 0, 0, 0.3f).getColor());

        context.drawTexture(RenderLayer::getGuiTextured, Screen.MENU_BACKGROUND_TEXTURE,
                0, 0,
                0, -topHeight.get(),
                this.width, this.topHeight.get(), 32, 32);

        context.drawTexture(RenderLayer::getGuiTextured, Screen.MENU_BACKGROUND_TEXTURE,
                0, this.height - bottomHeight.get(),
                0, 0,
                this.width, this.height, 32, 32);
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

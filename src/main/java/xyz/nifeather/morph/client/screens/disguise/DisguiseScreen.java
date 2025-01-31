package xyz.nifeather.morph.client.screens.disguise;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.NavigationAxis;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.ClientMorphManager;
import xyz.nifeather.morph.client.EntityCache;
import xyz.nifeather.morph.client.FeatherMorphClient;
import xyz.nifeather.morph.client.ServerHandler;
import xyz.nifeather.morph.client.graphics.*;
import xyz.nifeather.morph.client.graphics.color.Colors;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;
import xyz.nifeather.morph.client.graphics.container.*;
import xyz.nifeather.morph.client.graphics.container.Container;
import xyz.nifeather.morph.client.graphics.transforms.Recorder;
import xyz.nifeather.morph.client.screens.FeatherScreen;
import xyz.nifeather.morph.client.screens.WaitingForServerScreen;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.client.screens.disguise.preview.DisguisePreviewDisplay;

import java.util.List;
import java.util.function.Function;

public class DisguiseScreen extends FeatherScreen
{
    public DisguiseScreen()
    {
        super(Text.literal("选择界面"));

        var morphClient = FeatherMorphClient.getInstance();

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
            FeatherMorphClient.getInstance().schedule(() ->
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

        textBox = new TextFieldWidgetWrapper(new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 120, 17, Text.literal("Search disguise...")));
        textBox.setChangedListener(this::onTextBoxText);

        textBox.setAnchor(Anchor.TopRight);
        textBox.setMargin(new MarginPadding(5));
        //textBox.setX(Math.round(-textBox.getWidth()));
    }

    @Nullable
    private EntityDisplay playerDisplay;

    private void refreshEntityPreview(String newId)
    {
        String identifier = newId == null
                            ? this.currentIdentifier.get() == null
                                ? FeatherMorphClient.UNMORPH_STIRNG
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

    private final Bindable<String> selectedIdentifier = new Bindable<>(FeatherMorphClient.UNMORPH_STIRNG);
    private final Bindable<Boolean> serverReady = new Bindable<>(false);
    private final Bindable<String> currentIdentifier = new Bindable<>(FeatherMorphClient.UNMORPH_STIRNG);

    private final TextFieldWidgetWrapper textBox;

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
            disguiseList.addChild(new EntityDisplayEntry(FeatherMorphClient.UNMORPH_STIRNG));

            manager.getAvailableMorphs().forEach(s -> disguiseList.addChild(new EntityDisplayEntry(s)));

            //第一次打开时滚动到当前伪装
            scrollToCurrentOrLast(false);
        }

        if (last instanceof WaitingForServerScreen waitingForServerScreen)
            backgroundDim.set(waitingForServerScreen.getCurrentDim());

        int headerTargetHeight = textRenderer.fontHeight * 2 + fontMargin * 2;

        topHeight.set(headerTargetHeight);
        bottomHeight.set(30);
        backgroundDim.set(0.3f);

        topTextContainer.addRange(titleText, selectedIdentifierText);
        topTextContainer.setPadding(new MarginPadding(0, 0, fontMargin - 1, 0));

        if (!FeatherMorphClient.getInstance().serverHandler.serverApiMatch())
            bottomTextContainer.add(outdatedText);

        bottomTextContainer.add(serverAPIText);
        bottomTextContainer.setAnchor(Anchor.BottomLeft);
        bottomTextContainer.setPadding(new MarginPadding(0, 0, fontMargin + 1, 0));

        var fontHeight = textRenderer.fontHeight;
        serverAPIText.setMargin(new MarginPadding(0, 0, fontHeight + 2, 0));
        selectedIdentifierText.setMargin(new MarginPadding(0, 0, fontHeight + 2, 0));

        var containerHeight = 30;
        bottomTextContainer.setHeight(containerHeight);
        topTextContainer.setHeight(containerHeight);

        var buttonContainer = new BasicContainer<MDrawable>();

        //初始化按钮
        var closeButton = this.createDrawableWrapper(0, 0, 112, 20, Text.translatable("gui.back"), (button) ->
        {
            this.close();
        });

        var configMenuButton = this.createDrawableWrapper(0, 0, 20, 20, Text.literal("C"), (button ->
        {
            var screen = FeatherMorphClient.getInstance().getFactory(this).build();

            MinecraftClient.getInstance().setScreen(screen);
        }));

        var selfVisibleToggle = this.createToggleSelfButton();

        selfVisibleToggle.setX(Math.round(configMenuButton.getWidth() + 5));
        closeButton.setX(selfVisibleToggle.getX() + (int)selfVisibleToggle.getWidth() + 5);

        buttonContainer.addRange(closeButton, selfVisibleToggle, configMenuButton);
        buttonContainer.setAnchor(Anchor.BottomRight);
        buttonContainer.setSize(new Vector2f(closeButton.getX() + closeButton.getWidth(), 20));
        buttonContainer.setMargin(new MarginPadding(5));

        var topHeader = new DrawableSprite(Screen.INWORLD_HEADER_SEPARATOR_TEXTURE, false);
        topHeader.setY(this.topHeight.get() - 2);
        topHeader.setSize(new Vector2f(1, 2));
        topHeader.setRelativeSizeAxes(Axes.X);

        var bottomFooter = new DrawableSprite(Screen.INWORLD_FOOTER_SEPARATOR_TEXTURE, false);
        bottomFooter.setAnchor(Anchor.BottomLeft);
        bottomFooter.setY(-bottomHeight.get() + 1);
        bottomFooter.setSize(new Vector2f(1, 2));
        bottomFooter.setRelativeSizeAxes(Axes.X);

        //顶端文本
        var screenX = 30;

        topTextContainer.setX(screenX);
        bottomTextContainer.setX(screenX);
        textBox.setX(-25);
        buttonContainer.setX(-25);

        serverAPIText.setText("C %s :: S %s".formatted(serverHandler.getImplmentingApiVersion(), serverHandler.getServerApiVersion()));

        this.addRange(new IMDrawable[]
        {
                disguiseList,
                topTextContainer,
                bottomTextContainer,
                buttonContainer,
                textBox,
                topHeader,
                bottomFooter
        });
    }

    private DrawableButtonWrapper createToggleSelfButton()
    {
        var bindable = manager.selfVisibleEnabled;

        Function<Boolean, Text> textFunction = val ->
        {
            var color = val
                        ? TextColor.fromFormatting(Formatting.GREEN)
                        : TextColor.fromFormatting(Formatting.RED);

            return Text.literal(val ? "I" : "O")
                    .setStyle(Style.EMPTY.withColor(color));
        };

        var button = this.buildButtonWidget(0, 0, 20, 20, textFunction.apply(bindable.get()), btn ->
        {
            var val = !bindable.get();
            bindable.set(val);

            var modInstance = FeatherMorphClient.getInstance();
            var config = modInstance.getModConfigData();

            modInstance.updateClientView(config.allowClientView, val);
            btn.setMessage(textFunction.apply(val));
        });

        return new DrawableButtonWrapper(button);
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

    private final class_114514 field_1919810 = new class_114514(this);

    private void onTextBoxText(String str)
    {
        if (!str.startsWith("!"))
        {
            textBox.widget.setEditableColor(Colors.WHITE.getColor());
            applySearch(str);
        }
        else
        {
            textBox.widget.setEditableColor(MaterialColors.Amber500.getColor());
            field_1919810.apply(str);
        }
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
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.renderBackground(context, mouseX, mouseY, delta);

        context.drawTexture(RenderLayer::getGuiTextured, Screen.MENU_BACKGROUND_TEXTURE,
                0, 0,
                0, -topHeight.get(),
                this.width, this.topHeight.get(), 32, 32);

        context.drawTexture(RenderLayer::getGuiTextured, Screen.MENU_BACKGROUND_TEXTURE,
                0, this.height - bottomHeight.get(),
                0, 0,
                this.width, this.height, 32, 32);
    }
}

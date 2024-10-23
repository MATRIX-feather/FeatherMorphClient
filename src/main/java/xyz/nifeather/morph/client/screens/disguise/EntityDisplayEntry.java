package xyz.nifeather.morph.client.screens.disguise;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.client.ClientMorphManager;
import xyz.nifeather.morph.client.MorphClient;
import xyz.nifeather.morph.client.MorphClientObject;
import xyz.nifeather.morph.client.graphics.Anchor;
import xyz.nifeather.morph.client.graphics.EntityDisplay;
import xyz.nifeather.morph.client.graphics.DrawableSprite;
import xyz.nifeather.morph.client.graphics.color.Colors;
import xyz.nifeather.morph.client.graphics.container.Container;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.List;

public class EntityDisplayEntry extends ElementListWidget.Entry<EntityDisplayEntry> implements Comparable<EntityDisplayEntry>
{
    private DisplayWidget field;

    private String identifier = "???";

    public String getEntityName()
    {
        return field.entityName == null ? "???" : field.entityName;
    }

    public String getIdentifierString()
    {
        return identifier;
    }

    private Identifier identifierAsNms;

    private static final Identifier defaultIdentifier = Identifier.of("morph", "unknown");

    public Identifier getIdentifier()
    {
        if (identifierAsNms != null)
            return identifierAsNms;

        var id = Identifier.tryParse(identifier.toLowerCase());
        if (id == null)
            id = defaultIdentifier;

        this.identifierAsNms = id;
        return id;
    }

    @Override
    public List<? extends Selectable> selectableChildren()
    {
        return children;
    }

    @Override
    public List<? extends Element> children()
    {
        return children;
    }
    private final List<DisplayWidget> children = new ObjectArrayList<>();

    public EntityDisplayEntry(String name)
    {
        initFields(name);
    }

    public void clearChildren()
    {
        children.forEach(DisplayWidget::dispose);
        children.clear();
    }

    private void initFields(String name)
    {
        this.identifier = name;
        children.add(field = new DisplayWidget(0, 0, 180, 20, name));
    }

    @Override
    public void render(DrawContext matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
    {
        field.screenSpaceY = y;
        field.screenSpaceX = x;
        field.render(matrices, mouseX, mouseY, tickDelta);
    }

    @Override
    public int compareTo(@NotNull EntityDisplayEntry entityDisplayEntry)
    {
        return identifier.compareTo(entityDisplayEntry.identifier);
    }

    private static class DisplayWidget extends MorphClientObject implements Selectable, Drawable, Element
    {
        private final String identifier;
        private String entityName;

        private Text display;

        int screenSpaceY = 0;
        int screenSpaceX = 0;

        int width = 0;
        int height = 0;


        private final boolean isPlayerItSelf;

        @Resolved(shouldSolveImmediately = true)
        private ClientMorphManager manager;

        private void dispose()
        {
            currentIdentifier.dispose();
            currentIdentifier = null;

            selectedIdentifier.dispose();
            selectedIdentifier = null;
        }

        private Bindable<String> currentIdentifier = new Bindable<>();
        private Bindable<String> selectedIdentifier = new Bindable<>();

        public static final Identifier buttonTextureSelected = Identifier.of("morphclient", "disguise_selection/disguise_select_selected");
        public static final Identifier buttonTextureCurrent = Identifier.of("morphclient", "disguise_selection/disguise_select_current");
        public static final Identifier buttonTextureWaiting = Identifier.of("morphclient", "disguise_selection/disguise_select_waiting");
        public static final Identifier buttonTextureOverlay = Identifier.of("morphclient", "disguise_selection/disguise_select_overlay_hover");

        private final DrawableSprite spriteSelected;
        private final DrawableSprite spriteCurrent;
        private final DrawableSprite spriteWaiting;
        private final DrawableSprite spriteHover;

        public DisplayWidget(int screenSpaceX, int screenSpaceY, int width, int height, String identifier)
        {
            this.identifier = identifier;

            this.isPlayerItSelf = identifier.equals(MorphClient.UNMORPH_STIRNG);

            this.screenSpaceX = screenSpaceX;
            this.screenSpaceY = screenSpaceY;

            this.width = width;
            this.height = height;

            selectedIdentifier.bindTo(manager.selectedIdentifier);
            currentIdentifier.bindTo(manager.currentIdentifier);

            // Setup drawables
            this.entityDisplay = new EntityDisplay(identifier, false , EntityDisplay.InitialSetupMethod.ASYNC);
            entityDisplay.postEntitySetup = () ->
            {
                this.entityName = entityDisplay.getDisplayName().getString();
                this.trimDisplay(entityDisplay.getDisplayName());
            };

            // Container Setup
            var backgroundSize = new Vector2f(this.width, this.height);
            displayContainer.setSize(new Vector2f(48, 18));
            entityDisplay.setSize(new Vector2f(18, 18));
            backgroundContainer.setSize(backgroundSize);

            displayContainer.add(entityDisplay);
            backgroundContainer.add(spriteSelected = new DrawableSprite(buttonTextureSelected));
            backgroundContainer.add(spriteCurrent = new DrawableSprite(buttonTextureCurrent));
            backgroundContainer.add(spriteWaiting = new DrawableSprite(buttonTextureWaiting));
            backgroundContainer.add(spriteHover = new DrawableSprite(buttonTextureOverlay));
            backgroundContainer.children().forEach(d ->
            {
                d.setAlpha(0f);
                d.setSize(backgroundSize);
            });

            // Setup display
            this.display = entityDisplay.getDisplayName();
            entityDisplay.setAnchor(Anchor.BottomCentre);

            if (identifier.equals(currentIdentifier.get()) || isPlayerItSelf)
                entityDisplay.doSetupImmedately();

            activationState.onValueChanged((oldVal, newVal) ->
            {
                if (newVal == null) return;

                this.spriteCurrent.fadeOut(300, Easing.OutExpo);
                this.spriteSelected.fadeOut(300, Easing.OutExpo);
                this.spriteWaiting.fadeOut(300, Easing.OutExpo);

                switch (newVal)
                {
                    case SELECTED -> spriteSelected.fadeIn(300, Easing.OutExpo);
                    case CURRENT -> spriteCurrent.fadeIn(300, Easing.OutExpo);
                    case WAITING -> spriteWaiting.fadeIn(300, Easing.OutExpo);
                    default -> {}
                }
            });

            selectedIdentifier.onValueChanged((o, n) ->
            {
                var actState = activationState.get();

                if (!identifier.equals(n) && actState != ActivationState.CURRENT && actState != ActivationState.WAITING)
                    activationState.set(ActivationState.NONE);
            }, true);

            currentIdentifier.onValueChanged((o, n) ->
            {
                n = n == null ? MorphClient.UNMORPH_STIRNG : n;
                o = o == null ? MorphClient.UNMORPH_STIRNG : o;

                var isCurrent = identifier.equals(n);
                var prevIsCurrent = identifier.equals(o);

                if (prevIsCurrent)
                    entityDisplay.resetEntity();

                activationState.set(isCurrent
                        ? ActivationState.CURRENT
                        : (prevIsCurrent ? ActivationState.NONE : activationState.get())
                );
            }, true);
        }

        private void trimDisplay(Text text)
        {
            this.display = text;

            this.addSchedule(() ->
            {
                var targetMultiplier = entityDisplay.getDisplayingEntity() == null ? 0.85 : 0.7;
                var toDisplay = textRenderer.trimToWidth(text, (int)Math.round(this.width * targetMultiplier));
                var trimmed = !toDisplay.getString().equals(text.getString());

                if (trimmed)
                    this.display = Text.literal(toDisplay.getString() + "...");
            });
        }

        private final EntityDisplay entityDisplay;
        private final Container displayContainer = new Container();
        private final Container backgroundContainer = new Container();

        private final static TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta)
        {
            var lastHovered = this.hovered;
            this.hovered = mouseX < this.screenSpaceX + width && mouseX > this.screenSpaceX
                    && mouseY < this.screenSpaceY + height && mouseY > this.screenSpaceY;

            if (lastHovered != this.hovered)
            {
                if (this.hovered)
                    spriteHover.fadeIn(300, Easing.OutExpo);
                else
                    spriteHover.fadeOut(300, Easing.OutExpo);
            }

            var matrices = context.getMatrices();

            try
            {
                matrices.push();

                if (this.hovered)
                    matrices.translate(0, 0, 64);

                var actState = activationState.get();

                if (actState == ActivationState.CURRENT)
                    matrices.translate(0, 0, 64);

                matrices.push();
                this.backgroundContainer.setX(this.screenSpaceX);
                this.backgroundContainer.setY(this.screenSpaceY);
                this.backgroundContainer.render(context, mouseX, mouseY, delta);
                matrices.pop();

                matrices.translate(0, 0, 64);

                var x = screenSpaceX + width - 24 - 5;
                var y = screenSpaceY + 1;
                var mX = 30;
                var mY = -9;

                if (actState == ActivationState.CURRENT)
                {
                    mX = x - mouseX;
                    mY = y - mouseY - (this.height / 2);
                }

                displayContainer.setX(x);
                displayContainer.setY(y);
                displayContainer.setMasking(!hovered);
                displayContainer.render(context, mX, mY, 0);
            }
            catch (Exception e)
            {
                LoggerFactory.getLogger("morph").error(e.getMessage());
                e.printStackTrace();
            }
            finally
            {
                context.drawTextWithShadow(textRenderer, display,
                        screenSpaceX + 5, (screenSpaceY + Math.round((height - textRenderer.fontHeight) / 2f)), 0xffffffff);

                matrices.pop();
            }
        }

        private boolean hovered;

        @Override
        public boolean isMouseOver(double mouseX, double mouseY)
        {
            return isHovered();
        }

        @Override
        public Selectable.SelectionType getType()
        {
            return (activationState.get() == ActivationState.CURRENT ? Selectable.SelectionType.FOCUSED : Selectable.SelectionType.NONE);
        }

        @NotNull
        private Bindable<ActivationState> activationState = new Bindable<>(ActivationState.NONE);

        private void playClickSound()
        {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            if (!isHovered()) return false;

            manager.selectedIdentifier.set(this.identifier);

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
            {
                var lastFocusType = activationState.get();

                switch (lastFocusType)
                {
                    case SELECTED ->
                    {
                        activationState.set(isPlayerItSelf && manager.currentIdentifier.get() == null
                                ? ActivationState.NONE
                                : ActivationState.WAITING);

                        MorphClient.getInstance().sendMorphCommand(this.identifier);
                        playClickSound();
                    }

                    case CURRENT -> manager.selectedIdentifier.set(null);

                    case WAITING -> { }

                    default ->
                    {
                        activationState.set(ActivationState.SELECTED);
                        playClickSound();
                    }
                }

                return true;
            }
            else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) //Selected + 右键 -> 取消选择
            {
                var actState = activationState.get();

                if (actState == ActivationState.SELECTED)
                {
                    manager.selectedIdentifier.set(null);
                    playClickSound();
                }
                else if (actState == ActivationState.CURRENT && !isPlayerItSelf)
                {
                    activationState.set(ActivationState.WAITING);
                    MorphClient.getInstance().sendMorphCommand(null);
                    playClickSound();
                }
            }

            return Element.super.mouseClicked(mouseX, mouseY, button);
        }

        private boolean isHovered()
        {
            return this.hovered;
        }

        private boolean focused = false;

        @Override
        public void setFocused(boolean focused)
        {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return focused;
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder)
        {
            builder.nextMessage().put(NarrationPart.HINT, Text.literal("Disguise of").append(this.display));
        }

        private enum ActivationState
        {
            NONE,
            SELECTED,
            WAITING,
            CURRENT;
        }
    }
}

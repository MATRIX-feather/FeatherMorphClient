package xiamomc.morph.client.screens.disguise;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.*;
import xiamomc.morph.client.entities.MorphLocalPlayer;
import xiamomc.morph.client.graphics.PlayerRenderHelper;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EntityDisplayWidget extends ElementListWidget.Entry<EntityDisplayWidget> implements Comparable<EntityDisplayWidget>
{
    private TextWidget field;

    private String identifier = "???";

    public String getIdentifier()
    {
        return identifier;
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
    private final List<TextWidget> children = new ObjectArrayList<>();

    public EntityDisplayWidget(String name)
    {
        initFields(name);
    }

    public void clearChildren()
    {
        children.forEach(TextWidget::dispose);
        children.clear();
    }

    private void initFields(String name)
    {
        this.identifier = name;
        children.add(field = new TextWidget(0, 0, 180, 20, name));
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
    {
        field.screenSpaceY = y;
        field.screenSpaceX = x;
        field.render(matrices, mouseX, mouseY, tickDelta);
    }

    @Override
    public int compareTo(@NotNull EntityDisplayWidget entityDisplayWidget)
    {
        return identifier.compareTo(entityDisplayWidget.identifier);
    }

    private static class TextWidget extends MorphClientObject implements Selectable, Drawable, Element
    {
        private final String identifier;
        private Text display;

        int screenSpaceY = 0;
        int screenSpaceX = 0;

        int width = 0;
        int height = 0;

        private LivingEntity entity;
        private int entitySize;
        private int entityYOffset;

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

        public TextWidget(int screenSpaceX, int screenSpaceY, int width, int height, String identifier)
        {
            this.identifier = identifier;
            this.display = Text.translatable("gui.morphclient.loading")
                    .formatted(Formatting.ITALIC, Formatting.GRAY);

            this.isPlayerItSelf = identifier.equals(MorphClient.UNMORPH_STIRNG);

            this.screenSpaceX = screenSpaceX;
            this.screenSpaceY = screenSpaceY;

            this.width = width;
            this.height = height;

            selectedIdentifier.bindTo(manager.selectedIdentifier);
            currentIdentifier.bindTo(manager.currentIdentifier);

            if (identifier.equals(currentIdentifier.get()) || isPlayerItSelf)
                setupEntity(identifier);

            selectedIdentifier.onValueChanged((o, n) ->
            {
                if (!identifier.equals(n) && activationState != ActivationState.CURRENT && activationState != ActivationState.WAITING)
                    activationState = ActivationState.NONE;
            }, true);

            currentIdentifier.onValueChanged((o, n) ->
            {
                n = n == null ? MorphClient.UNMORPH_STIRNG : n;
                o = o == null ? MorphClient.UNMORPH_STIRNG : o;

                var isCurrent = identifier.equals(n);
                var prevIsCurrent = identifier.equals(o);

                if (prevIsCurrent && entity != null && !isPlayerItSelf)
                    entity = EntityCache.getEntity(identifier);

                activationState = isCurrent
                        ? ActivationState.CURRENT
                        : (prevIsCurrent ? ActivationState.NONE : activationState);
            }, true);
        }

        private void trimDisplay(StringVisitable text)
        {
            this.display = Text.literal(text.getString());

            this.addSchedule(() ->
            {
                var targetMultiplier = entity == null ? 0.9 : 0.7;
                var toDisplay = textRenderer.trimToWidth(text, (int)Math.round(this.width * targetMultiplier));
                var trimmed = !toDisplay.getString().equals(text.getString());

                this.display = Text.literal(toDisplay.getString() + (trimmed ? "..." : ""));
            });
        }

        private void setupEntity(String identifier)
        {
            try
            {
                LivingEntity living = EntityCache.getEntity(identifier);

                if (living == null)
                {
                    LivingEntity entity = null;

                    if (isPlayerItSelf)
                    {
                        entity = MinecraftClient.getInstance().player;
                    }
                    else if (identifier.startsWith("player:"))
                    {
                        var nameSplited = identifier.split(":", 2);

                        if (nameSplited.length == 2)
                        {
                            entity = new MorphLocalPlayer(MinecraftClient.getInstance().world,
                                    new GameProfile(UUID.randomUUID(), nameSplited[1]));
                        }
                    }

                    //没有和此ID匹配的实体
                    if (entity == null)
                    {
                        this.trimDisplay(Text.literal(identifier));
                        return;
                    }

                    living = entity;
                }

                this.entity = living;
                this.trimDisplay(entity.getDisplayName());

                entitySize = getEntitySize(entity);
                entityYOffset = getEntityYOffset(entity);

                if (entity.getType() == EntityType.MAGMA_CUBE)
                    ((MagmaCubeEntity) living).setSize(4, false);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }

        private int getEntityYOffset(LivingEntity entity)
        {
            var type = Registries.ENTITY_TYPE.getId(entity.getType());

            return switch (type.toString())
                    {
                        case "minecraft:squid", "minecraft:glow_squid" -> -6;
                        case "minecraft:ghast" -> -3;
                        default -> 0;
                    };
        }

        private int getEntitySize(LivingEntity entity)
        {
            var type = Registries.ENTITY_TYPE.getId(entity.getType());

            return switch (type.toString())
                    {
                        case "minecraft:ender_dragon" -> 3;
                        case "minecraft:squid", "minecraft:glow_squid" -> 10;
                        case "minecraft:magma_cube" ->
                        {
                            ((MagmaCubeEntity) entity).setSize(4, false);
                            yield 8;
                        }
                        case "minecraft:player" -> {
                            yield 8;
                        }
                        default ->
                        {
                            var size = (int) (15 / Math.max(entity.getHeight(), entity.getWidth()));
                            size = Math.max(1, size);

                            yield size;
                        }
                    };
        }

        private final static TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
        {
            this.hovered = mouseX < this.screenSpaceX + width && mouseX > this.screenSpaceX
                    && mouseY < this.screenSpaceY + height && mouseY > this.screenSpaceY;

            if (entity == null)
                CompletableFuture.runAsync(() -> this.setupEntity(identifier));

            var borderColor = 0x00000000;
            var contentColor = 0x00000000;

            if (hovered)
            {
                contentColor = 0xb5333333;
                borderColor = 0xff999999;
            }

            if (activationState != ActivationState.NONE)
            {
                borderColor = switch (activationState)
                        {
                            //ARGB color
                            case SELECTED -> 0xffffaa00;
                            case CURRENT -> 0xffabcdef;
                            case WAITING -> 0xff694400;
                            default -> 0x00000000;
                        };

                contentColor = 0x00333333 + (activationState == ActivationState.CURRENT ? 0xc9000000 : 0xb5000000);

                if (hovered)
                    contentColor += 0x00333333;
            }

            try
            {
                matrices.push();

                if (this.hovered)
                    matrices.translate(0, 0, 512);

                if (activationState == ActivationState.CURRENT)
                    matrices.translate(0, 0, 256);

                DrawableHelper.fill(matrices, screenSpaceX + 1, screenSpaceY + 1,
                        screenSpaceX + width - 1, screenSpaceY + height - 1,
                        contentColor);

                DrawableHelper.drawBorder(matrices, screenSpaceX, screenSpaceY,
                        width, height, borderColor);

                if (entity != null && allowER)
                {
                    var x = screenSpaceX + width - 5;
                    var y = screenSpaceY + height - 2 + entityYOffset;
                    var mX = 30;
                    var mY = 0;

                    if (activationState == ActivationState.CURRENT)
                    {
                        //entitySize = this.getEntitySize(entity);

                        mX = x - mouseX;
                        mY = y - mouseY - (this.height / 2);
                    }

                    if (entity == MinecraftClient.getInstance().player)
                        PlayerRenderHelper.instance.skipRender = true;

                    InventoryScreen.drawEntity(matrices, x, y, entitySize, mX, mY, entity);

                    PlayerRenderHelper.instance.skipRender = false;
                }
            }
            catch (Exception e)
            {
                allowER = false;
                LoggerFactory.getLogger("morph").error(e.getMessage());
                e.printStackTrace();
            }
            finally
            {
                textRenderer.drawWithShadow(matrices, display,
                        screenSpaceX + 5, screenSpaceY + ((height - textRenderer.fontHeight) / 2f), 0xffffffff);

                matrices.pop();
            }
        }

        private boolean hovered;

        @Override
        public boolean isMouseOver(double mouseX, double mouseY)
        {
            return isHovered();
        }

        private boolean allowER = true;

        @Override
        public Selectable.SelectionType getType()
        {
            return (activationState == ActivationState.CURRENT ? Selectable.SelectionType.FOCUSED : Selectable.SelectionType.NONE);
        }

        @NotNull
        private ActivationState activationState = ActivationState.NONE;

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
                var lastFocusType = activationState;

                switch (lastFocusType)
                {
                    case SELECTED ->
                    {
                        activationState = isPlayerItSelf && manager.currentIdentifier.get() == null
                                ? ActivationState.NONE
                                : ActivationState.WAITING;

                        MorphClient.getInstance().sendMorphCommand(this.identifier);
                        playClickSound();
                    }

                    case CURRENT -> manager.selectedIdentifier.set(null);

                    case WAITING -> { }

                    default ->
                    {
                        activationState = ActivationState.SELECTED;
                        playClickSound();
                    }
                }

                return true;
            }
            else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) //Selected + 右键 -> 取消选择
            {
                if (activationState == ActivationState.SELECTED)
                {
                    manager.selectedIdentifier.set(null);
                    playClickSound();
                }
                else if (activationState == ActivationState.CURRENT && !isPlayerItSelf)
                {
                    activationState = ActivationState.WAITING;
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

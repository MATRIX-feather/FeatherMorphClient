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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.*;
import xiamomc.morph.client.entities.MorphLocalPlayer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.morph.client.graphics.InventoryRenderHelper;

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
                if (!identifier.equals(n) && focusType != FocusType.CURRENT && focusType != FocusType.WAITING)
                    focusType = FocusType.NONE;
            }, true);

            currentIdentifier.onValueChanged((o, n) ->
            {
                n = n == null ? MorphClient.UNMORPH_STIRNG : n;

                if (identifier.equals(o) && entity != null) entity = EntityCache.getEntity(n);

                focusType = identifier.equals(n) ? FocusType.CURRENT : FocusType.NONE;
            }, true);
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
                        this.display = Text.literal(identifier);
                        return;
                    }

                    living = entity;
                }

                this.entity = living;
                this.display = entity.getDisplayName();

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
                        default -> 0;
                    };
        }

        private int getEntitySize(LivingEntity entity)
        {
            var type = Registries.ENTITY_TYPE.getId(entity.getType());

            return switch (type.toString())
                    {
                        case "minecraft:ender_dragon" -> 2;
                        case "minecraft:squid", "minecraft:glow_squid" -> 10;
                        case "minecraft:magma_cube" ->
                        {
                            ((MagmaCubeEntity) entity).setSize(4, false);
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

        private final InventoryRenderHelper inventoryRenderHelper = InventoryRenderHelper.getInstance();

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

            if (focusType != FocusType.NONE)
            {
                borderColor = switch (focusType)
                        {
                            //ARGB color
                            case SELECTED -> 0xffffaa00;
                            case CURRENT -> 0xffabcdef;
                            case WAITING -> 0xff694400;
                            default -> 0x00000000;
                        };

                contentColor = 0x00333333 + (focusType == FocusType.CURRENT ? 0xc9000000 : 0xb5000000);

                if (hovered)
                    contentColor += 0x00333333;
            }

            DrawableHelper.fill(matrices, screenSpaceX + 1, screenSpaceY + 1,
                    screenSpaceX + width - 1, screenSpaceY + height - 1,
                    contentColor);

            DrawableHelper.drawBorder(matrices, screenSpaceX, screenSpaceY,
                    width, height, borderColor);

            textRenderer.draw(matrices, display,
                    screenSpaceX + 5, screenSpaceY + ((height - textRenderer.fontHeight) / 2f), 0xffffffff);

            try
            {
                if (entity != null && allowER)
                {
                    var x = screenSpaceX + width - 5;
                    var y = screenSpaceY + height - 2 + entityYOffset;
                    var mX = 30;
                    var mY = 0;

                    if (focusType == FocusType.CURRENT || entity == MinecraftClient.getInstance().player)
                        entitySize = this.getEntitySize(entity);

                    if (focusType != FocusType.NONE)
                    {
                        mX = x - mouseX;
                        mY = y -mouseY;
                    }

                    if (entity == MinecraftClient.getInstance().player)
                        inventoryRenderHelper.onRenderCall(matrices, x, y, entitySize, mX, mY);
                    else
                        InventoryScreen.drawEntity(matrices, x, y, entitySize, mX, mY, entity);
                }
            }
            catch (Exception e)
            {
                allowER = false;
                LoggerFactory.getLogger("morph").error(e.getMessage());
                e.printStackTrace();
            }
        }

        private boolean hovered;

        @Override
        public boolean isMouseOver(double mouseX, double mouseY)
        {
            return isHovered(mouseX, mouseY);
        }

        private boolean allowER = true;

        @Override
        public SelectionType getType()
        {
            return (focusType == FocusType.CURRENT ? SelectionType.FOCUSED : SelectionType.NONE);
        }

        private FocusType focusType;

        private void playClickSound()
        {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            if (!isHovered(mouseX, mouseY)) return false;

            manager.selectedIdentifier.set(this.identifier);

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
            {
                var lastFocusType = focusType;

                switch (lastFocusType)
                {
                    case SELECTED ->
                    {
                        focusType = isPlayerItSelf && manager.currentIdentifier.get() == null
                                ? FocusType.NONE
                                : FocusType.WAITING;

                        MorphClient.getInstance().sendMorphCommand(this.identifier);
                        playClickSound();
                    }

                    case CURRENT -> manager.selectedIdentifier.set(null);

                    case WAITING -> { }

                    default ->
                    {
                        focusType = FocusType.SELECTED;
                        playClickSound();
                    }
                }

                return true;
            }
            else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) //Selected + 右键 -> 取消选择
            {
                if (focusType == FocusType.SELECTED)
                {
                    manager.selectedIdentifier.set(null);
                    playClickSound();
                }
                else if (focusType == FocusType.CURRENT && !isPlayerItSelf)
                {
                    focusType = FocusType.WAITING;
                    MorphClient.getInstance().sendMorphCommand(null);
                    playClickSound();
                }
            }

            return Element.super.mouseClicked(mouseX, mouseY, button);
        }

        private boolean isHovered(double mouseX, double mouseY)
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

        private enum FocusType
        {
            NONE,
            SELECTED,
            WAITING,
            CURRENT
        }
    }
}

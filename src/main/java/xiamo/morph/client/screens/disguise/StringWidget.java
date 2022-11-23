package xiamo.morph.client.screens.disguise;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.MorphClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StringWidget extends ElementListWidget.Entry<StringWidget>
{
    private TextWidget field;

    private String identifier = "???";
    private String name = "???";

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

    public StringWidget(String name)
    {
        initFields(name);
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

    private static class TextWidget implements Selectable, Drawable, Element
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

        private static final Map<String, LivingEntity> stringLivingEntityMap = new Object2ObjectOpenHashMap<>();

        public TextWidget(int screenSpaceX, int screenSpaceY, int width, int height, String identifier)
        {
            this.identifier = identifier;
            this.display = Text.literal(identifier);

            this.screenSpaceX = screenSpaceX;
            this.screenSpaceY = screenSpaceY;

            this.width = width;
            this.height = height;

            MorphClient.selectedIdentifier.onValueChanged((o, n) ->
            {
                if (!identifier.equals(n) && focusType != FocusType.CURRENT && focusType != FocusType.WAITING)
                    focusType = FocusType.NONE;
            }, true);

            MorphClient.currentIdentifier.onValueChanged((o, n) ->
            {
                if (identifier.equals(n)) focusType = FocusType.CURRENT;
                else focusType = FocusType.NONE;
            }, true);

            try
            {
                LivingEntity living = stringLivingEntityMap.getOrDefault(identifier, null);

                if (living == null)
                {
                    Entity entity = null;

                    var entityType = EntityType.get(identifier);

                    if (entityType.isPresent())
                    {
                        var type = entityType.get();

                        entity = type.create(MinecraftClient.getInstance().world);
                    }
                    else if (identifier.equals("morph:unmorph"))
                    {
                        entity = MinecraftClient.getInstance().player;
                    }
                    else if (identifier.startsWith("player:"))
                    {
                        var nameSplited = identifier.split(":", 2);

                        if (nameSplited.length == 2)
                        {
                            entity = new OtherClientPlayerEntity(MinecraftClient.getInstance().world,
                                    new GameProfile(UUID.randomUUID(), nameSplited[1]), null);
                        }
                    }

                    if (!(entity instanceof LivingEntity le)) return;
                    else
                    {
                        living = le;

                        if (entity != MinecraftClient.getInstance().player)
                            stringLivingEntityMap.put(identifier, le);
                    }
                }

                this.entity = living;
                this.display = entity.getDisplayName();

                switch (identifier)
                {
                    case "minecraft:ender_dragon" -> entitySize = 2;
                    case "minecraft:squid" ->
                    {
                        entitySize = 10;
                        entityYOffset = -6;
                    }
                    case "minecraft:magma_cube" ->
                    {
                        ((MagmaCubeEntity) living).setSize(4, false);
                        entitySize = 8;
                    }
                    default ->
                    {
                        entitySize = (int) (15 / Math.max(entity.getHeight(), entity.getWidth()));
                        entitySize = Math.max(1, entitySize);
                    }
                }
            }
            catch (Exception e)
            {
                LoggerFactory.getLogger("morph").error(e.getMessage());
                e.printStackTrace();
            }
        }

        private final static TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
        {
            if (focusType != FocusType.NONE)
            {
                var bordercolor = switch (focusType)
                        {
                            case SELECTED -> 0xffffaa00;
                            case CURRENT -> 0xffabcdef;
                            case WAITING -> 0xff694400;
                            default -> 0x00000000;
                        };

                DrawableHelper.fill(matrices, screenSpaceX, screenSpaceY,
                        screenSpaceX + width, screenSpaceY + height, bordercolor);

                DrawableHelper.fill(matrices, screenSpaceX + 1, screenSpaceY + 1,
                        screenSpaceX + width - 1, screenSpaceY + height - 1,
                        0xff333333);
            }

            textRenderer.draw(matrices, display,
                    screenSpaceX + 5, screenSpaceY + ((height - textRenderer.fontHeight) / 2f), 0xffffffff);

            try
            {
                if (entity != null && allowER)
                {
                    InventoryScreen.drawEntity(screenSpaceX + width - 5, screenSpaceY + height - 2 + entityYOffset,
                            entitySize, 30, 0, entity);
                }
            }
            catch (Exception e)
            {
                allowER = false;
                LoggerFactory.getLogger("morph").error(e.getMessage());
                e.printStackTrace();
            }
        }

        private boolean allowER = true;

        @Override
        public SelectionType getType()
        {
            return (focusType == FocusType.CURRENT ? SelectionType.FOCUSED : SelectionType.NONE);
        }

        private FocusType focusType;

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            var lastFocusType = focusType;

            switch (lastFocusType)
            {
                case SELECTED ->
                {
                    focusType = FocusType.WAITING;
                    MorphClient.getInstance().sendMorphCommand(this.identifier);
                }

                case CURRENT ->
                {
                    focusType = FocusType.WAITING;
                    MorphClient.getInstance().sendMorphCommand(null);
                }

                case WAITING ->
                {
                }

                default ->
                {
                    if (mouseX < this.screenSpaceX + width && mouseX > this.screenSpaceX
                            && mouseY < this.screenSpaceY + height && mouseY > this.screenSpaceY)
                    {
                        MorphClient.selectedIdentifier.set(this.identifier);
                        focusType = FocusType.SELECTED;
                    }
                }
            }

            return Element.super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) { }

        private enum FocusType
        {
            NONE,
            SELECTED,
            WAITING,
            CURRENT
        }
    }
}

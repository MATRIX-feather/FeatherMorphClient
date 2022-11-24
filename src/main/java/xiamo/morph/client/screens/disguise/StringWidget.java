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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.EntityCache;
import xiamo.morph.client.MorphClient;
import xiamo.morph.client.bindables.Bindable;

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

    public void clearChildren()
    {
        children.forEach(TextWidget::dispose);
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

        private void dispose()
        {
            currentIdentifier = null;
            selectedIdentifier = null;
        }

        private Bindable<String> currentIdentifier = new Bindable<>();
        private Bindable<String> selectedIdentifier = new Bindable<>();

        public TextWidget(int screenSpaceX, int screenSpaceY, int width, int height, String identifier)
        {
            this.identifier = identifier;
            this.display = Text.literal(identifier);

            this.screenSpaceX = screenSpaceX;
            this.screenSpaceY = screenSpaceY;

            this.width = width;
            this.height = height;

            selectedIdentifier.bindTo(MorphClient.selectedIdentifier);
            currentIdentifier.bindTo(MorphClient.currentIdentifier);

            selectedIdentifier.onValueChanged((o, n) ->
            {
                if (!identifier.equals(n) && focusType != FocusType.CURRENT && focusType != FocusType.WAITING)
                    focusType = FocusType.NONE;
            }, true);

            currentIdentifier.onValueChanged((o, n) ->
            {
                if (identifier.equals(n))
                {
                    focusType = FocusType.CURRENT;

                    if (entity != null && entity.isRemoved()) entity = EntityCache.getEntity(n);
                }
                else focusType = FocusType.NONE;
            }, true);

            try
            {
                LivingEntity living = EntityCache.getEntity(identifier);

                if (living == null)
                {
                    LivingEntity entity = null;

                    if (identifier.equals("morph:unmorph"))
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

                    if (entity == null) return; //没有和此ID匹配的实体

                    living = entity;
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
                    var x = screenSpaceX + width - 5;
                    var y = screenSpaceY + height - 2 + entityYOffset;
                    var mX = 30;
                    var mY = 0;

                    if (focusType != FocusType.NONE)
                    {
                        mX = x - mouseX;
                        mY = y -mouseY;
                    }

                    InventoryScreen.drawEntity(x, y, entitySize, mX, mY, entity);
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
            if (button == 0)
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
                        if (MorphClient.selectedIdentifier.get() != null)
                            MorphClient.selectedIdentifier.set(null);
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

                return true;
            }
            else if (button == 1) //Selected + 右键 -> 取消选择
            {
                if (focusType == FocusType.SELECTED)
                {
                    MorphClient.selectedIdentifier.set(null);
                }
                else if (focusType == FocusType.CURRENT)
                {
                    focusType = FocusType.WAITING;
                    MorphClient.getInstance().sendMorphCommand(null);
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

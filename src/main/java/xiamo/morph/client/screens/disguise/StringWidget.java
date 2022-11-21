package xiamo.morph.client.screens.disguise;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.MorphClient;

import java.util.List;

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
        private final Text text;

        int screenSpaceY = 0;
        int screenSpaceX = 0;

        int width = 0;
        int height = 0;
        private boolean hovered;

        public TextWidget(int screenSpaceX, int screenSpaceY, int width, int height, String identifier)
        {
            this.identifier = identifier;
            this.text = Text.literal(identifier);

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

            textRenderer.draw(matrices, text,
                    screenSpaceX + 5, screenSpaceY + ((height - textRenderer.fontHeight) / 2f), 0xffffffff);
        }

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

            if (mouseX < this.screenSpaceX + width && mouseX > this.screenSpaceX
                    && mouseY < this.screenSpaceY + height && mouseY > this.screenSpaceY
                    && focusType == FocusType.NONE)
            {
                focusType = FocusType.SELECTED;
            }

            if (lastFocusType == FocusType.SELECTED && this.identifier.equals(MorphClient.selectedIdentifier.get()))
            {
                focusType = FocusType.WAITING;
                MorphClient.getInstance().sendMorphCommand(this.identifier);
            }
            else
                MorphClient.selectedIdentifier.set(this.identifier);

            var logger = LoggerFactory.getLogger("morph");

            logger.info("press " + identifier + " with X " + mouseX + " and Y " + mouseY);

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

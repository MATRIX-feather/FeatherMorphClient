package xiamomc.morph.client.screens.disguise;

import me.shedaniel.math.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.*;
import xiamomc.morph.client.graphics.color.ColorUtils;
import xiamomc.morph.client.graphics.color.MaterialColors;
import xiamomc.morph.client.graphics.container.Container;
import xiamomc.morph.client.graphics.container.FlowContainer;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.morph.client.screens.FeatherScreen;

public class DisguiseScreenNew extends FeatherScreen
{
    private final MGridWidget grid = new MGridWidget();

    protected DisguiseScreenNew(Text title)
    {
        super(title);

        grid.setSpacing(2);
        grid.setRowSpacing(2);
    }

    @Override
    protected void onScreenEnter(@Nullable Screen lastScreen)
    {
        super.onScreenEnter(lastScreen);

        float flowWidthRatio = 0.6f;
        var otherContainer = new Container();
        otherContainer.setRelativeSizeAxes(Axes.Both);
        otherContainer.setSize(new Vector2f(1 - flowWidthRatio, 1));
        otherContainer.setAnchor(Anchor.TopRight);
        otherContainer.setX(Math.round(-this.width * (1 - flowWidthRatio)));
        otherContainer.setY(20);

        //region DisguiseSelectFlow

        var flow = new FlowContainer();
        flow.setSpacing(3);
        flow.setFlowAxes(Axes.Both);
        flow.setSize(new Vector2f(flowWidthRatio, 1));
        flow.setRelativeSizeAxes(Axes.Both);
        flow.setMargin(new Margin(5, 0, 20 + 5, 0));

        MorphClient.getInstance().morphManager.getAvailableMorphs().forEach(id ->
        {
            var widget = new DisplayWdgt(id);
            widget.setSize(new Vector2f(36, 48));
            flow.add(widget);
        });

        //endregion DisguiseSelectFlow

        //region TitleFlow

        var titleFlow = new FlowContainer();
        titleFlow.setFlowAxes(Axes.Y);
        titleFlow.setSize(new Vector2f(1, 20));
        titleFlow.setRelativeSizeAxes(Axes.X);

        var box = new Box();
        box.color = ColorUtils.forOpacity(ColorUtils.fromHex("#000000"), 0.5f).getColor();
        box.setRelativeSizeAxes(Axes.X);
        box.setSize(new Vector2f(1, 19));

        var line = new Box();
        line.color = MaterialColors.Blue500.getColor();
        line.setRelativeSizeAxes(Axes.X);

        titleFlow.addRange(box, line);

        //endregion TitleFlow

        //region Container

        var box2 = new Box();
        box2.setRelativeSizeAxes(Axes.Both);
        box2.color = ColorUtils.forOpacity(ColorUtils.fromHex("#000000"), 0.4f).getColor();
        otherContainer.add(box2);

        //endregion Container

        var children = new IMDrawable[]
        {
            flow,
            titleFlow,
            otherContainer
        };
        this.addRange(children);
    }

    private class DisplayWdgt extends Container
    {
        private final String identifier;

        public DisplayWdgt(String identifier)
        {
            this.identifier = identifier;
            this.display = new EntityDisplay(identifier, true, EntityDisplay.InitialSetupMethod.ASYNC);

            display.setParent(this);
            display.setRelativeSizeAxes(Axes.Both);
            display.setSize(new Vector2f(0.6f, 0.6f));
            display.setAnchor(Anchor.Centre);

            nameText.setRelativeSizeAxes(Axes.X);
            nameText.setAnchor(Anchor.BottomLeft);
            nameText.setY(15);
            nameText.setParent(this);

            this.setMasking(true);
            //this.setPadding(new Margin(2, 0, 0, 1));

            display.postEntitySetup = () ->
            {
                var entity = display.getDisplayingEntity();

                if (entity != null)
                    nameText.setText(entity.getDisplayName());
            };

            this.add(nameText);
        }

        private final EntityDisplay display;

        @Override
        protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
        {
            display.render(context, 30, -6, delta);

            super.onRender(context, mouseX, mouseY, delta);

            if (contentColor.get() != null)
                context.fill(0, 0, finalWidth, finalHeight, contentColor.get().getColor());

            context.drawBorder(0, 0, finalWidth, finalHeight, ColorUtils.fromHex("#888888").getColor() );
        }

        private final DrawableText nameText = new DrawableText();

        private final Recorder<Color> borderColor = new Recorder<>(null);
        private final Recorder<Color> contentColor = new Recorder<>(null);

        private void fadeContentColor(Color colorNext)
        {
            if (contentColor.get() == null)
                contentColor.set(colorNext);

            Transformer.transform(this.contentColor, colorNext, 300, Easing.OutExpo);
        }

        private void fadeBorderColor(Color colorNext)
        {
            if (borderColor.get() == null)
                borderColor.set(colorNext);

            Transformer.transform(this.borderColor, colorNext, 300, Easing.OutExpo);
        }

        @Override
        protected void onHover()
        {
            var colorNext = ColorUtils.fromHex("#666666");
            colorNext = ColorUtils.forOpacity(colorNext, 0.6f);
            fadeContentColor(colorNext);

            nameText.moveToY(0, 600, Easing.OutQuint);
            nameText.fadeIn(600, Easing.OutQuint);
        }

        @Override
        protected void onHoverLost()
        {
            var colorNext = ColorUtils.fromHex("#666666");
            colorNext = ColorUtils.forOpacity(colorNext, 0.3f);
            fadeContentColor(colorNext);

            nameText.moveToY(15, 600, Easing.OutQuint);
            nameText.fadeTo(0.1f, 600, Easing.OutQuint);
        }
    }
}

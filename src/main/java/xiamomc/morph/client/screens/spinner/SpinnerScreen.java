package xiamomc.morph.client.screens.spinner;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.text.Text;
import xiamomc.morph.client.graphics.Anchor;
import xiamomc.morph.client.screens.FeatherScreen;

import java.util.List;

public abstract class SpinnerScreen<T extends ClickableSpinnerWidget> extends FeatherScreen
{
    protected SpinnerScreen(Text title)
    {
        super(title);
    }

    protected abstract T createWidget();

    protected final List<T> spinnerWidgets = new ObjectArrayList<>();

    protected abstract int getWidgetSize();

    protected T addSingleWidget(int xOffset, int yOffset)
    {
        var widget = createWidget();
        widget.setAnchor(Anchor.Centre);

        var size = this.getWidgetSize();
        widget.setWidth(size);
        widget.setHeight(size);

        widget.setX(xOffset);
        widget.setY(yOffset);

        //widget.onClick(() -> MorphClient.getInstance().schedule(this::tryClose));

        this.add(widget);
        spinnerWidgets.add(widget);

        return widget;
    }

}

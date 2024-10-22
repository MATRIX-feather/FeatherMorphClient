package xyz.nifeather.morph.client.screens.spinner;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.text.Text;
import xyz.nifeather.morph.client.graphics.Anchor;
import xyz.nifeather.morph.client.screens.FeatherScreen;

import java.util.List;

public abstract class SpinnerScreen<T extends ClickableSpinnerWidget> extends FeatherScreen
{
    protected SpinnerScreen(Text title)
    {
        super(title);
    }

    protected abstract T createWidget();

    protected final List<T> spinnerWidgets = new ObjectArrayList<>();

    protected abstract int getWidgetWidth();
    protected abstract int getWidgetHeight();

    protected T addSingleWidget(int xOffset, int yOffset)
    {
        var widget = createWidget();
        widget.setAnchor(Anchor.Centre);

        widget.setWidth(getWidgetWidth());
        widget.setHeight(getWidgetHeight());

        widget.setX(xOffset);
        widget.setY(yOffset);

        //widget.onClick(() -> MorphClient.getInstance().schedule(this::tryClose));

        this.add(widget);
        spinnerWidgets.add(widget);

        return widget;
    }

}

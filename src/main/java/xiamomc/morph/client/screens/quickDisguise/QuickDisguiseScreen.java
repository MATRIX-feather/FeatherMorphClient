package xiamomc.morph.client.screens.quickDisguise;

import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Text;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.Anchor;
import xiamomc.morph.client.graphics.Axes;
import xiamomc.morph.client.graphics.container.FlowContainer;
import xiamomc.morph.client.screens.spinner.ClickableSpinnerWidget;
import xiamomc.morph.client.screens.spinner.SpinnerScreen;

public class QuickDisguiseScreen extends SpinnerScreen<QDWidget>
{
    private final MorphClient morphClient;

    private final ClientMorphManager morphManager;

    public QuickDisguiseScreen()
    {
        super(Text.literal("Quick disguise configuration screen"));

        this.morphClient = MorphClient.getInstance();
        this.morphManager = morphClient.morphManager;

        var flow = new FlowContainer();
        flow.addRange(
                this.createWidget(),
                this.createWidget(),
                this.createWidget(),
                this.createWidget()
        );
        flow.setFlowAxes(Axes.Y);
        flow.setSpacing(5);
        flow.setRelativeSizeAxes(Axes.Both);
        flow.setSize(new Vector2f(0.7f, 0.5f));
        flow.setAnchor(Anchor.Centre);

        this.add(flow);
    }

    @Override
    protected QDWidget createWidget()
    {
        var widget = new QDWidget();

        widget.setRelativeSizeAxes(Axes.X);
        widget.setHeight(this.getWidgetHeight());
        //widget.setAnchor(Anchor.Centre);

        return widget;
    }

    @Override
    protected int getWidgetWidth()
    {
        return 100;
    }

    @Override
    protected int getWidgetHeight()
    {
        return 30;
    }
}

package xiamomc.morph.client.screens.quickDisguise;

import net.minecraft.text.Text;
import xiamomc.morph.client.screens.spinner.ClickableSpinnerWidget;
import xiamomc.morph.client.screens.spinner.SpinnerScreen;

public class QuickDisguiseScreen extends SpinnerScreen<QDWidget>
{
    protected QuickDisguiseScreen(Text title)
    {
        super(title);
    }

    @Override
    protected QDWidget createWidget() {
        return new QDWidget();
    }

    @Override
    protected int getWidgetSize() {
        return 55;
    }
}

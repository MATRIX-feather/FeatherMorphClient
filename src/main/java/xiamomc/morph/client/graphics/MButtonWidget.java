package xiamomc.morph.client.graphics;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MButtonWidget extends ButtonWidget implements IMDrawable
{
    protected MButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, NarrationSupplier narrationSupplier)
    {
        super(x, y, width, height, message, onPress, narrationSupplier);
    }

    public static MButtonWidget from(ButtonWidget widget, PressAction onPress)
    {
        return new MButtonWidget(
                widget.getX(), widget.getY(),
                widget.getWidth(), widget.getHeight(),
                widget.getMessage(), onPress,
                ButtonWidget.DEFAULT_NARRATION_SUPPLIER
        );
    }
}

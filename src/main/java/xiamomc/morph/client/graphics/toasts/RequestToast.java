package xiamomc.morph.client.graphics.toasts;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import xiamomc.morph.client.graphics.color.MaterialColors;
import xiamomc.morph.network.commands.S2C.S2CRequestCommand;

public class RequestToast extends LinedToast
{
    @Override
    protected boolean fadeInOnEnter()
    {
        return true;
    }

    public RequestToast(S2CRequestCommand.Type type, String sourceName)
    {
        var color = switch (type)
        {
            case RequestExpired, RequestExpiredOwner -> MaterialColors.Orange500;
            case RequestAccepted -> MaterialColors.Green400;
            case RequestDenied -> MaterialColors.Red400;
            default -> MaterialColors.Indigo500;
        };

        this.setLineColor(color);

        Text text, desc;

        if (type == S2CRequestCommand.Type.RequestSend)
            text = Text.translatable("text.morphclient.toast.request.send");
        else if (type == S2CRequestCommand.Type.RequestExpired || type == S2CRequestCommand.Type.RequestExpiredOwner)
            text = Text.translatable("text.morphclient.toast.request.expire");
        else if (type == S2CRequestCommand.Type.NewRequest)
            text = Text.translatable("text.morphclient.toast.request.receive");
        else
            text = Text.translatable(type == S2CRequestCommand.Type.RequestAccepted
                    ? "text.morphclient.toast.request.accepted"
                    : "text.morphclient.toast.request.denied");

        this.setTitle(text);

        desc = Text.translatable(type.isRequestOwner()
                ? "text.morphclient.toast.request.to"
                : "text.morphclient.toast.request.from", sourceName);

        this.setDescription(desc);
    }

    @Override
    protected float getTextStartX()
    {
        return 8;
    }

    @Override
    protected int getTextWidth()
    {
        return (int) (this.getWidth() * 0.85F);
    }

    @Override
    protected void postBackgroundDrawing(DrawContext matrices, ToastManager manager, long startTime)
    {
        super.postBackgroundDrawing(matrices, manager, startTime);
    }
}

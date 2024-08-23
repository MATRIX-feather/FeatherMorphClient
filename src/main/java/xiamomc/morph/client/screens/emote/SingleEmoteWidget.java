package xiamomc.morph.client.screens.emote;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.graphics.*;
import xiamomc.morph.client.screens.spinner.ClickableSpinnerWidget;

public class SingleEmoteWidget extends ClickableSpinnerWidget
{
    public SingleEmoteWidget()
    {
        setText(Text.translatable("gui.none"));

        title.setAnchor(Anchor.Centre);

        // Set depth because in vanilla it seems that you can't draw text behind an area where DrawContext#fill() drew
        title.setDepth(-5);
        this.add(title);
    }

    private final DrawableText title = new DrawableText();

    public void setText(Text text)
    {
        title.setText(text);
    }

    @Nullable
    private String emote;

    public void setEmote(String identifier)
    {
        this.emote = identifier;
        this.setText(Text.translatable("emote.morphclient." + identifier));
    }

    @Nullable
    public String getEmote()
    {
        return emote;
    }
}

package xiamomc.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import xiamomc.pluginbase.Annotations.Initializer;

public class DrawableText extends MDrawable
{
    private static final Text defaultText = Text.literal("");
    private static final TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

    private Text text = defaultText;

    public void setText(Text text)
    {
        this.text = text;
    }

    public void setText(String text)
    {
        this.text = Text.literal(text);
    }

    public Text getText()
    {
        return text;
    }

    public DrawableText(String text)
    {
        this.setText(text);
    }

    public DrawableText(Text text)
    {
        this.setText(text);
    }

    public DrawableText()
    {
    }

    private int color = 0xffffffff;

    public void setColor(int c)
    {
        this.color = c;
    }

    public int getColor()
    {
        return color;
    }

    @Initializer
    private void load()
    {
    }

    @Override
    public void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        renderer.draw(matrices, text, 0, 0, color);

        this.width = renderer.getWidth(text);
        this.height = renderer.fontHeight;
    }
}

package xiamomc.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class DrawableText implements Drawable
{
    private static final Text defaultText = Text.literal("");
    private static final TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

    private Text text = defaultText;

    private int screenX;
    private int screenY;

    public int getScreenX()
    {
        return screenX;
    }

    public void setScreenX(int x)
    {
        this.screenX = x;
    }

    public int getScreenY()
    {
        return screenY;
    }

    public void setScreenY(int y)
    {
        this.screenY = y;
    }

    private int width;
    private int height;

    public int getWidth()
    {
        return width;
    }

    public void setWidth(int w)
    {
        this.width = w;
    }

    public int getHeight()
    {
        return height;
    }

    public void setHeight(int h)
    {
        this.height = h;
    }

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

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        RenderSystem.enableBlend();
        renderer.draw(matrices, text, this.screenX, this.screenY, color);
    }
}

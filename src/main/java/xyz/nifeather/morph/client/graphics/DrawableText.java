package xyz.nifeather.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xyz.nifeather.morph.client.graphics.color.Colors;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;

public class DrawableText extends MDrawable
{
    private static final Text defaultText = Text.literal("");
    private static final TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

    private Text text = defaultText;

    public void setText(Text text)
    {
        this.text = text;
        updateTextWidth();
    }

    public void setText(String text)
    {
        this.text = Text.literal(text);
        updateTextWidth();
    }

    private void updateTextWidth()
    {
        if (RenderSystem.isOnRenderThread())
            this.setWidth(renderer.getWidth(text));
        else
            this.addSchedule(() -> this.setWidth(renderer.getWidth(text)));
    }

    public Text getText()
    {
        return text;
    }

    public DrawableText(String text)
    {
        this();
        this.setText(text);
    }

    public DrawableText(Text text)
    {
        this();
        this.setText(text);
    }

    public DrawableText()
    {
        this.setHeight(renderer.fontHeight);
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

    private boolean drawShadow = false;

    public boolean drawShadow()
    {
        return drawShadow;
    }

    public void setDrawShadow(boolean val)
    {
        this.drawShadow = val;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        context.drawText(renderer, text, 0, 0, color, drawShadow);
    }

    @Override
    public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation)
    {
        return null;
    }
}

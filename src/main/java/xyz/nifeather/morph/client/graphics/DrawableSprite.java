package xyz.nifeather.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class DrawableSprite extends MDrawable
{
    private final Identifier textureIdentifier;
    private final boolean isGuiTexture;

    public DrawableSprite(Identifier textureIdentifier, boolean isGuiTexture)
    {
        this.textureIdentifier = textureIdentifier;
        this.isGuiTexture = isGuiTexture;
    }

    public DrawableSprite(Identifier textureIdentifier)
    {
        this(textureIdentifier, true);
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        int texWidth = Math.round(this.getRenderWidth());
        int texHeight = Math.round(this.getRenderHeight());

        if (isGuiTexture)
        {
            context.drawGuiTexture(RenderLayer::getGuiTextured, textureIdentifier,
                    0, 0,
                    texWidth, texHeight);
        }
        else
        {
            context.drawTexture(RenderLayer::getGuiTextured, textureIdentifier,
                    0, 0,
                    0, 0,
                    texWidth, texHeight,
                    texWidth, texHeight);
        }
    }

    @Override
    public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation)
    {
        return null;
    }
}

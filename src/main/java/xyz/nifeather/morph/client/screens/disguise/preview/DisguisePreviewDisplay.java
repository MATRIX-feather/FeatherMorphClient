package xyz.nifeather.morph.client.screens.disguise.preview;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.graphics.EntityDisplay;

public class DisguisePreviewDisplay extends EntityDisplay
{
    public DisguisePreviewDisplay(String rawIdentifier, boolean displayLoadingIfNotValid, InitialSetupMethod initialSetupMethod)
    {
        super(rawIdentifier, displayLoadingIfNotValid, initialSetupMethod);
    }

    public DisguisePreviewDisplay(String id)
    {
        super(id);
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        var matrices = context.getMatrices();

        matrices.push();

        matrices.translate(0, 0, 100);

        var mX = Math.round(this.getScreenSpaceX() + this.getRenderWidth() / 2f - 30);
        var mY = Math.round(this.getScreenSpaceY() + this.getRenderHeight() / 2f);

        try
        {
            super.onRender(context, mX, mY, delta);
        }
        finally
        {
            matrices.pop();
        }
    }

    @Override
    public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation)
    {
        return null;
    }
}

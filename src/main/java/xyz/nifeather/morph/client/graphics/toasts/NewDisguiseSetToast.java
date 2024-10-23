package xyz.nifeather.morph.client.graphics.toasts;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xiamomc.pluginbase.Annotations.Initializer;

import java.util.concurrent.atomic.AtomicBoolean;

public class NewDisguiseSetToast extends LinedToast
{
    public NewDisguiseSetToast(boolean allGone)
    {
        this.allGone.set(allGone);
    }

    @Override
    protected boolean fadeInOnEnter()
    {
        return true;
    }

    private final AtomicBoolean allGone = new AtomicBoolean(false);

    @Initializer
    private void load()
    {
        var transId = "text.morphclient.toast.new_disguises";
        setTitle(Text.translatable(transId));
        setDescription(Text.translatable(transId + (allGone.get() ? ".all_gone" : ".desc"), Text.keybind("key.morphclient.morph").formatted(Formatting.ITALIC)));
        setLineColor(ColorUtils.fromHex("#009688"));
    }

    @Override
    public int getWidth()
    {
        var textRenderer = MinecraftClient.getInstance().textRenderer;

        var desc = getDescription();
        var title = getTitle();

        var descLength = textRenderer.getWidth(desc == null ? Text.EMPTY : desc);
        var titleLength = textRenderer.getWidth(title == null ? Text.EMPTY : title);
        var max1 = Math.max(descLength, titleLength) + 20;

        return Math.max(super.getWidth(), max1);
    }

    private static final Identifier TEX = Identifier.of(Identifier.DEFAULT_NAMESPACE, "textures/gui/sprites/icon/info.png");

    @Override
    protected void postBackgroundDrawing(DrawContext context, long startTime)
    {
        super.postBackgroundDrawing(context, startTime);

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, TEX);

        context.drawGuiTexture(RenderLayer::getGuiTextured, TEX, this.getWidth() / 16 - 2, 6, 20, 20);
    }
}

package xiamomc.morph.client.screens;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.graphics.IMDrawable;
import xiamomc.morph.client.graphics.MButtonWidget;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FeatherScreen extends Screen implements IMDrawable
{
    protected FeatherScreen(Text title) {
        super(title);
    }

    private boolean isInitialInitialize = true;

    @Override
    protected void init()
    {
        if (isInitialInitialize)
        {
            var last = lastScreen;
            lastScreen = null;

            this.onScreenEnter(last);
            this.onScreenResize();
            isInitialInitialize = false;
        }
        else
        {
            this.onScreenResize();

            this.children.forEach(d ->
            {
                super.addDrawable(d);
                super.addSelectableChild((Element & Selectable) d);
            });
        }

        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        if (!layoutValid.get())
            this.clearAndInit();

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void removed()
    {
        isInitialInitialize = true;

        var next = nextScreen;
        nextScreen = null;

        this.onScreenExit(next);

        this.clearChildren();
        super.removed();
    }

    //region ChildrenV2

    private final AtomicBoolean layoutValid = new AtomicBoolean(false);

    protected void invalidateLayout()
    {
        layoutValid.set(false);
    }

    protected boolean layoutValidate()
    {
        return layoutValid.get();
    }

    private final List<IMDrawable> children = new ObjectArrayList<>();

    protected void add(IMDrawable drawable)
    {
        this.add(drawable, true);
    }

    private void add(IMDrawable drawable, boolean invalidateLayout)
    {
        if (this.contains(drawable)) return;

        children.add(drawable);

        if (invalidateLayout)
            invalidateLayout();
    }

    protected void addRange(IMDrawable[] drawables)
    {
        for (var drawable : drawables)
            this.add(drawable, false);

        invalidateLayout();
    }

    protected void addRange(List<IMDrawable> drawables)
    {
        drawables.forEach(this::add);
        invalidateLayout();
    }

    protected void remove(IMDrawable drawable)
    {
        children.remove(drawable);
        invalidateLayout();
    }

    protected boolean contains(IMDrawable drawable)
    {
        return children.contains(drawable);
    }

    //endregion

    //region Minecraft interface children handling

    @Override
    @Deprecated(forRemoval = true)
    protected <T extends Drawable> T addDrawable(T drawable)
    {
        throw new InvalidOperationException("May not use addDrawable to add children");
    }

    @Override
    @Deprecated(forRemoval = true)
    protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement)
    {
        throw new InvalidOperationException("May not use addDrawableChild to add children");
    }

    @Override
    @Deprecated(forRemoval = true)
    protected <T extends Element & Selectable> T addSelectableChild(T child)
    {
        throw new InvalidOperationException("May not use addSelectableChild to add children");
    }

    @Override
    @Deprecated(forRemoval = true)
    protected void clearAndInit()
    {
        super.clearAndInit();
    }

    private static class InvalidOperationException extends RuntimeException
    {
        public InvalidOperationException() {
        }

        public InvalidOperationException(String message) {
            super(message);
        }

        public InvalidOperationException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidOperationException(Throwable cause) {
            super(cause);
        }
    }

    //endregion

    protected boolean isCurrent()
    {
        return MinecraftClient.getInstance().currentScreen == this;
    }

    protected void onScreenResize()
    {
    }

    private FeatherScreen lastScreen;
    private FeatherScreen nextScreen;

    protected void onScreenEnter(@Nullable FeatherScreen lastScreen)
    {
    }

    protected void onScreenExit(@Nullable FeatherScreen nextScreen)
    {
    }

    protected MButtonWidget buildButtonWidget(int x, int y, int width, int height, Text text, ButtonWidget.PressAction action)
    {
        var builder = ButtonWidget.builder(text, action);

        builder.dimensions(x, y, width, height);

        return MButtonWidget.from(builder.build(), action);
    }

    protected void push(FeatherScreen screen)
    {
        screen.lastScreen = this;
        this.nextScreen = screen;

        MinecraftClient.getInstance().setScreen(screen);
    }

    //region Narratable Selectable

    @Override
    public void appendNarrations(NarrationMessageBuilder builder)
    {
    }

    @Override
    public SelectionType getType()
    {
        return SelectionType.NONE;
    }

    //endregion
}

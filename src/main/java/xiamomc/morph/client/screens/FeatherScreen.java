package xiamomc.morph.client.screens;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.graphics.IMDrawable;
import xiamomc.morph.client.graphics.MButtonWidget;
import xiamomc.morph.client.graphics.color.MaterialColors;
import xiamomc.morph.client.utilties.Screens;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FeatherScreen extends Screen implements IMDrawable
{
    protected FeatherScreen(Text title) {
        super(title);
    }

    protected boolean isInitialInitialize = true;

    private Screen lastScreen;
    private Screen nextScreen;

    @Override
    protected void init()
    {
        var last = lastScreen;

        if (last != null && last == nextScreen)
            this.onScreenResume(last);
        else if (isInitialInitialize)
            this.onScreenEnter(last);

        lastScreen = null;
        nextScreen = null;

        if (isInitialInitialize)
        {
            this.onScreenResize();
            isInitialInitialize = false;
        }
        else
        {
            this.onScreenResize();

            clearChildren();
            this.children.forEach(super::addDrawableChild);
        }

        super.init();
    }

    @Override
    public void onDisplayed()
    {
        lastScreen = Screens.getInstance().last;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        if (!layoutValid.get())
            this.clearAndInit();

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void clearAndInit()
    {
        super.clearAndInit();
        layoutValid.set(true);
    }

    @Override
    public void removed()
    {
        var next = Screens.getInstance().next;
        if (next == null)
            isInitialInitialize = true;

        nextScreen = next;
        this.onScreenExit(next);

        invalidateLayout();

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

    protected void onScreenEnter(@Nullable Screen lastScreen)
    {
    }

    protected void onScreenExit(@Nullable Screen nextScreen)
    {
    }

    protected void onScreenResume(@Nullable Screen lastScreen)
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
        return SelectionType.HOVERED;
    }

    //endregion
}

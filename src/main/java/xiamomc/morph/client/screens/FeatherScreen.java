package xiamomc.morph.client.screens;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.MorphClient;

import java.util.List;

public abstract class FeatherScreen extends Screen
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

            drawables.forEach(d ->
            {
                super.addDrawable(d);
            });

            elements.forEach(e ->
            {
                if (e instanceof Selectable selectable)
                {
                    super.addSelectableChild((Element & Selectable) selectable);
                }
            });
        }

        super.init();
    }

    @Override
    public void removed()
    {
        isInitialInitialize = true;

        var next = nextScreen;
        nextScreen = null;

        this.onScreenExit(next);

        this.drawables.clear();
        this.elements.clear();
        this.clearChildren();
        super.removed();
    }

    //region Children

    private final List<Drawable> drawables = new ObjectArrayList<>();
    private final List<Element> elements = new ObjectArrayList<>();

    protected <T extends Drawable> boolean contains(T drawable)
    {
        return drawables.contains(drawable);
    }

    protected <T extends Element> boolean contains(T element)
    {
        return elements.contains(element);
    }

    @Override
    protected <T extends Drawable> T addDrawable(T drawable)
    {
        if (!drawables.contains(drawable))
        {
            //throw new RuntimeException("We already have this drawable!");
            drawables.add(drawable);
        }

        return super.addDrawable(drawable);
    }

    @Override
    protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement)
    {
        if (!drawables.contains(drawableElement))
        {
            //throw new RuntimeException("We already have this drawable!");
            drawables.add(drawableElement);
        }

        return super.addDrawableChild(drawableElement);
    }

    @Override
    protected <T extends Element & Selectable> T addSelectableChild(T child)
    {
        if (!elements.contains(child))
        {
            //throw new RuntimeException("We already have this element!");

            elements.add(child);
        }
        return super.addSelectableChild(child);
    }

    @Override
    protected void clearAndInit()
    {
        super.clearAndInit();
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

    protected ButtonWidget buildWidget(int x, int y, int width, int height, Text text, ButtonWidget.PressAction action)
    {
        var builder = ButtonWidget.builder(text, action);

        builder.dimensions(x, y, width, height);

        return builder.build();
    }

    protected void push(FeatherScreen screen)
    {
        screen.lastScreen = this;
        this.nextScreen = screen;

        MinecraftClient.getInstance().setScreen(screen);
    }
}

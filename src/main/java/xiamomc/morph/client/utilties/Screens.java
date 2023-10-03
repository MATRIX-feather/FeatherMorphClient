package xiamomc.morph.client.utilties;

import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Bindables.Bindable;

public class Screens
{
    public static Screens getInstance()
    {
        if (instance == null) instance = new Screens();

        return instance;
    }

    private static Screens instance;

    @Nullable
    public Screen next;

    @Nullable
    public Screen last;

    public Bindable<Screen> currentScreen = new Bindable<>(null);

    public void onChange(Screen last, Screen next)
    {
        this.next = next;
        this.last = last;

        currentScreen.set(next);
    }
}
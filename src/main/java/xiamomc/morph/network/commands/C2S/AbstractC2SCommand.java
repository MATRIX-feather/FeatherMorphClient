package xiamomc.morph.network.commands.C2S;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractC2SCommand<T>
{
    public AbstractC2SCommand(@Nullable T argument)
    {
        this.arguments = toList(argument);
    }

    public AbstractC2SCommand(@Nullable T[] arguments)
    {
        this.arguments = toList(arguments);
    }

    @NotNull
    protected List<T> arguments;

    @SafeVarargs
    private List<T> toList(T... elements)
    {
        if (elements == null)
            return new ArrayList<>();

        return Arrays.stream(elements).toList();
    }

    public abstract String getBaseName();

    public void onCommand(String[] arguments) { }

    public String buildCommand()
    {
        return getBaseName();
    }

    //region Utilities
    @Nullable
    protected T getArgumentAt(int index)
    {
        return index >= arguments.size() ? null : arguments.get(index);
    }

    @NotNull
    protected T getArgumentAt(int index, @NotNull T defaultValue)
    {
        var val = this.getArgumentAt(index);

        return val == null ? defaultValue : val;
    }
    //endregion Utilities
}

package xiamomc.morph.network.commands.C2S;

import org.jetbrains.annotations.Nullable;

public class C2SMorphCommand extends AbstractC2SCommand<String>
{
    public C2SMorphCommand(@Nullable String argument)
    {
        super(argument);
    }

    public C2SMorphCommand(@Nullable String... arguments)
    {
        super(arguments);
    }

    @Override
    public String getBaseName()
    {
        return "morph";
    }

    @Override
    public String buildCommand()
    {
        return super.buildCommand() + " " + getArgumentAt(0, "");
    }
}

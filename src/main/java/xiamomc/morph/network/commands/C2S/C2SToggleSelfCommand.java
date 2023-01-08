package xiamomc.morph.network.commands.C2S;

import org.jetbrains.annotations.Nullable;

public class C2SToggleSelfCommand extends AbstractC2SCommand<String>
{
    public C2SToggleSelfCommand(String argument)
    {
        super(argument);
    }

    public C2SToggleSelfCommand(String... arguments)
    {
        super(arguments);
    }

    @Override
    public String getBaseName()
    {
        return "toggleself";
    }

    @Override
    public String buildCommand()
    {
        return super.buildCommand() + " " + getArgumentAt(0) + " " + getArgumentAt(1, "");
    }
}

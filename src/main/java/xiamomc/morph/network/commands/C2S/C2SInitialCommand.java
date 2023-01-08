package xiamomc.morph.network.commands.C2S;

import org.jetbrains.annotations.Nullable;

public class C2SInitialCommand extends AbstractC2SCommand<String>
{
    public C2SInitialCommand()
    {
        super("");
    }

    @Override
    public String getBaseName()
    {
        return "initial";
    }
}

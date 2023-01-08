package xiamomc.morph.network.commands.C2S;

import org.jetbrains.annotations.Nullable;

public class C2SSkillCommand extends AbstractC2SCommand<String>
{
    public C2SSkillCommand()
    {
        super("");
    }

    @Override
    public String getBaseName()
    {
        return "skill";
    }
}

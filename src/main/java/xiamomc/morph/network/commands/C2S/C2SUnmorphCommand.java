package xiamomc.morph.network.commands.C2S;

public class C2SUnmorphCommand extends AbstractC2SCommand<String>
{
    public C2SUnmorphCommand()
    {
        super("");
    }

    @Override
    public String getBaseName()
    {
        return "unmorph";
    }
}

package xiamomc.morph.client.network.commands.S2C.query;

import org.jetbrains.annotations.NotNull;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.network.commands.S2C.S2CCommandWithChild;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

import java.util.List;

public final class S2CQueryCommand extends S2CCommandWithChild<String>
{
    public S2CQueryCommand(ClientMorphManager morphManager)
    {
        this.morphManager = morphManager;
        this.subCmds = List.of(
                new S2CQueryAddCommand(morphManager),
                new S2CQueryRemoveCommand(morphManager),
                new S2CQuerySetCommand(morphManager)
        );
    }

    private final ClientMorphManager morphManager;

    private final List<AbstractS2CCommand<?>> subCmds;

    @Override
    protected @NotNull List<AbstractS2CCommand<?>> subCommands()
    {
        return subCmds;
    }

    @Override
    public final String getBaseName()
    {
        return "query";
    }

    @Override
    public String buildCommand()
    {
        return "query" + " " + getBaseName();
    }

    protected String serializeArguments()
    {
        var builder = new StringBuilder();

        for (var a : arguments)
            builder.append(a).append(" ");

        return builder.toString().trim();
    }
}

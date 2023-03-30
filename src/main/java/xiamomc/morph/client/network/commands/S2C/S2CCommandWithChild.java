package xiamomc.morph.client.network.commands.S2C;

import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

import java.util.List;

public abstract class S2CCommandWithChild<T> extends AbstractS2CCommand<T>
{
    @NotNull
    protected abstract List<AbstractS2CCommand<?>> subCommands();

    @Override
    public final void onCommand(String rawArguments)
    {
        var arguments = rawArguments.split(" ", 2);
        var subBaseName = arguments.length > 1 ? arguments[0] : "";
        var subCmd = subCommands().stream()
                .filter(c -> c.getBaseName().equals(subBaseName))
                .findFirst().orElse(null);

        if (subCmd != null)
        {
            subCmd.onCommand(arguments.length == 2 ? arguments[1] : "");
            return;
        }

        onCommandUnknown(rawArguments);
    }

    protected void onCommandUnknown(String rawArguments)
    {
        MorphClient.LOGGER.warn("Unknown client command: " + rawArguments);
    }
}

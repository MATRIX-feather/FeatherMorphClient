package xiamomc.morph.client.network.commands.S2C;

import org.jetbrains.annotations.NotNull;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.ClientSkillHandler;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.network.commands.S2C.set.*;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;

import java.util.List;

public final class S2CSetCommand extends S2CCommandWithChild<String>
{
    public S2CSetCommand(ClientMorphManager morphManager, ClientSkillHandler skillHandler)
    {
        this.morphManager = morphManager;
        var client = MorphClient.getInstance();

        this.subCmds = List.of(
                new S2CSetAggressiveCommand(morphManager, client.disguiseSyncer),
                new S2CSetCooldownCommand(morphManager, skillHandler),
                new S2CSetEquipCommand(morphManager),
                new S2CSetFakeEquipCommand(morphManager),
                new S2CSetNbtCommand(morphManager),
                new S2CSetProfileCommand(morphManager, client, client.disguiseSyncer),
                new S2CSetSelfViewCommand(morphManager),
                new S2CSetSneakingCommand(morphManager),
                new S2CSetToggleSelfCommand(morphManager)
        );
    }

    private final ClientMorphManager morphManager;

    @Override
    public final String getBaseName()
    {
        return "set";
    }

    @Override
    public String buildCommand()
    {
        return "set " + getBaseName();
    }

    private final List<AbstractS2CCommand<?>> subCmds;

    @Override
    protected @NotNull List<AbstractS2CCommand<?>> subCommands() {
        return subCmds;
    }
}

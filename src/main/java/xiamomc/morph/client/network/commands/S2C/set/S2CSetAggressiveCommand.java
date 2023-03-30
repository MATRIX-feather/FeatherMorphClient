package xiamomc.morph.client.network.commands.S2C.set;

import net.minecraft.entity.mob.GhastEntity;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.DisguiseSyncer;

public class S2CSetAggressiveCommand extends AbstractSetCommand<Boolean>
{
    public S2CSetAggressiveCommand(ClientMorphManager morphManager, DisguiseSyncer syncer)
    {
        super(morphManager);

        this.syncer = syncer;
    }

    private final DisguiseSyncer syncer;

    @Override
    public String getBaseName()
    {
        return "aggressive";
    }

    @Override
    public void onCommand(String arguments)
    {
        var aggressive = Boolean.parseBoolean(arguments);

        if (syncer.entity instanceof GhastEntity ghastEntity)
            ghastEntity.setShooting(aggressive);
    }
}

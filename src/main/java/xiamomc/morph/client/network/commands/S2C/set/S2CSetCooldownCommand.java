package xiamomc.morph.client.network.commands.S2C.set;

import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.ClientSkillHandler;

public class S2CSetCooldownCommand extends AbstractSetCommand<Integer>
{
    public S2CSetCooldownCommand(ClientMorphManager morphManager, ClientSkillHandler skillHandler)
    {
        super(morphManager);

        this.skillHandler = skillHandler;
    }

    @Override
    public String getBaseName()
    {
        return "cd";
    }

    private final ClientSkillHandler skillHandler;

    @Override
    public void onCommand(String arguments)
    {
        long val = -1;

        try
        {
            val = Long.parseLong(arguments);
        }
        catch (Throwable ignored)
        {
        }

        skillHandler.setSkillCooldown(val);
    }
}

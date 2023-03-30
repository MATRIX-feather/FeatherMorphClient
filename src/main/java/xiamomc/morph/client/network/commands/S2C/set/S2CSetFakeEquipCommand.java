package xiamomc.morph.client.network.commands.S2C.set;

import xiamomc.morph.client.ClientMorphManager;

public class S2CSetFakeEquipCommand extends AbstractSetCommand<Boolean>
{
    public S2CSetFakeEquipCommand(ClientMorphManager morphManager)
    {
        super(morphManager);
    }

    @Override
    public String getBaseName()
    {
        return "fake_equip";
    }

    @Override
    public void onCommand(String rawArguments)
    {
        var value = Boolean.valueOf(rawArguments);

        morphManager.equipOverriden.set(value);
    }
}

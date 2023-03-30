package xiamomc.morph.client.network.commands.S2C.set;

import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.DisguiseSyncer;
import xiamomc.pluginbase.AbstractSchedulablePlugin;

public class S2CSetProfileCommand extends AbstractSetCommand<String>
{
    public S2CSetProfileCommand(ClientMorphManager morphManager, AbstractSchedulablePlugin client, DisguiseSyncer syncer)
    {
        super(morphManager);

        this.client = client;
        this.syncer = syncer;
    }

    private final AbstractSchedulablePlugin client;
    private final DisguiseSyncer syncer;

    @Override
    public String getBaseName()
    {
        return "profile";
    }

    @Override
    public String buildCommand()
    {
        return super.buildCommand() + " " + this.getArgumentAt(0, "{}");
    }

    @Override
    public void onCommand(String arguments)
    {
        try
        {
            var nbt = StringNbtReader.parse(arguments);
            var profile = NbtHelper.toGameProfile(nbt);

            if (profile != null)
                this.client.schedule(() -> syncer.updateSkin(profile));
        }
        catch (Exception e)
        {
            //todo
        }
    }
}

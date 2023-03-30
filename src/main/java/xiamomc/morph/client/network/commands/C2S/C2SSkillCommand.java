package xiamomc.morph.client.network.commands.C2S;

import net.minecraft.entity.player.PlayerEntity;
import xiamomc.morph.network.commands.C2S.AbstractC2SCommand;

public class C2SSkillCommand extends AbstractC2SCommand<PlayerEntity, String>
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

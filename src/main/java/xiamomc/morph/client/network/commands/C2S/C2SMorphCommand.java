package xiamomc.morph.client.network.commands.C2S;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.commands.C2S.AbstractC2SCommand;

public class C2SMorphCommand extends AbstractC2SCommand<PlayerEntity, String>
{
    public C2SMorphCommand(@Nullable String argument)
    {
        super(argument);
    }

    public C2SMorphCommand(@Nullable String... arguments)
    {
        super(arguments);
    }

    @Override
    public String getBaseName()
    {
        return "morph";
    }

    @Override
    public String buildCommand()
    {
        return super.buildCommand() + " " + getArgumentAt(this.arguments, 0, "");
    }
}

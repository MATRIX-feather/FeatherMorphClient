package xiamomc.morph.client.network.commands.C2S;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.network.commands.C2S.AbstractC2SCommand;

public class C2SOptionCommand extends AbstractC2SCommand<PlayerEntity, C2SOptionCommand.ClientOptions>
{
    public C2SOptionCommand(@NotNull ClientOptions option)
    {
        super(option);
    }

    private Object value;

    public C2SOptionCommand setValue(Object value)
    {
        this.value = value;

        return this;
    }

    @Override
    public String getBaseName()
    {
        return "option";
    }

    @Override
    public String buildCommand()
    {
        return super.buildCommand() + " " + getArgumentAt(this.arguments, 0).networkName + " " + value;
    }

    public enum ClientOptions
    {
        CLIENTVIEW("clientview"),
        HUD("hud");

        private ClientOptions(String optionNetworkName)
        {
            this.networkName = optionNetworkName;
        }

        public final String networkName;
    }
}

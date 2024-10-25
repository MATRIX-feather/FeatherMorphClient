package xyz.nifeather.morph.testserver;

import net.minecraft.entity.player.PlayerEntity;

public class DisguiseSession
{
    private final PlayerEntity bindingPlayer;

    public PlayerEntity player()
    {
        return bindingPlayer;
    }

    private final String disguiseIdentifier;

    public String disguiseIdentifier()
    {
        return disguiseIdentifier;
    }

    public DisguiseSession(PlayerEntity bindingPlayer, String disguiseIdentifier)
    {
        this.disguiseIdentifier = disguiseIdentifier;
        this.bindingPlayer = bindingPlayer;
    }
}

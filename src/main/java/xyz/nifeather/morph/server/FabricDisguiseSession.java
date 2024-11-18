package xyz.nifeather.morph.server;

import net.minecraft.entity.player.PlayerEntity;

public class FabricDisguiseSession
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

    public FabricDisguiseSession(PlayerEntity bindingPlayer, String disguiseIdentifier)
    {
        this.disguiseIdentifier = disguiseIdentifier;
        this.bindingPlayer = bindingPlayer;
    }
}

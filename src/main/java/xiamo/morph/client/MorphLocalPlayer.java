package xiamo.morph.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class MorphLocalPlayer extends OtherClientPlayerEntity
{
    private final Pair<Integer, GameProfile> currentProfilePair = new Pair<>(0, null);

    private final String playerName;

    private Identifier morphTextureIdentifier;
    private Identifier capeTextureIdentifier;

    private String model;

    public MorphLocalPlayer(ClientWorld clientWorld, GameProfile profile, @Nullable PlayerPublicKey playerPublicKey)
    {
        super(clientWorld, profile, playerPublicKey);

        LoggerFactory.getLogger("morph").info("Fetching skin for " + profile);

        this.playerName = profile.getName();

        currentProfilePair.setLeft(0);
        currentProfilePair.setRight(profile);

        this.updateSkin(profile);
    }

    private int requestId = 0;

    public void updateSkin(GameProfile profile)
    {
        if (!profile.getName().equals(playerName))
        {
            LoggerFactory.getLogger("morph").info("Profile player name not match : " + profile.getName() + " <-> " + playerName);
            return;
        }

        requestId++;

        var invokeId = requestId;

        SkullBlockEntity.loadProperties(profile, o ->
        {
            MinecraftClient.getInstance().getSkinProvider().loadSkin(o, (type, id, texture) ->
            {
                var currentId = currentProfilePair.getLeft();

                if (invokeId < currentId)
                {
                    LoggerFactory.getLogger("morph").warn("Not setting: A newer request has been finished! " + invokeId + " <-> " + currentProfilePair.getLeft());
                    return;
                }

                if (type == MinecraftProfileTexture.Type.SKIN)
                {
                    LoggerFactory.getLogger("morph").info("Loading skin for " + playerName + " :: " + id.toString());

                    currentProfilePair.setLeft(requestId);
                    currentProfilePair.setRight(o);
                    this.morphTextureIdentifier = id;
                    this.model = texture.getMetadata("model");

                    AbstractClientPlayerEntity.loadSkin(id, o.getName());
                }
                else if (type == MinecraftProfileTexture.Type.CAPE)
                {
                    this.capeTextureIdentifier = id;
                }
            }, true);
        });
    }

    public boolean fallFlying;

    @Override
    public boolean isFallFlying() {
        return fallFlying;
    }

    public void setActiveItem(ItemStack stack)
    {
        this.activeItemStack = stack;
    }

    @Override
    public boolean isUsingItem() {
        return itemUseTime > 0;
    }

    public int itemUseTime;

    @Override
    public int getItemUseTime()
    {
        return itemUseTime;
    }

    public int itemUseTimeLeft;

    @Override
    public int getItemUseTimeLeft() {
        return itemUseTimeLeft;
    }

    public boolean usingRiptide;

    @Override
    public boolean isUsingRiptide()
    {
        return usingRiptide;
    }

    @Override
    public Vec3d getPos()
    {
        var clientPlayer = MinecraftClient.getInstance().player;

        if (clientPlayer != null)
            return clientPlayer.getPos();

        return super.getPos();
    }

    @Override
    protected void initDataTracker()
    {
        super.initDataTracker();

        this.dataTracker.set(PLAYER_MODEL_PARTS, (byte)127);
    }

    @Override
    public boolean isPartVisible(PlayerModelPart modelPart) {
        return true;
    }

    @Override
    public Identifier getSkinTexture()
    {
        if (morphTextureIdentifier != null) return morphTextureIdentifier;

        return super.getSkinTexture();
    }

    @Override
    public boolean canRenderCapeTexture() {
        return capeTextureIdentifier != null;
    }

    @Nullable
    @Override
    public Identifier getCapeTexture()
    {
        return capeTextureIdentifier;
    }

    @Override
    public boolean hasSkinTexture() {
        return morphTextureIdentifier != null;
    }

    @Override
    public String getModel()
    {
        if (model != null) return model;

        return super.getModel();
    }

    @Override
    public boolean shouldRenderName() {
        return false;
    }
}

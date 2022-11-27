package xiamo.morph.client.graphics;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.entity.EntityChangeListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class MorphLocalPlayer extends OtherClientPlayerEntity
{
    private GameProfile morphProfile;

    private final String playerName;

    private Identifier morphTextureIdentifier;
    private Identifier capeTextureIdentifier;

    private String model;

    public MorphLocalPlayer(ClientWorld clientWorld, GameProfile profile, @Nullable PlayerPublicKey playerPublicKey)
    {
        super(clientWorld, profile, playerPublicKey);

        LoggerFactory.getLogger("morph").info("Fetching skin for " + profile);

        this.playerName = profile.getName();

        this.updateSkin(profile, false);
    }

    public void updateSkin(GameProfile profile, boolean force)
    {
        if (!profile.getName().equals(playerName))
        {
            LoggerFactory.getLogger("morph").info("Profile player name not match : " + profile.getName() + " <-> " + playerName);
            return;
        }

        if (force) morphProfile = null;

        //记录调用时的Profile状态
        var currentProfile = morphProfile;

        SkullBlockEntity.loadProperties(profile, o ->
        {
            MinecraftClient.getInstance().getSkinProvider().loadSkin(o, (type, id, texture) ->
            {
                //如果当前profile和调用时的不一样，拒绝设置
                if (currentProfile != morphProfile)
                {
                    LoggerFactory.getLogger("morph").info("GameProfile mismatch! : " + currentProfile + " <-> " + morphProfile);
                    return;
                }

                if (type == MinecraftProfileTexture.Type.SKIN)
                {
                    LoggerFactory.getLogger("morph").info("Loading skin for " + playerName + " :: " + id.toString());

                    this.morphProfile = o;
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

    public void setFallFlying(boolean val)
    {
        fallFlying = val;
    }

    private boolean fallFlying;

    @Override
    public boolean isFallFlying() {
        return fallFlying;
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

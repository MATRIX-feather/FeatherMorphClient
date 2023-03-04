package xiamomc.morph.client.entities;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.graphics.capes.ICapeProvider;
import xiamomc.morph.client.graphics.capes.providers.KappaCapeProvider;

public class MorphLocalPlayer extends OtherClientPlayerEntity
{
    private final Pair<Integer, GameProfile> currentProfilePair = new Pair<>(0, null);

    private final String playerName;

    private Identifier morphTextureIdentifier;
    private Identifier capeTextureIdentifier;

    private String model;

    public MorphLocalPlayer(ClientWorld clientWorld, GameProfile profile)
    {
        super(clientWorld, profile);

        logger.info("Fetching skin for " + profile);

        this.playerName = profile.getName();

        currentProfilePair.setLeft(0);
        currentProfilePair.setRight(profile);

        this.updateSkin(profile);
    }

    private int requestId = 0;

    private ICapeProvider capeProvider = new KappaCapeProvider();

    private static final Logger logger = LoggerFactory.getLogger("MorphClient");

    public void updateSkin(GameProfile profile)
    {
        if (!profile.getName().equals(playerName))
        {
            logger.info("Profile player name not match : " + profile.getName() + " <-> " + playerName);
            return;
        }

        requestId++;

        var invokeId = requestId;

        SkullBlockEntity.loadProperties(profile, o ->
        {
            MinecraftClient.getInstance().getSkinProvider().loadSkin(o, (type, id, texture) ->
            {
                if (this.isRemoved()) return;

                var currentId = currentProfilePair.getLeft();

                if (invokeId < currentId)
                {
                    logger.warn("Not setting skin for " + this + ": A newer request has been finished! " + invokeId + " <-> " + currentProfilePair.getLeft());
                    return;
                }

                if (type == MinecraftProfileTexture.Type.SKIN)
                {
                    logger.info("Loading skin for " + playerName + " :: " + id.toString());

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

        //为披风提供器单独创建新的GameProfile以避免影响皮肤功能
        capeProvider.getCape(new GameProfile(profile.getId(), profile.getName()), a ->
        {
            logger.info("Received custom cape texture from a cape provider!");

            if (this.capeTextureIdentifier == null)
                this.capeTextureIdentifier = a;
            else
                logger.info("But capeTextureIdentifier is not null (" + capeTextureIdentifier + "), not setting...");
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

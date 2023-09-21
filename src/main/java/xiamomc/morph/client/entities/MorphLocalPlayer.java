package xiamomc.morph.client.entities;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.graphics.capes.ICapeProvider;
import xiamomc.morph.client.graphics.capes.providers.KappaCapeProvider;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MorphLocalPlayer extends OtherClientPlayerEntity
{
    private final Pair<Integer, GameProfile> currentProfilePair = new Pair<>(0, null);

    private final String playerName;

    private Identifier morphTextureIdentifier;
    private Identifier capeTextureIdentifier;

    private SkinTextures.Model model;

    public boolean personEquals(MorphLocalPlayer other)
    {
        return other.playerName.equals(this.playerName);
    }

    public void copyFrom(MorphLocalPlayer prevPlayer)
    {
        requestId++;

        this.capeTextureIdentifier = prevPlayer.capeTextureIdentifier;
        this.morphTextureIdentifier = prevPlayer.morphTextureIdentifier;

        this.model = prevPlayer.model;

        this.currentProfilePair.setLeft(requestId);
        this.currentProfilePair.setRight(prevPlayer.currentProfilePair.getRight());
    }

    public MorphLocalPlayer(ClientWorld clientWorld, GameProfile profile)
    {
        super(clientWorld, profile);

        this.playerName = profile.getName();

        currentProfilePair.setLeft(0);
        currentProfilePair.setRight(profile);

        this.updateSkin(profile);
    }

    @Override
    public void remove(RemovalReason reason)
    {
        super.remove(reason);
        this.setWorld(null);
    }

    private int requestId = 0;

    private final static ICapeProvider capeProvider = new KappaCapeProvider();

    private static final Logger logger = LoggerFactory.getLogger("MorphClient");

    private static boolean hasTextures(GameProfile profile) {
        return profile.getProperties().containsKey("textures");
    }

    @Nullable
    private static MinecraftSessionService sessionService;

    private static CompletableFuture<Optional<GameProfile>> fetchProfileWithTextures(GameProfile profile) {
        if (hasTextures(profile)) {
            return CompletableFuture.completedFuture(Optional.of(profile));
        }

        sessionService = MinecraftClient.getInstance().getSessionService();

        return CompletableFuture.supplyAsync(() -> {
            MinecraftSessionService minecraftSessionService = sessionService;
            if (minecraftSessionService != null) {
                ProfileResult profileResult = minecraftSessionService.fetchProfile(profile.getId(), true);
                return profileResult == null ? Optional.of(profile) : Optional.of(profileResult.profile());
            }
            return Optional.empty();
        }, Util.getMainWorkerExecutor());
    }

    public void updateSkin(GameProfile profile)
    {
        logger.debug("Fetching skin for " + profile);

        if (!profile.getName().equals(playerName))
        {
            logger.debug("Profile player name not match : " + profile.getName() + " <-> " + playerName);
            return;
        }

        requestId++;

        var invokeId = requestId;

        logger.info("Fetching skin...");

        var skinProvider = MinecraftClient.getInstance().getSkinProvider();
        var fetchTask = skinProvider.fetchSkinTextures(profile);
        fetchTask.thenApply(a ->
        {
            logger.info("Fetching skin complete!");

            onFetchComplete(invokeId, a, profile);
            return null;
        });
    }

    private void onFetchComplete(int invokeId, SkinTextures tex, GameProfile profile)
    {
        if (this.isRemoved()) return;

        logger.info("Get tex:" + tex.texture() + " :: " + tex.capeTexture() + " :: " + tex.model() + " :: " + tex.textureUrl());

        var currentId = currentProfilePair.getLeft();

        if (invokeId < currentId)
        {
            logger.debug("Not setting skin for " + this + ": A newer request has been finished! " + invokeId + " <-> " + currentProfilePair.getLeft());
            return;
        }

        this.capeTextureIdentifier = tex.capeTexture();
        currentProfilePair.setLeft(requestId);
        currentProfilePair.setRight(profile);

        this.skinTexUrl = tex.textureUrl();

        this.morphTextureIdentifier = tex.texture();
        this.model = tex.model();

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

    private String skinTexUrl;

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
    public SkinTextures getSkinTextures()
    {
        var tex = new SkinTextures(morphTextureIdentifier,
                skinTexUrl,
                capeTextureIdentifier, capeTextureIdentifier,
                model, true);

        return tex;
    }

    @Override
    public boolean shouldRenderName() {
        return false;
    }
}

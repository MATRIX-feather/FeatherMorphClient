package xyz.nifeather.morph.client.entities;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xyz.nifeather.morph.client.MorphClient;
import xyz.nifeather.morph.client.graphics.capes.ICapeProvider;
import xyz.nifeather.morph.client.graphics.capes.providers.KappaCapeProvider;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class MorphLocalPlayer extends OtherClientPlayerEntity
{
    private final Pair<Integer, GameProfile> currentProfilePair = new Pair<>(0, null);

    private final String playerName;

    @NotNull
    private Identifier morphTextureIdentifier = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");

    @Nullable
    private Identifier capeTextureIdentifier;

    @Nullable
    private Identifier ofCapeIdentifier;

    @Nullable
    private String skinTextureUrl;

    @NotNull
    private SkinTextures.Model model = SkinTextures.Model.SLIM;

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

        this.skinTextureUrl = prevPlayer.skinTextureUrl;
    }

    private PlayerEntity bindingPlayer;

    public void setBindingPlayer(PlayerEntity newInstance)
    {
        bindingPlayer = newInstance;
    }

    public MorphLocalPlayer(ClientWorld clientWorld, GameProfile profile, PlayerEntity bindingPlayer)
    {
        super(clientWorld, profile);

        if (bindingPlayer == null) bindingPlayer = MinecraftClient.getInstance().player;
        this.bindingPlayer = bindingPlayer;

        this.playerName = profile.getName();

        currentProfilePair.setLeft(0);
        currentProfilePair.setRight(profile);

        this.dataTracker.set(PLAYER_MODEL_PARTS, (byte)127);
        this.updateSkin(profile, true);
    }

    private int requestId = 0;

    private final static ICapeProvider capeProvider = new KappaCapeProvider();

    private static final Logger logger = MorphClient.LOGGER;

    private static ApiServices apiServices;
    private static Executor apiExecutor;

    public static void setMinecraftAPIServices(ApiServices apiSrv, Executor apiExec)
    {
        apiServices = apiSrv;
        apiExecutor = apiExec;
    }

    //region From SkullBlockEntity

    public static CompletableFuture<Optional<GameProfile>> fetchProfileWithTextures(GameProfile profile)
    {
        return profile.getProperties().containsKey("textures")
                ? CompletableFuture.completedFuture(Optional.of(profile))
                : CompletableFuture.supplyAsync(() ->
                    {
                        var sessionService = MinecraftClient.getInstance().getSessionService();

                        if (sessionService != null)
                        {
                            ProfileResult profileResult = sessionService.fetchProfile(profile.getId(), true);
                            return profileResult == null ? Optional.of(profile) : Optional.of(profileResult.profile());
                        } else
                        {
                            return Optional.empty();
                        }
                    }, Util.getMainWorkerExecutor());
    }

    private static UserCache userCache;

    private static CompletableFuture<Optional<GameProfile>> fetchProfile(String name)
    {
        if (userCache == null && apiServices != null)
            userCache = apiServices.userCache();

        UserCache userCache = MorphLocalPlayer.userCache;
        return userCache == null
                ? CompletableFuture.completedFuture(Optional.empty())
                : userCache.findByNameAsync(name).thenCompose((optional) ->
                {
                    return optional.isPresent() ? fetchProfileWithTextures(optional.get()) : CompletableFuture.completedFuture(Optional.empty());
                }).thenApplyAsync((profile) ->
                {
                    return profile;
                }, apiExecutor);
    }

    //endregion From SkullBlockEntity

    private final AtomicBoolean initialFetchFired = new AtomicBoolean(false);

    public void updateSkin(GameProfile profile)
    {
        updateSkin(profile, false);
    }

    private void updateSkin(GameProfile profile, boolean isInitial)
    {
        if (!RenderSystem.isOnRenderThread())
        {
            MorphClient.getInstance().schedule(() -> updateSkin(profile));
            return;
        }

        if (isInitial && initialFetchFired.get())
            return;

        initialFetchFired.set(true);

        //logger.info("Fetching skin for " + profile);

        if (!profile.getName().equals(playerName))
        {
            //logger.info("Profile %s player name not match : '%s' <-> '%s'".formatted(profile.getId(), profile.getName(), playerName));
            return;
        }

        requestId++;

        var invokeId = requestId;

        // 根据传入的profile来决定要不要由我们自己获取皮肤
        CompletableFuture<Optional<GameProfile>> profileFetchTask;

        // 如果传入的是NIL_UUID，则自己获取，否则就用传入的profile
        if (profile.getId().equals(Util.NIL_UUID))
            profileFetchTask = fetchProfile(profile.getName());
        else
            profileFetchTask = CompletableFuture.completedFuture(Optional.of(profile));

        profileFetchTask.thenApply(optional ->
        {
            // 确保targetProfile不是null
            GameProfile targetProfile = optional.orElse(profile);

            //logger.info("Target UUID is " + targetProfile.getId() + " :: Optional is " + optional.orElse(null));

            // 开始获取皮肤信息
            startFetchTask(targetProfile, invokeId);
            return null;
        });
    }

    private void startFetchTask(GameProfile profile, int invokeId)
    {
        // 通过fetchProfileWithTextures获取带皮肤的gameProfile
        var texturedProfileFetchTask = fetchProfileWithTextures(profile);
        texturedProfileFetchTask.thenApply((optional ->
        {
            GameProfile gameProfile = optional.orElse(null);

            // 如果没有，则不做任何举动
            if (gameProfile == null)
                return null;

            // 反之，获取其中的皮肤
            var skinProvider = MinecraftClient.getInstance().getSkinProvider();
            var skinFetchTask = skinProvider.fetchSkinTextures(profile);
            skinFetchTask.thenApply(a ->
            {
                onFetchComplete(invokeId, a, profile);
                return null;
            });

            return null;
        }));
    }

    private void onFetchComplete(int invokeId, SkinTextures tex, GameProfile profile)
    {
        if (this.isRemoved()) return;

        var currentId = currentProfilePair.getLeft();

        if (invokeId < currentId)
        {
            //logger.info("Not setting skin for " + this + ": A newer request has been finished! " + invokeId + " <-> " + currentProfilePair.getLeft());
            return;
        }

        this.capeTextureIdentifier = tex.capeTexture();
        currentProfilePair.setLeft(requestId);
        currentProfilePair.setRight(profile);

        this.skinTextureUrl = tex.textureUrl();

        this.morphTextureIdentifier = tex.texture();
        this.model = tex.model();

        updateSkinTextures();

        //为披风提供器单独创建新的GameProfile以避免影响皮肤功能
        capeProvider.getCape(new GameProfile(profile.getId(), profile.getName()), identifier ->
        {
            this.ofCapeIdentifier = identifier;
            updateSkinTextures();
        });
    }

    private void updateSkinTextures()
    {
        var cape = ofCapeIdentifier == null ? capeTextureIdentifier : ofCapeIdentifier;

        this.skinTextures = new SkinTextures(morphTextureIdentifier,
                skinTextureUrl,
                cape, cape,
                model, false);
    }

    public boolean fallFlying;

    @Override
    public boolean isSpectator()
    {
        return bindingPlayer != null && bindingPlayer.isSpectator();
    }

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
        var clientPlayer = bindingPlayer;

        if (clientPlayer != null)
            return clientPlayer.getPos();

        return super.getPos();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder)
    {
        super.initDataTracker(builder);
    }

    @Override
    public boolean isPartVisible(PlayerModelPart modelPart)
    {
        return bindingPlayer.isPartVisible(modelPart);
    }

    @Nullable
    private SkinTextures skinTextures;

    @Override
    public SkinTextures getSkinTextures()
    {
        if (skinTextures != null) return skinTextures;

        return new SkinTextures(morphTextureIdentifier,
                skinTextureUrl,
                capeTextureIdentifier, capeTextureIdentifier,
                model, true);
    }

    @Override
    public boolean shouldRenderName() {
        return false;
    }

    @Override
    public double squaredDistanceTo(Vec3d vector)
    {
        if (vector.equals(MinecraftClient.getInstance().gameRenderer.getCamera().getPos()))
            return 0d;

        return super.squaredDistanceTo(vector);
    }

    @Nullable
    private EntityPose overridePose;

    public void setOverridePose(@Nullable EntityPose newPose)
    {
        this.overridePose = newPose;
    }

    @Override
    public EntityPose getPose()
    {
        if (overridePose != null) return overridePose;

        return super.getPose();
    }
}

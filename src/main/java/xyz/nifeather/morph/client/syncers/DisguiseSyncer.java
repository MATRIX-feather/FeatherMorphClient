package xyz.nifeather.morph.client.syncers;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.client.*;
import xyz.nifeather.morph.client.entities.MorphLocalPlayer;
import xyz.nifeather.morph.client.mixin.accessors.AbstractHorseEntityMixin;
import xyz.nifeather.morph.client.mixin.accessors.EntityAccessor;
import xyz.nifeather.morph.client.mixin.accessors.LimbAnimatorAccessor;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;
import xiamomc.pluginbase.Annotations.Resolved;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DisguiseSyncer extends MorphClientObject
{
    @Nullable
    protected LivingEntity disguiseInstance;

    @Nullable
    public LivingEntity getDisguiseInstance()
    {
        return disguiseInstance;
    }

    @NotNull
    protected AbstractClientPlayerEntity bindingPlayer;

    @NotNull
    private ConvertedMeta bindingMeta = new ConvertedMeta();

    @NotNull
    protected ConvertedMeta getBindingMeta()
    {
        return bindingMeta;
    }

    @Nullable
    protected NbtCompound getCompound()
    {
        return bindingMeta.nbt;
    }

    protected final String disguiseId;

    protected final int bindingNetworkId;

    @Resolved(shouldSolveImmediately = true)
    private DisguiseInstanceTracker instanceTracker;

    @NotNull
    protected abstract EntityCache getEntityCache();

    public AbstractClientPlayerEntity getBindingPlayer()
    {
        return bindingPlayer;
    }

    public DisguiseSyncer(AbstractClientPlayerEntity bindingPlayer, String morphId, int networkId)
    {
        this.bindingPlayer = bindingPlayer;
        this.disguiseId = morphId;
        this.bindingNetworkId = networkId;

        bindingMeta.outdated = true;

        refreshEntity();
    }

    private AnimationHandler animHandler;

    public void setAnimationHandler(AnimationHandler handler)
    {
        this.animHandler = handler;
    }

    public void playAnimation(String animation)
    {
        if (animHandler == null)
        {
            logger.warn("No animation handler for disguise '%s'!".formatted(disguiseId));
            return;
        }

        animHandler.play(disguiseInstance, animation);
    }

    private int crystalId;

    protected Entity beamTarget;

    private void scheduleCrystalSearch()
    {
        if (beamTarget != null || crystalId == 0) return;

        this.addSchedule(this::scheduleCrystalSearch, 10);

        this.beamTarget = findCrystalBy(crystalId);
    }

    @Nullable
    private Entity findCrystalBy(int id)
    {
        if (disguiseInstance == null || id == 0) return null;

        var world = MinecraftClient.getInstance().player.getWorld();
        if (world == null) return null;

        return world.getEntityById(id);
    }

    public void refreshEntity()
    {
        try (var clientWorld = MinecraftClient.getInstance().world)
        {
            if (clientWorld == null)
            {
                disguiseInstance = null;
                return;
            }

            var entityCache = getEntityCache();

            var prevEntity = disguiseInstance;
            var client = MorphClient.getInstance();

            if (prevEntity != null)
            {
                prevEntity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                prevEntity.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

                prevEntity.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
                prevEntity.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
                prevEntity.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
                prevEntity.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);

                beamTarget = null;

                prevEntity.hurtTime = 0;

                entityCache.discardEntity(disguiseId);
            }

            disguiseInstance = entityCache.getEntity(disguiseId, bindingPlayer);

            if (disguiseInstance != null)
            {
                var entityToAdd = disguiseInstance;
                entityToAdd.setId(entityToAdd.getId() - entityToAdd.getId() * 2);

                client.schedule(() -> clientWorld.addEntity(entityToAdd));

                var nbt = getCompound();
                if (nbt != null)
                    client.schedule(() -> mergeNbt(nbt));

                disguiseInstance.addCommandTag("BINDING_" + bindingPlayer.getId());
                disguiseInstance.noClip = true;

                if (disguiseInstance instanceof MorphLocalPlayer localPlayer && prevEntity instanceof MorphLocalPlayer prevPlayer && prevPlayer.personEquals(localPlayer))
                {
                    localPlayer.copyFrom(prevPlayer);
                    localPlayer.setBindingPlayer(MinecraftClient.getInstance().player);
                }

                initialSync();
                baseSync();
            }
        }
        catch (Throwable t)
        {
            MorphClient.LOGGER.error("Error occurred while refreshing client view: %s".formatted(t.getMessage()));
            t.printStackTrace();

            disguiseInstance = null;
        }
    }

    public void playAttackAnimation()
    {
        if (disguiseInstance != null)
            disguiseInstance.handleStatus(EntityStatuses.PLAY_ATTACK_SOUND);
    }

    private static final ItemStack air = new ItemStack(Items.AIR);

    protected void mergeNbt(NbtCompound nbtCompound)
    {
        var entity = disguiseInstance;

        if (entity == null) return;

        entity.readCustomDataFromNbt(nbtCompound);

        if (entity instanceof HorseEntity horse)
        {
            var haveSaddle = nbtCompound.contains("SaddleItem", 10);

            if (haveSaddle)
            {
                ItemStack itemStack = ItemStack.fromNbt(bindingPlayer.getWorld().getRegistryManager(), nbtCompound.getCompound("SaddleItem"))
                        .orElse(air);

                var isSaddle = itemStack.isOf(Items.SADDLE);

                ((AbstractHorseEntityMixin) horse).callSetHorseFlag(4, isSaddle);
            }

            //Doesn't work for unknown reason
            if (nbtCompound.contains("ArmorItem", NbtElement.COMPOUND_TYPE))
            {
                ItemStack armorItem = ItemStack.fromNbt(bindingPlayer.getWorld().getRegistryManager(), nbtCompound.getCompound("ArmorItem"))
                        .orElse(air);

                horse.equipBodyArmor(armorItem);
            }
        }

        bindingPlayer.calculateDimensions();

        var crystalPosition = nbtCompound.getInt("BeamTarget");
        crystalId = crystalPosition;
        this.beamTarget = findCrystalBy(crystalPosition);

        if (beamTarget == null)
            this.scheduleCrystalSearch();
    }

    //region DisguiseSyncing

    private final AtomicBoolean isSyncing = new AtomicBoolean(false);

    protected void markSyncing()
    {
        isSyncing.set(true);
    }

    protected void markNotSyncing()
    {
        isSyncing.set(false);
    }

    public boolean isSyncing()
    {
        return isSyncing.get();
    }

    private boolean allowTick = true;

    protected void onTickError()
    {
    }

    private void onSyncError(Exception e)
    {
        allowTick = false;
        markNotSyncing();

        logger.error(e.getMessage());
        e.printStackTrace();

        if (disguiseInstance != null)
        {
            try
            {
                disguiseInstance.remove(Entity.RemovalReason.DISCARDED);
            }
            catch (Exception ee)
            {
                LoggerFactory.getLogger("MorphClient").error("无法移除实体：" + ee.getMessage());
                ee.printStackTrace();
            }

            disguiseInstance = null;
        }

        onTickError();

        var clientPlayer = MinecraftClient.getInstance().player;
        assert clientPlayer != null;

        clientPlayer.sendMessage(Text.literal(this + "Sync Failed!"));
    }

    public void onGameTick()
    {
        if (!allowTick) return;

        try
        {
            var clientPlayer = MinecraftClient.getInstance().player;
            assert clientPlayer != null;

            syncTick();
        }
        catch (Exception e)
        {
            onSyncError(e);
        }
    }

    public void onGameRender()
    {
        if (!allowTick) return;

        try
        {
            syncDraw();
        }
        catch (Exception e)
        {
            onSyncError(e);
        }
    }

    public void updateSkin(GameProfile profile)
    {
        if (disposed.get())
        {
            logger.warn("Trying to update skin for a disposed DisguiseSyncer " + this);
            Thread.dumpStack();
            return;
        }

        if (!RenderSystem.isOnRenderThread())
            throw new RuntimeException("May not invoke updateSkin() while not on the render thread.");

        if (disguiseInstance instanceof MorphLocalPlayer localPlayer)
            localPlayer.updateSkin(profile);
        else
            LoggerFactory.getLogger("MorphClient")
                    .warn(this + " Received a GameProfile while current disguise is not a player! Current instance is %s".formatted(disguiseInstance));
    }

    public abstract void syncTick();

    public abstract void syncDraw();

    protected abstract void initialSync();

    protected void syncYawPitch()
    {
        if (disguiseInstance == null) return;

        var player = bindingPlayer;

        //幻翼的pitch需要倒转
        if (disguiseInstance.getType() == EntityType.PHANTOM)
            disguiseInstance.setPitch(-player.getPitch());
        else
            disguiseInstance.setPitch(player.getPitch());

        //末影龙的Yaw和玩家是反的
        if (disguiseInstance.getType() == EntityType.ENDER_DRAGON)
            disguiseInstance.setYaw(180 + player.getYaw());
        else
            disguiseInstance.setYaw(player.getYaw());

        disguiseInstance.headYaw = player.headYaw;
        disguiseInstance.prevHeadYaw = player.prevHeadYaw;

        if (disguiseInstance.getType() == EntityType.ARMOR_STAND)
        {
            disguiseInstance.bodyYaw = player.headYaw;
            disguiseInstance.prevBodyYaw = player.prevHeadYaw;
        }
    }

    protected boolean showOverridedEquips()
    {
        return bindingMeta.showOverridedEquips;
    }

    protected void syncEquipments()
    {
        if (disguiseInstance == null) return;

        var meta = getBindingMeta();
        var showOverridedEquips = showOverridedEquips();
        var disguiseEquip = meta.convertedEquipment;

        var headStack = showOverridedEquips ? disguiseEquip.head : bindingPlayer.getEquippedStack(EquipmentSlot.HEAD);
        var chestStack = showOverridedEquips ? disguiseEquip.chest : bindingPlayer.getEquippedStack(EquipmentSlot.CHEST);
        var legStack = showOverridedEquips ? disguiseEquip.leggings : bindingPlayer.getEquippedStack(EquipmentSlot.LEGS);
        var feetStack = showOverridedEquips ? disguiseEquip.feet : bindingPlayer.getEquippedStack(EquipmentSlot.FEET);
        var handStack = showOverridedEquips ? disguiseEquip.mainHand : bindingPlayer.getEquippedStack(EquipmentSlot.MAINHAND);
        var offHandStack = showOverridedEquips ? disguiseEquip.offHand : bindingPlayer.getEquippedStack(EquipmentSlot.OFFHAND);

        //logger.info("Show disguised? " + showOverridedEquips + " :: Checkstack? " + chestStack);

        disguiseInstance.equipStack(EquipmentSlot.MAINHAND, handStack);
        disguiseInstance.equipStack(EquipmentSlot.OFFHAND, offHandStack);

        disguiseInstance.equipStack(EquipmentSlot.HEAD, headStack);
        disguiseInstance.equipStack(EquipmentSlot.CHEST, chestStack);
        disguiseInstance.equipStack(EquipmentSlot.LEGS, legStack);
        disguiseInstance.equipStack(EquipmentSlot.FEET, feetStack);
    }

    protected abstract void syncPosition();

    protected void onMetaChange()
    {
    }

    private void preMetaChange(ConvertedMeta meta)
    {
        if (meta.nbt != null)
            this.mergeNbt(meta.nbt);

        if (meta.profileNbt != null && this.disguiseInstance instanceof MorphLocalPlayer localPlayer)
            localPlayer.updateSkin(meta.profileNbt);

        meta.outdated = false;

        this.bindingMeta = meta;
    }

    private ClientWorld world;
    private ClientWorld prevWorld;
    protected void baseSync()
    {
        var entity = disguiseInstance;
        if (entity == null) return;

        if (this.disposed())
        {
            logger.warn("Trying to update a disposed DisguiseSyncer(%s)!".formatted(this));
            Thread.dumpStack();
            return;
        }

        if (bindingPlayer.isRemoved() || bindingPlayer.getWorld() != MinecraftClient.getInstance().world)
        {
            logger.info(this + " Player removed, scheduling dispose");
            this.addSchedule(this::dispose);
            return;
        }

        world = MinecraftClient.getInstance().world;

        if (world != prevWorld)
        {
            var prev = prevWorld;
            prevWorld = world;

            if (prev != null)
            {
                logger.info(this + " World changed, refreshing");

                getEntityCache().dropAll();
                refreshEntity();
            }

            return;
        }

        if (disguiseInstance.isRemoved())
        {
            logger.info(this + " Instance removed, refreshing");
            refreshEntity();
            return;
        }

        if (bindingMeta.outdated)
            this.preMetaChange(instanceTracker.getMetaFor(this.bindingNetworkId));

        markSyncing();
        syncPosition();
        syncEquipments();

        // 因为我们在LivingEntity和PlayerEntity那里都加了阻止伪装实体被世界tick的mixin,
        // 所以在这里手动调用tick
        entity.tick();

        if (beamTarget != null && beamTarget.isRemoved())
            beamTarget = null;

        var entitylimbAnimatorAccessor = (LimbAnimatorAccessor) entity.limbAnimator;
        var playerLimbAccessor = (LimbAnimatorAccessor) bindingPlayer.limbAnimator;
        var playerLimb = bindingPlayer.limbAnimator;

        entitylimbAnimatorAccessor.setPrevSpeed(playerLimbAccessor.getPrevSpeed());
        entitylimbAnimatorAccessor.setPos(playerLimb.getPos());
        entitylimbAnimatorAccessor.setSpeed(playerLimb.getSpeed());

        // Sleep Pos
        var sleepPos = bindingPlayer.getSleepingPosition().orElse(null);

        if (sleepPos != null)
            entity.setSleepingPosition(sleepPos);
        else
            entity.clearSleepingPosition();

        entity.setOnFire(bindingPlayer.isOnFire());

        // Health
        var healthAttribute = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);

        if (healthAttribute != null)
            healthAttribute.setBaseValue(bindingPlayer.getMaxHealth());

        entity.setHealth(bindingPlayer.getHealth());

        // Scale
        var scaleAttribute = entity.getAttributeInstance(EntityAttributes.GENERIC_SCALE);

        if (scaleAttribute != null)
            scaleAttribute.setBaseValue(bindingPlayer.getAttributeValue(EntityAttributes.GENERIC_SCALE));

        // Hand Swing
        entity.handSwinging = bindingPlayer.handSwinging;
        entity.handSwingProgress = bindingPlayer.handSwingProgress;
        entity.lastHandSwingProgress = bindingPlayer.lastHandSwingProgress;
        entity.handSwingTicks = bindingPlayer.handSwingTicks;
        entity.preferredHand = bindingPlayer.preferredHand;

        // Hand and sneaking
        entity.setSneaking(bindingPlayer.isSneaking());

        // Hurt and death
        entity.hurtTime = bindingPlayer.hurtTime;
        entity.deathTime = bindingPlayer.deathTime;

        //entity.inPowderSnow = clientPlayer.inPowderSnow;
        entity.setFrozenTicks(bindingPlayer.getFrozenTicks());

        entity.setVelocity(bindingPlayer.getVelocity());

        entity.setOnGround(bindingPlayer.isOnGround());

        ((EntityAccessor) entity).setTouchingWater(bindingPlayer.isTouchingWater());

        // Glowing
        if (bindingPlayer.isGlowing() != entity.isGlowing())
            entity.setGlowing(bindingPlayer.isGlowing());

        //同步Pose
        entity.setPose(bindingPlayer.getPose());
        entity.setSwimming(bindingPlayer.isSwimming());

        entity.isUsingItem();
        entity.stopUsingItem();

        if (bindingPlayer.hasVehicle() && !bindingPlayer.getVehicle().equals(entity.getVehicle()))
        {
            entity.startRiding(bindingPlayer, true);
        }
        else if (!bindingPlayer.hasVehicle() && entity.hasVehicle())
        {
            entity.stopRiding();
        }

        entity.setStuckArrowCount(bindingPlayer.getStuckArrowCount());

        if (entity instanceof MorphLocalPlayer player)
        {
            player.fallFlying = bindingPlayer.isFallFlying();
            player.usingRiptide = bindingPlayer.isUsingRiptide();

            player.fishHook = bindingPlayer.fishHook;

            player.itemUseTimeLeft = bindingPlayer.getItemUseTimeLeft();
            player.itemUseTime = bindingPlayer.getItemUseTime();
            player.setActiveItem(bindingPlayer.getActiveItem());

            player.setMainArm(bindingPlayer.getMainArm());
        }

        entity.setInvisible(bindingPlayer.isInvisible());

        markNotSyncing();
    }

    //endregion

    //region Disposal

    private final AtomicBoolean disposed = new AtomicBoolean(false);

    public final boolean disposed()
    {
        return disposed.get();
    }

    protected abstract void onDispose();

    public final void dispose()
    {
        if (disposed())
            return;

        if (getEntityCache() != EntityCache.getGlobalCache())
            getEntityCache().dispose();

        if (disguiseInstance != null)
        {
            if (RenderSystem.isOnRenderThread())
                disguiseInstance.discard();
            else
                addSchedule(disguiseInstance::discard);
        }

        try
        {
            this.onDispose();
        }
        catch (Throwable t)
        {
            logger.warn("Error calling onDispose() for DisguiseSyncer: %s".formatted(t.getMessage()));
            t.printStackTrace();
        }

        disguiseInstance = null;
        world = null;
        prevWorld = null;
        disposed.set(true);

        bindingPlayer.calculateDimensions();

        try
        {
            postDispose();
        }
        catch (Throwable t)
        {
            logger.warn("Error calling postDispose() for DisguiseSyncer: %s".formatted(t.getMessage()));
            t.printStackTrace();
        }
    }

    protected void postDispose()
    {
    }

    //endregion Disposal
}

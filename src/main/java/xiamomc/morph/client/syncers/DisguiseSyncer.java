package xiamomc.morph.client.syncers;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
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
import xiamomc.morph.client.*;
import xiamomc.morph.client.entities.MorphLocalPlayer;
import xiamomc.morph.client.mixin.accessors.AbstractHorseEntityMixin;
import xiamomc.morph.client.mixin.accessors.EntityAccessor;
import xiamomc.morph.client.mixin.accessors.HorseEntityMixin;
import xiamomc.morph.client.mixin.accessors.LimbAnimatorAccessor;
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

    protected final AbstractClientPlayerEntity bindingPlayer;

    @NotNull
    private ConvertedMeta bindingMeta = new ConvertedMeta();

    @NotNull
    protected ConvertedMeta getBindingMeta()
    {
        return bindingMeta;
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

            var clientPlayer = MinecraftClient.getInstance().player;

            if (disguiseInstance != null)
            {
                var entityToAdd = disguiseInstance;
                entityToAdd.setId(entityToAdd.getId() - entityToAdd.getId() * 2);

                client.schedule(() -> clientWorld.addEntity(entityToAdd));

                initialSync();
                baseSync();

                var nbt = bindingMeta.nbt;
                if (nbt != null)
                    client.schedule(() -> mergeNbt(nbt));

                if (disguiseInstance instanceof MorphLocalPlayer localPlayer && prevEntity instanceof MorphLocalPlayer prevPlayer && prevPlayer.personEquals(localPlayer))
                    localPlayer.copyFrom(prevPlayer);
            }

            if (clientPlayer != null)
                clientPlayer.calculateDimensions();
        }
        catch (Throwable t)
        {
            MorphClient.LOGGER.error("Error occurred while refreshing client view: %s".formatted(t.getMessage()));
            t.printStackTrace();

            disguiseInstance = null;
        }
    }

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
                ItemStack itemStack = ItemStack.fromNbt(nbtCompound.getCompound("SaddleItem"));
                var isSaddle = itemStack.isOf(Items.SADDLE);

                ((AbstractHorseEntityMixin) horse).callSetHorseFlag(4, isSaddle);
            }

            //Doesn't work for unknown reason
            if (nbtCompound.contains("ArmorItem", NbtElement.COMPOUND_TYPE))
            {
                ItemStack armorItem = ItemStack.fromNbt(nbtCompound.getCompound("ArmorItem"));

                ((HorseEntityMixin) horse).callEquipArmor(armorItem);
            }
        }

        MinecraftClient.getInstance().player.calculateDimensions();

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
        if (disguiseInstance instanceof MorphLocalPlayer localPlayer)
            localPlayer.updateSkin(profile);
        else
            LoggerFactory.getLogger("MorphClient")
                    .warn("Received a GameProfile while current disguise is not a player! : " + profile);
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
            logger.info(this + " Player removed, disposing");
            this.dispose();
            return;
        }

        if (world != prevWorld)
        {
            logger.info(this + " World changed, refreshing");
            prevWorld = world;

            getEntityCache().dropAll();
            refreshEntity();

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

        // Health
        var attribute = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);

        if (attribute != null)
            attribute.setBaseValue(bindingPlayer.getMaxHealth());

        entity.setHealth(bindingPlayer.getHealth());

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

    public boolean disposed()
    {
        return disposed.get();
    }

    protected abstract void onDispose();

    public final void dispose()
    {
        getEntityCache().dispose();

        try
        {
            this.onDispose();
        }
        catch (Throwable t)
        {
            logger.warn("Error calling onDispose() for a DisguiseSyncer: %s".formatted(t.getMessage()));
            t.printStackTrace();
        }

        disguiseInstance = null;
        world = null;
        prevWorld = null;
        disposed.set(true);

        postDispose();
    }

    protected void postDispose()
    {
    }

    //endregion Disposal
}

package xiamomc.morph.client;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.entities.MorphLocalPlayer;
import xiamomc.morph.client.graphics.CameraHelper;
import xiamomc.morph.client.mixin.accessors.AbstractHorseEntityMixin;
import xiamomc.morph.client.mixin.accessors.EntityAccessor;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;

public class DisguiseSyncer extends MorphClientObject
{
    @Resolved
    private ClientMorphManager morphManager;

    @Initializer
    private void load(ServerHandler serverHandler)
    {
        var selfViewIdentifier = morphManager.selfViewIdentifier;
        var currentNbtCompound = morphManager.currentNbtCompound;

        selfViewIdentifier.onValueChanged(this::refreshClientViewEntity);

        currentNbtCompound.onValueChanged((o, n) ->
        {
            if (n != null) MorphClient.getInstance().schedule(() -> this.mergeNbt(entity, n));
        });

        ClientTickEvents.END_WORLD_TICK.register((w) ->
        {
            if (w != prevWorld && serverHandler.serverReady() && prevWorld != null)
            {
                var id = morphManager.selfViewIdentifier.get();

                refreshClientViewEntity(id, id);
            }

            prevWorld = w;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            prevWorld = null;
        });

        CameraHelper.isThirdPerson.onValueChanged((o, n) -> this.onThirdPersonChange(this.entity, MinecraftClient.getInstance().player));
    }

    public void updateSkin(GameProfile profile)
    {
        if (this.entity instanceof MorphLocalPlayer localPlayer)
            localPlayer.updateSkin(profile);
        else
            LoggerFactory.getLogger("MorphClient")
                    .warn("Received a GameProfile while current disguise is not a player! : " + profile);
    }

    public static Bindable<LivingEntity> currentEntity = new Bindable<>();

    private World prevWorld;

    private void refreshClientViewEntity(String prevIdentifier, String newIdentifier)
    {
        var clientWorld = MinecraftClient.getInstance().world;
        if (clientWorld == null)
        {
            entity = null;
            return;
        }

        var prevEntity = entity;
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

            EntityCache.discardEntity(prevIdentifier);
        }

        entity = EntityCache.getEntity(newIdentifier);
        currentEntity.set(entity);

        allowTick = true;

        if (entity != null)
        {
            var entityToAdd = entity;
            entityToAdd.setId(entityToAdd.getId() - entityToAdd.getId() * 2);

            client.schedule(() -> clientWorld.addEntity(entityToAdd.getId(), entityToAdd));

            var clientPlayer = MinecraftClient.getInstance().player;

            initialSync(entity, clientPlayer);
            sync(entity, clientPlayer);
            syncDraw(entity, clientPlayer);

            var nbt = morphManager.currentNbtCompound.get();
            if (nbt != null)
                client.schedule(() -> mergeNbt(entity, nbt));
        }
    }

    public LivingEntity entity;

    private boolean allowTick = true;

    public void onGameTick()
    {
        if (!allowTick) return;

        try
        {
            var clientPlayer = MinecraftClient.getInstance().player;
            assert clientPlayer != null;

            if (entity != null)
                sync(entity, clientPlayer);
        }
        catch (Exception e)
        {
            onTickError(e);
        }
    }

    public void onGameRender()
    {
        if (!allowTick) return;

        var clientPlayer = MinecraftClient.getInstance().player;

        try
        {
            syncDraw(entity, clientPlayer);
        }
        catch (Exception e)
        {
            onTickError(e);
        }
    }

    private void onTickError(Exception e)
    {
        allowTick = false;
        e.printStackTrace();

        if (entity != null)
        {
            try
            {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
            catch (Exception ee)
            {
                LoggerFactory.getLogger("MorphClient").error("无法移除实体：" + ee.getMessage());
                ee.printStackTrace();
            }

            entity = null;
        }

        var clientPlayer = MinecraftClient.getInstance().player;
        assert clientPlayer != null;

        MorphClient.getInstance().updateClientView(true, false);
        morphManager.selfViewIdentifier.set(null);

        clientPlayer.sendMessage(Text.translatable("text.morphclient.error.update_disguise1").formatted(Formatting.RED));
        clientPlayer.sendMessage(Text.translatable("text.morphclient.error.update_disguise2").formatted(Formatting.RED));
    }

    private void mergeNbt(LivingEntity entity, NbtCompound nbtCompound)
    {
        if (entity != null)
        {
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
                if (nbtCompound.contains("ArmorItem", 10))
                {
                    ItemStack armorItem = ItemStack.fromNbt(nbtCompound.getCompound("ArmorItem"))
                            .getItem().getDefaultStack().copyWithCount(1);

                    //horse.equipHorseArmor(MinecraftClient.getInstance().player, armorItem);

                    horse.equipStack(EquipmentSlot.CHEST, armorItem);
                    ((AbstractHorseEntityMixin) horse).getItems().setStack(1, armorItem);
                }
            }
        }

        var crystalPosition = nbtCompound.getInt("BeamTarget");
        crystalId = crystalPosition;
        this.beamTarget = findCrystalBy(crystalPosition);

        if (beamTarget == null)
            this.scheduleCrystalSearch();
    }

    private int crystalId;

    private Entity beamTarget;

    private void scheduleCrystalSearch()
    {
        if (beamTarget != null || crystalId == 0) return;

        this.addSchedule(this::scheduleCrystalSearch, 10);

        this.beamTarget = findCrystalBy(crystalId);
    }

    @Nullable
    private Entity findCrystalBy(int id)
    {
        if (currentEntity == null || id == 0) return null;

        var world = MinecraftClient.getInstance().player.world;
        if (world == null) return null;

        return world.getEntityById(id);
    }

    @Nullable
    public Entity getBeamTarget()
    {
        return beamTarget;
    }

    private void syncDraw(LivingEntity entity, PlayerEntity clientPlayer)
    {
        if (entity == null || clientPlayer == null) return;

        //幻翼的pitch需要倒转
        if (entity.getType() == EntityType.PHANTOM)
            entity.setPitch(-clientPlayer.getPitch());
        else
            entity.setPitch(clientPlayer.getPitch());

        entity.prevPitch = clientPlayer.prevPitch;

        entity.headYaw = clientPlayer.headYaw;
        entity.prevHeadYaw = clientPlayer.prevHeadYaw;

        if (entity.getType() == EntityType.ARMOR_STAND)
        {
            entity.bodyYaw = clientPlayer.headYaw;
            entity.prevBodyYaw = clientPlayer.prevHeadYaw;
        }
    }

    private void initialSync(LivingEntity entity, PlayerEntity clientPlayer)
    {
        if (entity == null || clientPlayer == null) return;

        //更新prevXYZ和披风
        entity.prevX = clientPlayer.prevX;
        entity.prevY = clientPlayer.prevY - 4096;
        entity.prevZ = clientPlayer.prevZ;

        if (entity instanceof MorphLocalPlayer player)
        {
            player.capeX = clientPlayer.capeX;
            player.capeY = clientPlayer.capeY;
            player.capeZ = clientPlayer.capeZ;

            player.prevCapeX = clientPlayer.prevCapeX;
            player.prevCapeY = clientPlayer.prevCapeY;
            player.prevCapeZ = clientPlayer.prevCapeZ;
        }

        //更新BodyYaw
        onThirdPersonChange(entity, clientPlayer);
    }

    private void onThirdPersonChange(LivingEntity entity, PlayerEntity clientPlayer)
    {
        if (entity == null || clientPlayer == null) return;

        if (entity.getType() == EntityType.ARMOR_STAND)
        {
            entity.bodyYaw = clientPlayer.headYaw;
            entity.prevBodyYaw = clientPlayer.prevHeadYaw;
        }
        else
        {
            entity.prevBodyYaw = clientPlayer.prevBodyYaw;
            entity.bodyYaw = clientPlayer.bodyYaw;
        }
    }

    private Vec3d lastPosition = new Vec3d(0, 0, 0);

    private void sync(LivingEntity entity, PlayerEntity clientPlayer)
    {
        if (entity.isRemoved() || entity.world == null)
        {
            LoggerFactory.getLogger("MorphClient").warn("正试图更新一个已被移除的客户端预览实体");

            var id = morphManager.selfViewIdentifier.get();
            this.refreshClientViewEntity(id, id);
            return;
        }

        if (beamTarget != null && beamTarget.isRemoved())
            beamTarget = null;

        var playerPos = clientPlayer.getPos();
        entity.setPosition(playerPos.x, playerPos.y - 4096, playerPos.z);

        if (!entity.ignoreCameraFrustum)
            entity.tick();

        if (entity instanceof CamelEntity camelEntity)
        {
            var playerHasVehicle = clientPlayer.hasVehicle();
            var playerStanding = Vec3dUtils.horizontalSquaredDistance(lastPosition, clientPlayer.getPos()) == 0d;

            if (!playerStanding)
                lastPosition = clientPlayer.getPos();

            camelEntity.dashingAnimationState.setRunning(clientPlayer.isSprinting(), camelEntity.age);
            camelEntity.sittingAnimationState.setRunning(playerHasVehicle, camelEntity.age);

            //camelEntity.walkingAnimationState.setRunning(!playerHasVehicle && !playerStanding, camelEntity.age);
            camelEntity.idlingAnimationState.setRunning(playerStanding, camelEntity.age);
        }

        var sleepPos = clientPlayer.getSleepingPosition().orElse(null);

        if (sleepPos != null)
            entity.setSleepingPosition(sleepPos);
        else
            entity.clearSleepingPosition();

        var attribute = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);

        if (attribute != null)
            attribute.setBaseValue(clientPlayer.getMaxHealth());

        entity.setHealth(clientPlayer.getHealth());

        entity.handSwinging = clientPlayer.handSwinging;
        entity.handSwingProgress = clientPlayer.handSwingProgress;
        entity.lastHandSwingProgress = clientPlayer.lastHandSwingProgress;
        entity.handSwingTicks = clientPlayer.handSwingTicks;

        entity.preferredHand = clientPlayer.preferredHand;

        entity.setSneaking(clientPlayer.isSneaking());

        entity.hurtTime = clientPlayer.hurtTime;
        entity.deathTime = clientPlayer.deathTime;

        //entity.inPowderSnow = clientPlayer.inPowderSnow;
        entity.setFrozenTicks(clientPlayer.getFrozenTicks());

        //末影龙的Yaw和玩家是反的
        if (entity.getType() == EntityType.ENDER_DRAGON)
            entity.setYaw(180 + clientPlayer.getYaw());
        else
            entity.setYaw(clientPlayer.getYaw());

        entity.setOnGround(clientPlayer.isOnGround());

        ((EntityAccessor) entity).setTouchingWater(clientPlayer.isTouchingWater());

        //同步装备
        if (!morphManager.equipOverriden.get())
        {
            entity.equipStack(EquipmentSlot.MAINHAND, clientPlayer.getEquippedStack(EquipmentSlot.MAINHAND));
            entity.equipStack(EquipmentSlot.OFFHAND, clientPlayer.getEquippedStack(EquipmentSlot.OFFHAND));

            entity.equipStack(EquipmentSlot.HEAD, clientPlayer.getEquippedStack(EquipmentSlot.HEAD));
            entity.equipStack(EquipmentSlot.CHEST, clientPlayer.getEquippedStack(EquipmentSlot.CHEST));
            entity.equipStack(EquipmentSlot.LEGS, clientPlayer.getEquippedStack(EquipmentSlot.LEGS));
            entity.equipStack(EquipmentSlot.FEET, clientPlayer.getEquippedStack(EquipmentSlot.FEET));
        }
        else
        {
            var manager = MorphClient.getInstance().morphManager;

            entity.equipStack(EquipmentSlot.MAINHAND, manager.getOverridedItemStackOn(EquipmentSlot.MAINHAND));
            entity.equipStack(EquipmentSlot.OFFHAND, manager.getOverridedItemStackOn(EquipmentSlot.OFFHAND));

            entity.equipStack(EquipmentSlot.HEAD, manager.getOverridedItemStackOn(EquipmentSlot.HEAD));
            entity.equipStack(EquipmentSlot.CHEST, manager.getOverridedItemStackOn(EquipmentSlot.CHEST));
            entity.equipStack(EquipmentSlot.LEGS, manager.getOverridedItemStackOn(EquipmentSlot.LEGS));
            entity.equipStack(EquipmentSlot.FEET, manager.getOverridedItemStackOn(EquipmentSlot.FEET));
        }

        //同步Pose
        entity.setPose(clientPlayer.getPose());
        entity.setSwimming(clientPlayer.isSwimming());

        if (clientPlayer.hasVehicle())
            entity.startRiding(clientPlayer, true);
        else if (entity.hasVehicle())
            entity.stopRiding();

        entity.setStuckArrowCount(clientPlayer.getStuckArrowCount());

        if (entity instanceof MorphLocalPlayer player)
        {
            player.fallFlying = clientPlayer.isFallFlying();
            player.usingRiptide = clientPlayer.isUsingRiptide();

            player.fishHook = clientPlayer.fishHook;

            player.itemUseTimeLeft = clientPlayer.getItemUseTimeLeft();
            player.itemUseTime = clientPlayer.getItemUseTime();
            player.setActiveItem(clientPlayer.getActiveItem());

            player.setMainArm(clientPlayer.getMainArm());
        }

        entity.setInvisible(clientPlayer.isInvisible());
    }
}

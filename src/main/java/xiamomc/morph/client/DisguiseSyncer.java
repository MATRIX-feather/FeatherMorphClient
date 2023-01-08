package xiamomc.morph.client;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.morph.client.mixin.accessors.EntityAccessor;

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
            if (n != null) MorphClient.getInstance().schedule(() -> this.mergeNbt(n));
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

            prevEntity.hurtTime = 0;

            EntityCache.discardEntity(prevIdentifier);
        }

        entity = EntityCache.getEntity(newIdentifier);
        currentEntity.set(entity);

        allowTick = true;

        if (entity != null)
        {
            client.schedule(() -> clientWorld.addEntity(entity.getId(), entity));

            var nbt = morphManager.currentNbtCompound.get();
            if (nbt != null)
                client.schedule(() -> mergeNbt(nbt));
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

        clientPlayer.sendMessage(Text.literal("更新当前实体时出现错误。"));
        clientPlayer.sendMessage(Text.literal("在当前伪装变更前客户端预览将被禁用以避免游戏崩溃。"));
    }

    private void mergeNbt(NbtCompound nbtCompound)
    {
        if (entity != null)
            entity.readCustomDataFromNbt(nbtCompound);
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
        else
        {
            entity.bodyYaw = clientPlayer.bodyYaw;
            entity.prevBodyYaw = clientPlayer.prevBodyYaw;
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

        var playerPos = clientPlayer.getPos();
        entity.setPosition(playerPos.x, playerPos.y - 4096, playerPos.z);

        if (entity.getType() != EntityType.ENDER_DRAGON)
            entity.tick();

        if (entity instanceof CamelEntity camelEntity)
        {
            var playerHasVehicle = clientPlayer.hasVehicle();
            var playerStanding = Vec3dUtils.horizontalSquaredDistance(lastPosition, clientPlayer.getPos()) == 0d;

            if (!playerStanding)
                lastPosition = clientPlayer.getPos();

            camelEntity.dashingAnimationState.setRunning(clientPlayer.isSprinting(), camelEntity.age);
            camelEntity.sittingAnimationState.setRunning(playerHasVehicle, camelEntity.age);

            camelEntity.walkingAnimationState.setRunning(!playerHasVehicle && !playerStanding, camelEntity.age);
            camelEntity.idlingAnimationState.setRunning(playerStanding, camelEntity.age);
        }

        var sleepPos = clientPlayer.getSleepingPosition().orElse(null);

        if (sleepPos != null)
            entity.setSleepingPosition(sleepPos);
        else
            entity.clearSleepingPosition();

        entity.handSwinging = clientPlayer.handSwinging;
        entity.handSwingProgress = clientPlayer.handSwingProgress;
        entity.lastHandSwingProgress = clientPlayer.lastHandSwingProgress;
        entity.handSwingTicks = clientPlayer.handSwingTicks;

        entity.preferredHand = clientPlayer.preferredHand;

        entity.limbAngle = clientPlayer.limbAngle;
        entity.limbDistance = clientPlayer.limbDistance;
        entity.lastLimbDistance = clientPlayer.lastLimbDistance;

        entity.setSneaking(clientPlayer.isSneaking());

        entity.hurtTime = clientPlayer.hurtTime;
        entity.deathTime = clientPlayer.deathTime;

        //entity.inPowderSnow = clientPlayer.inPowderSnow;
        entity.setFrozenTicks(clientPlayer.getFrozenTicks());

        //末影龙的Yaw和玩家是反的
        if (entity.getType() == EntityType.ENDER_DRAGON)
            entity.setYaw(180 + clientPlayer.getYaw());

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
            entity.startRiding(clientPlayer);
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

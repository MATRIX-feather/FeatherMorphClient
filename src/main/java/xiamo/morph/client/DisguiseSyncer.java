package xiamo.morph.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import xiamo.morph.client.mixin.accessors.EntityAccessor;

public class DisguiseSyncer
{
    public DisguiseSyncer()
    {
        MorphClient.currentIdentifier.onValueChanged((o, n) -> this.onCurrentChanged(n));
    }

    private void onCurrentChanged(String newIdentifier)
    {
        var clientWorld = MinecraftClient.getInstance().world;
        if (clientWorld == null)
        {
            entity = null;
            return;
        }

        if (entity != null)
        {
            entity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

            entity.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);

            clientWorld.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        }

        this.entity = EntityCache.getEntity(newIdentifier);

        allowTick = true;

        if (entity != null)
        {
            clientWorld.addEntity(entity.getId(), entity);
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
            e.printStackTrace();
            allowTick = false;
        }
    }

    private boolean notick(EntityType<?> t)
    {
        return t == EntityType.ENDER_DRAGON
                || t == EntityType.GUARDIAN
                || t == EntityType.ELDER_GUARDIAN;
    }

    private void sync(LivingEntity entity, PlayerEntity clientPlayer)
    {
        var playerPos = clientPlayer.getPos();
        entity.setPosition(playerPos.x, -32767, playerPos.z);

        //黑名单里的实体不要tick
        if (!notick(entity.getType()))
            entity.tick();

        entity.setSprinting(clientPlayer.isSprinting());

        //幻翼的pitch需要倒转
        if (entity.getType().equals(EntityType.PHANTOM))
            entity.setPitch(-clientPlayer.getPitch());
        else
            entity.setPitch(clientPlayer.getPitch());

        entity.prevPitch = clientPlayer.prevPitch;

        entity.headYaw = clientPlayer.headYaw;
        entity.prevHeadYaw = clientPlayer.prevHeadYaw;

        if (entity.getType().equals(EntityType.ARMOR_STAND))
        {
            entity.bodyYaw = clientPlayer.headYaw;
            entity.prevBodyYaw = clientPlayer.prevHeadYaw;
        }
        else
        {
            entity.bodyYaw = clientPlayer.bodyYaw;
            entity.prevBodyYaw = clientPlayer.prevBodyYaw;
        }

        entity.limbAngle = clientPlayer.limbAngle;
        entity.limbDistance = clientPlayer.limbDistance;
        entity.lastLimbDistance = clientPlayer.lastLimbDistance;

        entity.inPowderSnow = clientPlayer.inPowderSnow;
        entity.setSneaking(clientPlayer.isSneaking());

        //末影龙的Yaw和玩家是反的
        if (entity.getType().equals(EntityType.ENDER_DRAGON))
            entity.setYaw(180 + clientPlayer.getYaw());

        entity.setOnGround(clientPlayer.isOnGround());

        ((EntityAccessor) entity).setTouchingWater(clientPlayer.isTouchingWater());

        //同步装备
        entity.equipStack(EquipmentSlot.MAINHAND, clientPlayer.getEquippedStack(EquipmentSlot.MAINHAND));
        entity.equipStack(EquipmentSlot.OFFHAND, clientPlayer.getEquippedStack(EquipmentSlot.OFFHAND));

        entity.equipStack(EquipmentSlot.HEAD, clientPlayer.getEquippedStack(EquipmentSlot.HEAD));
        entity.equipStack(EquipmentSlot.CHEST, clientPlayer.getEquippedStack(EquipmentSlot.CHEST));
        entity.equipStack(EquipmentSlot.LEGS, clientPlayer.getEquippedStack(EquipmentSlot.LEGS));
        entity.equipStack(EquipmentSlot.FEET, clientPlayer.getEquippedStack(EquipmentSlot.FEET));

        //同步Pose
        entity.setPose(clientPlayer.getPose());
        entity.setSwimming(clientPlayer.isSwimming());

        entity.setInvisible(clientPlayer.isInvisible());
    }
}

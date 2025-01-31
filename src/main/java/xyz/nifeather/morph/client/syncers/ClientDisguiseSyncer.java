package xyz.nifeather.morph.client.syncers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.ClientMorphManager;
import xyz.nifeather.morph.client.EntityCache;
import xyz.nifeather.morph.client.FeatherMorphClient;
import xyz.nifeather.morph.client.ServerHandler;
import xyz.nifeather.morph.client.entities.MorphLocalPlayer;
import xyz.nifeather.morph.client.graphics.CameraHelper;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;

public class ClientDisguiseSyncer extends DisguiseSyncer
{
    private static ClientDisguiseSyncer currentInstance;

    @Nullable
    public static ClientDisguiseSyncer getCurrentInstance()
    {
        return currentInstance;
    }

    public ClientDisguiseSyncer(AbstractClientPlayerEntity clientPlayer, String morphId, int networkId)
    {
        super(clientPlayer, morphId, networkId);

        currentInstance = this;
    }

    @Resolved(shouldSolveImmediately = true)
    private ClientMorphManager morphManager;

    private final Bindable<NbtCompound> currentNbtCompound = new Bindable<>(null);

    private final Bindable<Boolean> isThirdPerson = new Bindable<>(false);

    @Initializer
    private void load(ServerHandler serverHandler)
    {
        currentNbtCompound.bindTo(morphManager.currentNbtCompound);
        isThirdPerson.bindTo(CameraHelper.isThirdPerson);

        currentNbtCompound.onValueChanged((o, n) ->
        {
            if (n != null) FeatherMorphClient.getInstance().schedule(() -> this.mergeNbt(n));
        }, true);

        isThirdPerson.onValueChanged((o, n) -> this.onThirdPersonChange(disguiseInstance, MinecraftClient.getInstance().player));

        //ServerHandler.spiderEnabled.onValueChanged((o, n) -> this.isSpider = n);
    }

    private World prevWorld;

    @Override
    protected @NotNull EntityCache getEntityCache()
    {
        return EntityCache.getGlobalCache();
    }

    @Override
    public void refreshEntity()
    {
        super.refreshEntity();
        beamTarget = null;

        var clientPlayer = MinecraftClient.getInstance().player;
        if (clientPlayer != null)
            clientPlayer.calculateDimensions();
    }

    @Override
    protected void markSyncing()
    {
        syncing = true;
    }

    @Override
    protected void markNotSyncing()
    {
        syncing = false;
    }

    @Override
    public boolean isSyncing()
    {
        return syncing;
    }

    @Override
    protected void onTickError()
    {
        var clientPlayer = MinecraftClient.getInstance().player;
        assert clientPlayer != null;

        FeatherMorphClient.getInstance().updateClientView(true, false);

        clientPlayer.sendMessage(Text.translatable("text.morphclient.error.update_disguise1").formatted(Formatting.RED), false);
        clientPlayer.sendMessage(Text.translatable("text.morphclient.error.update_disguise2").formatted(Formatting.RED), false);
    }

    @Nullable
    public Entity getBeamTarget()
    {
        return beamTarget;
    }

    private void onThirdPersonChange(LivingEntity entity, AbstractClientPlayerEntity clientPlayer)
    {
        if (entity == null || clientPlayer == null) return;

        syncYawPitch();
    }

    //private boolean isSpider = false;

    @Override
    public void syncDraw()
    {
        if (disguiseInstance == null || bindingPlayer == null) return;

        syncYawPitch();
    }

    @Override
    protected void initialSync()
    {
        var entity = disguiseInstance;
        var clientPlayer = bindingPlayer;

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
    }

    public static boolean syncing;

    @Override
    protected void syncPosition()
    {
        if (disguiseInstance == null) return;

        var playerPos = bindingPlayer.getPos();
        disguiseInstance.setPosition(playerPos.x, playerPos.y - 4096, playerPos.z);
    }

    @Override
    public void syncTick()
    {
        if (this.disposed())
            throw new RuntimeException("May not update a disposed DisguiseSyncer");

        var clientPlayer = MinecraftClient.getInstance().player;
        if (bindingPlayer != clientPlayer && clientPlayer != null)
            bindingPlayer = clientPlayer;

        if (disguiseInstance == null || disguiseInstance.isRemoved() || disguiseInstance.getWorld() == null)
        {
            logger.warn("Trying to update an removed entity " + disguiseInstance);

            this.refreshEntity();
            return;
        }

        baseSync();
        syncYawPitch();
    }

    @Override
    protected @Nullable NbtCompound getCompound()
    {
        return morphManager.currentNbtCompound.get();
    }

    @Override
    protected void syncEquipments()
    {
        if (disguiseInstance == null) return;

        //同步装备
        if (!morphManager.equipOverriden.get())
        {
            disguiseInstance.equipStack(EquipmentSlot.MAINHAND, bindingPlayer.getEquippedStack(EquipmentSlot.MAINHAND));
            disguiseInstance.equipStack(EquipmentSlot.OFFHAND, bindingPlayer.getEquippedStack(EquipmentSlot.OFFHAND));

            disguiseInstance.equipStack(EquipmentSlot.HEAD, bindingPlayer.getEquippedStack(EquipmentSlot.HEAD));
            disguiseInstance.equipStack(EquipmentSlot.CHEST, bindingPlayer.getEquippedStack(EquipmentSlot.CHEST));
            disguiseInstance.equipStack(EquipmentSlot.LEGS, bindingPlayer.getEquippedStack(EquipmentSlot.LEGS));
            disguiseInstance.equipStack(EquipmentSlot.FEET, bindingPlayer.getEquippedStack(EquipmentSlot.FEET));
        }
        else
        {
            var manager = FeatherMorphClient.getInstance().morphManager;

            disguiseInstance.equipStack(EquipmentSlot.MAINHAND, manager.getOverridedItemStackOn(EquipmentSlot.MAINHAND));
            disguiseInstance.equipStack(EquipmentSlot.OFFHAND, manager.getOverridedItemStackOn(EquipmentSlot.OFFHAND));

            disguiseInstance.equipStack(EquipmentSlot.HEAD, manager.getOverridedItemStackOn(EquipmentSlot.HEAD));
            disguiseInstance.equipStack(EquipmentSlot.CHEST, manager.getOverridedItemStackOn(EquipmentSlot.CHEST));
            disguiseInstance.equipStack(EquipmentSlot.LEGS, manager.getOverridedItemStackOn(EquipmentSlot.LEGS));
            disguiseInstance.equipStack(EquipmentSlot.FEET, manager.getOverridedItemStackOn(EquipmentSlot.FEET));
        }

    }

    @Override
    protected boolean showOverridedEquips()
    {
        return morphManager.equipOverriden.get();
    }

    @Override
    protected void onDispose()
    {
        currentNbtCompound.unBindFromTarget();
        currentNbtCompound.unBindBindings();

        isThirdPerson.unBindFromTarget();
        isThirdPerson.unBindBindings();
    }
}

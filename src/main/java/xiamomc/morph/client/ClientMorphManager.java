package xiamomc.morph.client;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.graphics.toasts.DisguiseEntryToast;
import xiamomc.morph.client.graphics.toasts.NewDisguiseSetToast;
import xiamomc.morph.client.syncers.ClientDisguiseSyncer;
import xiamomc.morph.client.syncers.DisguiseSyncer;
import xiamomc.morph.client.syncers.OtherClientDisguiseSyncer;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Exceptions.NullDependencyException;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Function;

public class ClientMorphManager extends MorphClientObject
{
    private final SortedSet<String> availableMorphs = new ObjectAVLTreeSet<>();

    public List<String> getAvailableMorphs()
    {
        return availableMorphs.stream().toList();
    }

    public void clearAvailableDisguises()
    {
        var disguises = new ObjectArrayList<>(availableMorphs);
        availableMorphs.clear();

        invokeRevoke(disguises);
    }

    //region Common

    public final Bindable<String> selectedIdentifier = new Bindable<>(null);

    public final Bindable<String> currentIdentifier = new Bindable<>(null);

    public final Bindable<Boolean> equipOverriden = new Bindable<>(false);

    public final Bindable<NbtCompound> currentNbtCompound = new Bindable<>(null);

    public final Bindable<Float> revealingValue = new Bindable<>(0f);

    public final Map<Integer, String> playerMap = new Object2ObjectArrayMap<>();

    @Resolved
    private DisguiseInstanceTracker instanceTracker;

    //endregion

    @Nullable
    private DisguiseSyncer localPlayerSyncer;

    @Initializer
    private void load()
    {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            playerMap.clear();

            world = null;
            prevWorld = null;

            reset();
        });

        this.addSchedule(this::update);
    }

    private ClientWorld world;
    private ClientWorld prevWorld;

    private void update()
    {
        this.addSchedule(this::update);

        world = MinecraftClient.getInstance().world;

        if (prevWorld == null)
            prevWorld = world;

        if (world != null && world != prevWorld)
        {
            prevWorld = world;
            //refreshLocalSyncer(this.currentIdentifier.get());
        }
    }

    private void refreshLocalSyncer(String n)
    {
        if (localPlayerSyncer != null)
        {
            logger.info("Removing previous syncer " + localPlayerSyncer);
            instanceTracker.removeSyncer(localPlayerSyncer);
            localPlayerSyncer.dispose();
            localPlayerSyncer = null;
        }

        if (n == null || n.isEmpty()) return;

        localPlayerSyncer = instanceTracker.setSyncer(MinecraftClient.getInstance().player, n);
    }

    //region Add/Remove/Set disguises

    public final Bindable<Boolean> selfVisibleEnabled = new Bindable<>(false);

    private final List<Function<List<String>, Boolean>> onGrantConsumers = new ObjectArrayList<>();
    public void onMorphGrant(Function<List<String>, Boolean> consumer)
    {
        onGrantConsumers.add(consumer);
    }

    private final List<Function<List<String>, Boolean>> onRevokeConsumers = new ObjectArrayList<>();
    public void onMorphRevoke(Function<List<String>, Boolean> consumer)
    {
        onRevokeConsumers.add(consumer);
    }

    private void invokeRevoke(List<String> diff)
    {
        var tobeRemoved = new ObjectArrayList<Function<List<String>, Boolean>>();

        onRevokeConsumers.forEach(f ->
        {
            if (!f.apply(diff)) tobeRemoved.add(f);
        });

        onRevokeConsumers.removeAll(tobeRemoved);
    }

    private void invokeGrant(List<String> diff)
    {
        var tobeRemoved = new ObjectArrayList<Function<List<String>, Boolean>>();

        onGrantConsumers.forEach(f ->
        {
            if (!f.apply(diff)) tobeRemoved.add(f);
        });

        onGrantConsumers.removeAll(tobeRemoved);
    }

    public void setDisguises(List<String> identifiers, boolean displayToasts)
    {
        invokeRevoke(availableMorphs.stream().toList());

        availableMorphs.clear();

        this.addDisguises(identifiers, false);

        DisguiseEntryToast.invalidateAll();

        if (displayToasts)
            toastManager.add(new NewDisguiseSetToast(availableMorphs.size() <= 0));
    }

    private final ToastManager toastManager = MinecraftClient.getInstance().getToastManager();

    public void addDisguises(List<String> identifiers, boolean displayToasts)
    {
        identifiers = new ObjectArrayList<>(identifiers);

        identifiers.removeIf(availableMorphs::contains);
        identifiers.forEach(i -> addDisguisePrivate(i, displayToasts));

        invokeGrant(identifiers);
    }

    public void addDisguise(String identifier, boolean displayToasts)
    {
        addDisguisePrivate(identifier, displayToasts);
    }

    public void removeDisguises(List<String> identifiers, boolean displayToasts)
    {
        identifiers.forEach(i -> removeDisguisePrivate(i, displayToasts));

        invokeRevoke(identifiers);
    }

    public void removeDisguise(String identifier, boolean displayToasts)
    {
        removeDisguisePrivate(identifier, displayToasts);
    }

    private void addDisguisePrivate(String identifier, boolean displayToasts)
    {
        if (identifier.isEmpty()) return;

        availableMorphs.add(identifier);

        if (displayToasts)
            toastManager.add(new DisguiseEntryToast(identifier, true));
    }

    private void removeDisguisePrivate(String identifier, boolean displayToasts)
    {
        availableMorphs.remove(identifier);

        if (displayToasts)
            toastManager.add(new DisguiseEntryToast(identifier, false));
    }

    //endregion Add/Remove/Set disguises

    //region Items

    private final Map<EquipmentSlot, ItemStack> equipmentSlotItemStackMap = new Object2ObjectOpenHashMap<>();

    public ItemStack getOverridedItemStackOn(EquipmentSlot slot)
    {
        return equipmentSlotItemStackMap.getOrDefault(slot, air);
    }

    public void swapHand()
    {
        var mainHand = equipmentSlotItemStackMap.getOrDefault(EquipmentSlot.MAINHAND, air);
        var offHand = equipmentSlotItemStackMap.getOrDefault(EquipmentSlot.OFFHAND, air);
        equipmentSlotItemStackMap.put(EquipmentSlot.MAINHAND, offHand);
        equipmentSlotItemStackMap.put(EquipmentSlot.OFFHAND, mainHand);
    }

    public void setEquip(EquipmentSlot slot, ItemStack item)
    {
        equipmentSlotItemStackMap.put(slot, item);
    }

    private final ItemStack air = ItemStack.EMPTY;

    //endregion Items

    public void reset()
    {
        this.clearAvailableDisguises();

        selectedIdentifier.set(null);
        currentIdentifier.set(null);

        revealingValue.set(0f);
        if (localPlayerSyncer != null)
            localPlayerSyncer.dispose();

        localPlayerSyncer = null;

        prevWorld = null;
        world = null;
    }

    public void setCurrent(String val)
    {
        if (localPlayerSyncer != null)
            localPlayerSyncer.dispose();

        localPlayerSyncer = null;

        String finalVal = val;
        this.addSchedule(() -> refreshLocalSyncer(finalVal));

        if (val != null && (val.isBlank() || val.isEmpty()))
            val = null;

        currentIdentifier.set(val);

        equipOverriden.set(false);
        equipmentSlotItemStackMap.clear();
        currentNbtCompound.set(null);
    }

    public DisguiseSyncer createSyncerFor(AbstractClientPlayerEntity player, String disguiseId, int networkId)
    {
        var clientPlayer = MinecraftClient.getInstance().player;
        if (clientPlayer == null)
            throw new NullDependencyException("Required non-null client player to get DisguiseSyncer");

        DisguiseSyncer syncer;
        if (clientPlayer == player)
            syncer = new ClientDisguiseSyncer(player, disguiseId, networkId);
        else
            syncer = new OtherClientDisguiseSyncer(player, disguiseId, networkId);

        return syncer;
    }

    public void updateSkin(GameProfile profile)
    {
        if (localPlayerSyncer != null)
        {
            localPlayerSyncer.updateSkin(profile);
        }
        else
        {
            logger.warn("Calling UpdateSkin while localPlayerSyncer is null!");
            Thread.dumpStack();
        }
    }
}

package xiamomc.morph.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import xiamomc.morph.client.bindables.Bindable;
import xiamomc.morph.client.bindables.BindableList;
import xiamomc.morph.misc.ClientItemUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ClientMorphManager
{
    private final BindableList<String> availableMorphs = new BindableList<>();

    public List<String> getAvailableMorphs()
    {
        return availableMorphs;
    }

    public void clearAvailableDisguises()
    {
        var disguises = new ObjectArrayList<>(availableMorphs);
        availableMorphs.clear();

        invokeRevoke(disguises);
    }

    //region Common

    public static final Bindable<String> selectedIdentifier = new Bindable<>(null);

    public static final Bindable<String> currentIdentifier = new Bindable<>(null);

    public static final Bindable<String> selfViewIdentifier = new Bindable<>(null);

    public static final Bindable<Boolean> equipOverriden = new Bindable<>(false);

    public static final Bindable<NbtCompound> currentNbtCompound = new Bindable<>(null);

    //endregion

    //region Add/Remove/Set disguises

    public final Bindable<Boolean> selfVisibleToggled = new Bindable<>(false);

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

    public void setDisguises(List<String> identifiers)
    {
        invokeRevoke(availableMorphs);

        availableMorphs.clear();

        this.addDisguises(identifiers);
    }

    public void addDisguises(List<String> identifiers)
    {
        identifiers.removeIf(availableMorphs::contains);
        identifiers.forEach(this::addDisguisePrivate);

        invokeGrant(identifiers);
    }

    public void addDisguise(String identifier)
    {
        addDisguisePrivate(identifier);
    }

    public void removeDisguises(List<String> identifiers)
    {
        identifiers.forEach(this::removeDisguisePrivate);
        invokeRevoke(identifiers);
    }

    public void removeDisguise(String identifier)
    {
        availableMorphs.remove(identifier);
    }

    private void addDisguisePrivate(String identifier)
    {
        availableMorphs.add(identifier);
    }

    private void removeDisguisePrivate(String identifier)
    {
        availableMorphs.remove(identifier);
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
        selectedIdentifier.set(null);
    }

    public void setCurrent(String val)
    {
        currentIdentifier.set(val);

        equipOverriden.set(false);
        selfViewIdentifier.set(null);
        equipmentSlotItemStackMap.clear();
    }
}

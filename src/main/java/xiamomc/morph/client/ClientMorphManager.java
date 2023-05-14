package xiamomc.morph.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xiamomc.morph.client.graphics.color.ColorUtils;
import xiamomc.morph.client.graphics.toasts.DisguiseEntryToast;
import xiamomc.morph.client.graphics.toasts.NewDisguiseSetToast;
import xiamomc.pluginbase.Bindables.Bindable;

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

    public final Bindable<String> selfViewIdentifier = new Bindable<>(null);

    public final Bindable<Boolean> equipOverriden = new Bindable<>(false);

    public final Bindable<NbtCompound> currentNbtCompound = new Bindable<>(null);

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

    public void setDisguises(List<String> identifiers, boolean displayToasts)
    {
        invokeRevoke(availableMorphs.stream().toList());

        availableMorphs.clear();

        this.addDisguises(identifiers, false);

        DisguiseEntryToast.invalidateAll();

        if (displayToasts)
        {
            var toast = new NewDisguiseSetToast();

            var transId = "text.morphclient.toast.new_disguises";
            toast.setTitle(Text.translatable(transId));
            toast.setDescription(Text.translatable(transId + (availableMorphs.size() > 0 ? ".desc" : ".all_gone"), Text.keybind("key.morphclient.morph").formatted(Formatting.ITALIC)));
            toast.setLineColor(ColorUtils.fromHex("#009688"));

            toastManager.add(toast);
        }
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
        selfViewIdentifier.set(null);
    }

    public void setCurrent(String val)
    {
        if (val != null && (val.isBlank() || val.isEmpty()))
            val = null;

        currentIdentifier.set(val);

        equipOverriden.set(false);
        selfViewIdentifier.set(null);
        equipmentSlotItemStackMap.clear();
        currentNbtCompound.set(null);
    }
}

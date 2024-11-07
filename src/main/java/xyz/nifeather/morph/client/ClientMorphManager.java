package xyz.nifeather.morph.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.graphics.toasts.DisguiseEntryToast;
import xyz.nifeather.morph.client.graphics.toasts.NewDisguiseSetToast;
import xyz.nifeather.morph.client.syncers.ClientDisguiseSyncer;
import xyz.nifeather.morph.client.syncers.DisguiseSyncer;
import xyz.nifeather.morph.client.syncers.OtherClientDisguiseSyncer;
import xyz.nifeather.morph.client.syncers.animations.AnimHandlerIndex;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Exceptions.NullDependencyException;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Resolved
    private DisguiseInstanceTracker instanceTracker;

    //endregion

    private final List<String> emotes = new ObjectArrayList<>();

    public void setEmotes(List<String> emotes)
    {
        if (emotes.size() > 4)
            logger.warn("Server send a emote that has more than 4 elements!");

        this.emotes.clear();
        this.emotes.addAll(emotes);
    }

    @Nullable
    public String lastEmote;

    @Nullable
    public String emoteDisplayName;

    public void setEmoteDisplay(String id)
    {
        this.emoteDisplayName = id;
    }

    public void playEmote(String emote)
    {
        if (!emote.equals(AnimationNames.RESET) && !emote.equals(AnimationNames.TRY_RESET))
            this.lastEmote = emote;
        else
            this.lastEmote = null;

        if (localPlayerSyncer != null)
            localPlayerSyncer.playAnimation(emote);
    }

    public List<String> getEmotes()
    {
        return new ObjectArrayList<>(emotes);
    }

    @Nullable
    private DisguiseSyncer localPlayerSyncer;

    @Initializer
    private void load()
    {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            if (RenderSystem.isOnRenderThread())
                onDisconnect();
            else
                this.addSchedule(this::onDisconnect);
        });

        this.addSchedule(this::update);
    }

    private void onDisconnect()
    {
        world = null;
        prevWorld = null;

        reset();
    }

    private ClientWorld world;
    private ClientWorld prevWorld;

    private void update()
    {
        this.addSchedule(this::update);

        world = MinecraftClient.getInstance().world;

        if (world == null) return;

        if (prevWorld == null)
            prevWorld = world;

        if (world != prevWorld)
            prevWorld = world;

        var currentClientPlayer = MinecraftClient.getInstance().player;
        if (currentClientPlayer != null && lastClientPlayer != currentClientPlayer && !syncerRefreshScheduled.get())
        {
            if (localPlayerSyncer != null && localPlayerSyncer.disposed())
                localPlayerSyncer = null;

            refreshLocalSyncer(currentIdentifier.get());
        }

        lastClientPlayer = currentClientPlayer;
    }

    @Nullable
    private PlayerEntity lastClientPlayer;

    private final AtomicBoolean syncerRefreshScheduled = new AtomicBoolean(false);

    private void refreshLocalSyncer(String n)
    {
        syncerRefreshScheduled.set(false);

        if (localPlayerSyncer != null)
        {
            logger.info("Removing previous syncer " + localPlayerSyncer);
            instanceTracker.removeSyncer(localPlayerSyncer);
            localPlayerSyncer.dispose();
            localPlayerSyncer = null;
        }

        if (n == null || n.isEmpty()) return;

        localPlayerSyncer = instanceTracker.setSyncer(MinecraftClient.getInstance().player, n);

        if (localPlayerSyncer == null) return;

        if (lastEmote != null)
            localPlayerSyncer.playAnimation(lastEmote);

        if (serverSkin != null)
            localPlayerSyncer.updateSkin(serverSkin);
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

        this.setEmotes(List.of());

        selectedIdentifier.set(null);
        currentIdentifier.set(null);

        revealingValue.set(0f);
        if (localPlayerSyncer != null)
            localPlayerSyncer.dispose();

        localPlayerSyncer = null;
        lastEmote = null;

        EntityCache.getGlobalCache().dropAll();

        prevWorld = null;
        world = null;
        lastClientPlayer = null;
    }

    public void setCurrent(String val)
    {
        if (localPlayerSyncer != null)
            localPlayerSyncer.dispose();

        localPlayerSyncer = null;
        lastEmote = null;
        emoteDisplayName = null;
        serverSkin = null;

        String finalVal = val;
        this.addSchedule(() -> refreshLocalSyncer(finalVal));
        syncerRefreshScheduled.set(true);

        if (val != null && (val.isBlank() || val.isEmpty()))
            val = null;

        currentIdentifier.set(val);

        equipOverriden.set(false);
        equipmentSlotItemStackMap.clear();
        currentNbtCompound.set(null);
    }

    @Resolved
    private AnimHandlerIndex animIndex;

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

        var handler = animIndex.get(disguiseId);
        syncer.setAnimationHandler(handler);

        return syncer;
    }

    @Nullable
    private GameProfile serverSkin;

    public void updateSkin(GameProfile profile)
    {
        serverSkin = profile;

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

    private final Map<Integer, String> quickDisguiseMap = new Object2ObjectArrayMap<>();

    public void setupQuickDisguise(Map<Integer, String> newMap)
    {
        this.clearQuickDisguise();
        this.quickDisguiseMap.putAll(newMap);
    }

    public void setupQuickDisguise(int index, String disguiseID)
    {
        quickDisguiseMap.put(index, disguiseID);
    }

    public void clearQuickDisguise()
    {
        quickDisguiseMap.clear();
    }

    private void testClientDisguise(int index)
    {
        String[] disguises = new String[]
                {
                        "player:Faruzan_",
                        "minecraft:sheep",
                        "minecraft:ender_dragon",
                        "player:Tairiitsuu"
                };

        var player = MinecraftClient.getInstance().player;
        player.sendMessage(Text.literal("Test! " + disguises[index]), true);
        this.selfVisibleEnabled.set(!this.selfVisibleEnabled.get());

        this.availableMorphs.clear();
        this.availableMorphs.addAll(List.of("minecraft:sheep", "player:Faruzan_", "minecraft:ender_dragon", "player:Tairiitsuu"));
        MorphClient.getInstance().serverHandler.testSetServerReady();
        this.setEmotes(List.of("crawl", "sniff", "lay", "roar"));
        this.setCurrent(disguises[index]);
    }

    public void onQuickDisguise(int index)
    {
        if (1+1==2)
        {
            testClientDisguise(index);
            return;
        }

        MinecraftClient.getInstance().player.sendMessage(Text.literal("QUick Disguise! " + index), false);

        var disguise = quickDisguiseMap.getOrDefault(index, null);
        if (disguise == null) return;

        MorphClient.getInstance().sendMorphCommand(disguise);
    }
}

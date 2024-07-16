package xiamomc.morph.client;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentMapImpl;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.utilties.NbtHelperCopy;
import xiamomc.morph.client.utilties.NbtUtils;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMeta;

public class ConvertedMeta
{
    private static final Logger log = LoggerFactory.getLogger(ConvertedMeta.class);
    @Nullable
    public NbtCompound nbt;

    @Nullable
    public GameProfile profileNbt;

    @Nullable
    public ConvertedEquipment convertedEquipment;

    public boolean showOverridedEquips;

    public boolean outdated = false;

    public void mergeFrom(ConvertedMeta other)
    {
        if (other.nbt != null)
            this.nbt = other.nbt;

        if (other.profileNbt != null)
            this.profileNbt = other.profileNbt;

        if (other.convertedEquipment != null)
            this.convertedEquipment = other.convertedEquipment;

        this.showOverridedEquips = other.showOverridedEquips;
    }

    public static class ConvertedEquipment
    {
        @NotNull
        public ItemStack head = ItemStack.EMPTY;

        @NotNull
        public ItemStack chest = ItemStack.EMPTY;

        @NotNull
        public ItemStack leggings = ItemStack.EMPTY;

        @NotNull
        public ItemStack feet = ItemStack.EMPTY;

        @NotNull
        public ItemStack mainHand = ItemStack.EMPTY;

        @NotNull
        public ItemStack offHand = ItemStack.EMPTY;

        public boolean isEmpty()
        {
            return head.isEmpty() && chest.isEmpty() && leggings.isEmpty() && feet.isEmpty() && mainHand.isEmpty() && offHand.isEmpty();
        }

        public static ConvertedEquipment from(ItemStack... stacks)
        {
            var instance = new ConvertedEquipment();

            for (int i = 0; i < stacks.length; i++)
            {
                var item = stacks[i];

                if (i == 0) instance.head = item;
                else if (i == 1) instance.chest = item;
                else if (i == 2) instance.leggings = item;
                else if (i == 3) instance.feet = item;
                else if (i == 4) instance.mainHand = item;
                else if (i == 5) instance.offHand = item;
            }

            return instance;
        }
    }

    public static ConvertedMeta of(S2CRenderMeta renderMeta, RegistryWrapper.WrapperLookup registry)
    {
        var instance = new ConvertedMeta();

        var logger = LoggerFactory.getLogger("MorphClient");

        // NBT
        instance.nbt = NbtUtils.parseSNbt(renderMeta.sNbt);

        // Profile
        var profileNbt = NbtUtils.parseSNbt(renderMeta.profileCompound);

        if (profileNbt != null)
            instance.profileNbt = NbtHelperCopy.toGameProfile(profileNbt);

        instance.showOverridedEquips = renderMeta.showOverridedEquipment;

        // Equipment
        try
        {
            var overridedEquipment = renderMeta.overridedEquipment;

            if (overridedEquipment != null)
            {
                String[] eqIds = new String[]
                        {
                                overridedEquipment.headId,
                                overridedEquipment.chestId,
                                overridedEquipment.leggingId,
                                overridedEquipment.feetId,
                                overridedEquipment.handId,
                                overridedEquipment.offhandId
                        };

                String[] eqNbt = new String[]
                        {
                                overridedEquipment.headNbt,
                                overridedEquipment.chestNbt,
                                overridedEquipment.leggingNbt,
                                overridedEquipment.feetNbt,
                                overridedEquipment.handNbt,
                                overridedEquipment.offhandNbt
                        };

                var items = new ObjectArrayList<ItemStack>();

                for (int i = 0; i < eqIds.length - 1; i++)
                {
                    var itemId = eqIds[i];
                    var identifier = Identifier.tryParse(itemId);

                    ItemStack item;

                    if (identifier == null)
                    {
                        logger.warn("Cannot parse item id %s, ignoring".formatted(itemId));
                        item = ItemStack.EMPTY;
                    }
                    else
                    {
                        item = new ItemStack(Registries.ITEM.get(identifier));
                    }

                    // todo: IMPLEMENT THIS
                    //var itemNbt = NbtUtils.parseSNbt(eqNbt[i]);
                    //logger.info("todo: IMPLEMENT CONVERTEDMETA ITEM SETNBT!!!");
                    //item.setNbt(itemNbt);
                    item.setCount(Math.max(item.getCount(), 1));

                    items.add(item);
                }

                instance.convertedEquipment = ConvertedEquipment.from(items.toArray(new ItemStack[]{}));
            }

            return instance;
        }
        catch (Throwable t)
        {
            logger.error("Error occurred while parsing meta: %s".formatted(t.getMessage()));
            t.printStackTrace();

            return null;
        }
    }
}

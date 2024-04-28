package xiamomc.morph.client.utilties;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.UUID;

public class NbtHelperCopy
{
    @Nullable
    public static GameProfile toGameProfile(NbtCompound nbt) {
        UUID uUID = nbt.containsUuid("Id") ? nbt.getUuid("Id") : Util.NIL_UUID;
        String string = nbt.getString("Name");

        try {
            GameProfile gameProfile = new GameProfile(uUID, string);
            if (nbt.contains("Properties", 10)) {
                NbtCompound nbtCompound = nbt.getCompound("Properties");
                Iterator var5 = nbtCompound.getKeys().iterator();

                while(var5.hasNext()) {
                    String string2 = (String)var5.next();
                    NbtList nbtList = nbtCompound.getList(string2, 10);

                    for(int i = 0; i < nbtList.size(); ++i) {
                        NbtCompound nbtCompound2 = nbtList.getCompound(i);
                        String string3 = nbtCompound2.getString("Value");
                        if (nbtCompound2.contains("Signature", 8)) {
                            gameProfile.getProperties().put(string2, new Property(string2, string3, nbtCompound2.getString("Signature")));
                        } else {
                            gameProfile.getProperties().put(string2, new Property(string2, string3));
                        }
                    }
                }
            }

            return gameProfile;
        } catch (Throwable var11) {
            System.err.println("[FeatherMorph] Failed parsing compound to GameProfile: " + var11.getMessage());
            var11.printStackTrace();

            return null;
        }
    }

}

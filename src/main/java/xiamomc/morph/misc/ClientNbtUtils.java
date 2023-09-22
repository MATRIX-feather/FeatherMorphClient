package xiamomc.morph.misc;

import net.minecraft.command.EntityDataObject;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.visitor.StringNbtWriter;

public class ClientNbtUtils
{
    /**
     * 获取目标实体的 {@link net.minecraft.nbt.NbtCompound}
     * @param entity 目标实体
     * @return 此实体的NBT数据，当实体为null或不为 {@link Entity} 的实例时返回null
     */
    public static NbtCompound getRawTagCompound(Entity entity)
    {
        var dataSource = new EntityDataObject(entity);
        return dataSource.getNbt();
    }

    /**
     * 将目标NBT序列化为字符串
     * @param compound 目标NBT
     * @return 由此NBT序列化的字符串，当compound为null时返回null
     */
    public static String getCompoundString(NbtCompound compound)
    {
        if (compound == null) return null;

        //StringNbtWriter
        var visitor = new StringNbtWriter();

        //StringNbtWriter#apply(NbtElement)
        return visitor.apply(compound);
    }
}

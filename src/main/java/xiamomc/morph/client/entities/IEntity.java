package xiamomc.morph.client.entities;

import net.minecraft.entity.EntityPose;
import org.jetbrains.annotations.Nullable;

public interface IEntity
{
    public void featherMorph$setOverridePose(@Nullable EntityPose newPose);
}

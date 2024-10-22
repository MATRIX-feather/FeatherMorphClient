package xyz.nifeather.morph.client.entities;

import net.minecraft.entity.EntityPose;
import org.jetbrains.annotations.Nullable;

public interface IEntity
{
    public void featherMorph$overridePose(@Nullable EntityPose newPose);
    public void featherMorph$overrideInvisibility(boolean invisible);
    public void featherMorph$setNoAcceptSetPose(boolean noAccept);
}

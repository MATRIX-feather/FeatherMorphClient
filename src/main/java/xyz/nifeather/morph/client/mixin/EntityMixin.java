package xyz.nifeather.morph.client.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.nifeather.morph.client.entities.IMorphClientEntity;

@Mixin(Entity.class)
public abstract class EntityMixin implements IMorphClientEntity
{
    @Unique
    private boolean featherMorph$isDisguiseEntity;

    @Unique
    private int featherMorph$masterId = -1;

    @Override
    public void featherMorph$setIsDisguiseEntity(int masterId)
    {
        this.featherMorph$masterId = masterId;
        this.featherMorph$isDisguiseEntity = true;
    }

    @Override
    public boolean featherMorph$isDisguiseEntity()
    {
        return this.featherMorph$isDisguiseEntity;
    }

    @Override
    public int featherMorph$getMasterEntityId()
    {
        return this.featherMorph$masterId;
    }
}

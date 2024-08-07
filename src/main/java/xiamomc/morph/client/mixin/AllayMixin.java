package xiamomc.morph.client.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xiamomc.morph.client.entities.IAllay;

@Mixin(AllayEntity.class)
public abstract class AllayMixin extends Entity implements IAllay
{
    @Shadow @Final private static TrackedData<Boolean> DANCING;

    public AllayMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void morphclient$forceSetDancing(boolean dancing)
    {
        this.dataTracker.set(DANCING, dancing);
    }
}

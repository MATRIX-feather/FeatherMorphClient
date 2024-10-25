package xyz.nifeather.morph.shared.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.shared.entities.IMorphEntity;

@Mixin(Entity.class)
public abstract class EntityMixin implements IMorphEntity
{
    @Shadow public abstract void setBoundingBox(Box boundingBox);

    @Shadow @Final private static Box NULL_BOX;
    @Shadow private Box boundingBox;
    @Shadow @Final private static Logger LOGGER;
    @Unique
    private boolean featherMorph$isDisguiseEntity;

    @Unique
    private int featherMorph$masterId = -1;

    @Override
    public void featherMorph$setIsDisguiseEntity(int masterId)
    {
        this.featherMorph$masterId = masterId;
        this.featherMorph$isDisguiseEntity = true;

        this.setBoundingBox(NULL_BOX);
    }

    @Inject(method = "setBoundingBox", at = @At("HEAD"), cancellable = true)
    public void morphclient$disableBoundingBoxForDisguiseInstances(Box boundingBox, CallbackInfo ci)
    {
        if (this.featherMorph$isDisguiseEntity)
        {
            //this.boundingBox = NULL_BOX;
            //ci.cancel();
        }
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

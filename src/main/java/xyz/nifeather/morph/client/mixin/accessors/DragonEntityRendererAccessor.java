package xyz.nifeather.morph.client.mixin.accessors;

import net.minecraft.client.render.entity.DragonEntityModel;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragonEntityRenderer.class)
public interface DragonEntityRendererAccessor
{
    @Accessor
    DragonEntityModel getModel();
}

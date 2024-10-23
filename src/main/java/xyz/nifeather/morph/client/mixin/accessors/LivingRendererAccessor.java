package xyz.nifeather.morph.client.mixin.accessors;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingRendererAccessor
{
    @Invoker
    public RenderLayer callGetRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline);
}

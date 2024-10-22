package xyz.nifeather.morph.client.graphics.toasts;

import net.minecraft.client.MinecraftClient;
import xyz.nifeather.morph.client.MorphClientObject;
import xiamomc.pluginbase.Annotations.Initializer;

public class MorphToastHelper extends MorphClientObject
{
    @Initializer
    private void load()
    {
        var toastMgr = MinecraftClient.getInstance().getToastManager();
    }
}

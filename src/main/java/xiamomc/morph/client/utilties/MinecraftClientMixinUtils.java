package xiamomc.morph.client.utilties;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ApiServices;
import xiamomc.morph.client.entities.MorphLocalPlayer;

import java.io.File;

public class MinecraftClientMixinUtils
{
    public static void setApiService(YggdrasilAuthenticationService authenticationService, File runDirectory)
    {
        ApiServices apiServices = ApiServices.create(authenticationService, runDirectory);
        apiServices.userCache().setExecutor(MinecraftClient.getInstance());

        MorphLocalPlayer.setMinecraftAPIServices(apiServices, MinecraftClient.getInstance());
    }
}

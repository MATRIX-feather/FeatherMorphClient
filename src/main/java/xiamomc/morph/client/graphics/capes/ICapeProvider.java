package xiamomc.morph.client.graphics.capes;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public interface ICapeProvider
{
    /**
     * 尝试获取和profile对应的披风
     * @param profile {@link GameProfile}
     * @param callback 获取到披风后执行的callback，参数为披风的 {@link Identifier}, 若参数为null则代表披风不可用
     */
    public void getCape(GameProfile profile, Consumer<Identifier> callback);
}

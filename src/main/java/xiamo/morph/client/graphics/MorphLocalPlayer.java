package xiamo.morph.client.graphics;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class MorphLocalPlayer extends OtherClientPlayerEntity
{
    private GameProfile morphProfile;

    private Identifier morphTextureIdentifier;
    private String model;

    public static TrackedData<Byte> getPMPMask()
    {
        return PLAYER_MODEL_PARTS;
    }

    public MorphLocalPlayer(ClientWorld clientWorld, GameProfile profile, @Nullable PlayerPublicKey playerPublicKey)
    {
        super(clientWorld, profile, playerPublicKey);

        LoggerFactory.getLogger("morph").info("Fetching skin for " + profile);

        var playerName = profile.getName();

        SkullBlockEntity.loadProperties(profile, o ->
        {
            MinecraftClient.getInstance().getSkinProvider().loadSkin(o, (type, id, texture) ->
            {
                if (type == MinecraftProfileTexture.Type.SKIN)
                {
                    LoggerFactory.getLogger("morph").info("Loading skin for " + playerName + " :: " + id.toString());

                    this.morphProfile = o;
                    this.morphTextureIdentifier = id;
                    this.model = texture.getMetadata("model");

                    AbstractClientPlayerEntity.loadSkin(id, playerName);
                }
            }, true);
        });
    }

    @Override
    public Vec3d getPos()
    {
        var clientPlayer = MinecraftClient.getInstance().player;

        if (clientPlayer != null)
            return clientPlayer.getPos();

        return super.getPos();
    }

    @Override
    protected void initDataTracker()
    {
        super.initDataTracker();

        this.dataTracker.set(PLAYER_MODEL_PARTS, (byte)127);
    }

    @Override
    public boolean isPartVisible(PlayerModelPart modelPart) {
        return true;
    }

    @Override
    public Identifier getSkinTexture()
    {
        if (morphTextureIdentifier != null) return morphTextureIdentifier;

        return super.getSkinTexture();
    }

    @Override
    public boolean hasSkinTexture() {
        return morphTextureIdentifier != null;
    }

    @Override
    public String getModel()
    {
        if (model != null) return model;

        return super.getModel();
    }

    @Override
    public boolean shouldRenderName() {
        return false;
    }
}

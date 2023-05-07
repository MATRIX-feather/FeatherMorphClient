package xiamomc.morph.client.graphics;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.EntityCache;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.MorphClientObject;
import xiamomc.morph.client.entities.MorphLocalPlayer;
import xiamomc.pluginbase.Annotations.Initializer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class EntityDisplay extends MorphClientObject
{
    private final String rawIdentifier;

    private final Boolean isPlayerItSelf;

    public EntityDisplay(String rawIdentifier)
    {
        this.rawIdentifier = rawIdentifier;
        this.isPlayerItSelf = rawIdentifier.equals(MorphClient.UNMORPH_STIRNG);

        this.displayName = Text.literal(rawIdentifier);
    }

    @Nullable
    private LivingEntity displayingEntity;

    @Nullable
    public LivingEntity getDisplayingEntity()
    {
        return displayingEntity;
    }

    private boolean isLiving = true;

    public boolean isLiving()
    {
        return isLiving;
    }

    private Text displayName;

    public Text getDisplayName()
    {
        return displayName;
    }

    private int entitySize;
    private int entityYOffset;

    private int getEntityYOffset(LivingEntity entity)
    {
        var type = Registries.ENTITY_TYPE.getId(entity.getType());

        return switch (type.toString())
        {
            case "minecraft:squid", "minecraft:glow_squid" -> -6;
            case "minecraft:ghast" -> -3;
            default -> 0;
        };
    }

    private int getEntitySize(LivingEntity entity)
    {
        var type = Registries.ENTITY_TYPE.getId(entity.getType());

        return switch (type.toString())
        {
            case "minecraft:ender_dragon" -> 3;
            case "minecraft:squid", "minecraft:glow_squid" -> 10;
            case "minecraft:magma_cube" ->
            {
                ((MagmaCubeEntity) entity).setSize(4, false);
                yield 8;
            }
            case "minecraft:player" -> {
                yield 8;
            }
            default ->
            {
                var size = (int) (15 / Math.max(entity.getHeight(), entity.getWidth()));
                size = Math.max(1, size);

                yield size;
            }
        };
    }

    public void resetEntity()
    {
        this.displayingEntity = null;
    }

    public void doSetupImmedately()
    {
        setupEntity();
    }

    private void setupEntity()
    {
        try
        {
            LivingEntity living = EntityCache.getEntity(rawIdentifier);
            isLiving = EntityCache.isLiving(rawIdentifier);

            if (living == null)
            {
                LivingEntity entity = null;

                if (isPlayerItSelf)
                {
                    entity = MinecraftClient.getInstance().player;
                    isLiving = true;
                }
                else if (rawIdentifier.startsWith("player:"))
                {
                    var nameSplited = rawIdentifier.split(":", 2);

                    if (nameSplited.length == 2)
                    {
                        entity = new MorphLocalPlayer(MinecraftClient.getInstance().world,
                                new GameProfile(UUID.randomUUID(), nameSplited[1]));
                    }
                }

                //没有和此ID匹配的实体
                if (entity == null)
                {
                    this.displayName = Text.literal(rawIdentifier);

                    if (onEntitySetup != null)
                        onEntitySetup.run();

                    loadingEntity.set(false);
                    return;
                }

                living = entity;
            }

            loadingEntity.set(false);
            allowRender = true;

            this.displayingEntity = living;
            this.displayName = displayingEntity.getDisplayName();

            entitySize = getEntitySize(displayingEntity);
            entityYOffset = getEntityYOffset(displayingEntity);

            if (displayingEntity.getType() == EntityType.MAGMA_CUBE)
                ((MagmaCubeEntity) living).setSize(4, false);

            if (onEntitySetup != null)
                onEntitySetup.run();
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private final AtomicBoolean loadingEntity = new AtomicBoolean(false);

    public int x;
    public int y;

    public Runnable onEntitySetup;

    private boolean allowRender;

    private void renderLoading(MatrixStack matrices)
    {
        var tex = new Identifier("morphclient", "textures/gui/loading.png");

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, tex);
        int offset = (int)plugin.getCurrentTick() / 4;

        DrawableHelper.drawTexture(matrices, x - 8, y - 16, 0, 16 * offset, 16, 16, 16, 128);
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY)
    {
        if (displayingEntity == null && isLiving)
        {
            if (!loadingEntity.get())
                CompletableFuture.runAsync(this::setupEntity);

            renderLoading(matrices);
            return;
        }

        if (!allowRender || !isLiving) return;

        try
        {
            if (displayingEntity == MinecraftClient.getInstance().player)
                PlayerRenderHelper.instance.skipRender = true;

            InventoryScreen.drawEntity(matrices, x, y + entityYOffset, entitySize, mouseX, mouseY, displayingEntity);

            PlayerRenderHelper.instance.skipRender = false;
        }
        catch (Throwable t)
        {
            allowRender = false;
            LoggerFactory.getLogger("morph").error(t.getMessage());
            t.printStackTrace();
        }
    }
}

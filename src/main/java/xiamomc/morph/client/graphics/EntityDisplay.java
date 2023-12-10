package xiamomc.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;
import xiamomc.morph.client.EntityCache;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.color.MaterialColors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityDisplay extends MDrawable
{
    private final String rawIdentifier;

    private final boolean isPlayerItSelf;

    private final boolean displayLoadingIfInvalid;

    /**
     * 此实体显示初始化的方式
     */
    public enum InitialSetupMethod
    {
        /**
         * 无初始化方式，实体会在第一次渲染时异步设置
         */
        NONE,

        /**
         * 异步设置实体
         */
        ASYNC,

        /**
         * 立即设置实体
         */
        SYNC
    }

    public EntityDisplay(String rawIdentifier, boolean displayLoadingIfNotValid, InitialSetupMethod initialSetupMethod)
    {
        this.rawIdentifier = rawIdentifier;
        this.isPlayerItSelf = rawIdentifier.equals(MorphClient.UNMORPH_STIRNG);

        this.displayName = Text.translatable("gui.morphclient.loading")
                .formatted(Formatting.ITALIC, Formatting.GRAY);

        this.displayLoadingIfInvalid = displayLoadingIfNotValid;

        loadingSpinner.setAnchor(Anchor.Centre);
        loadingSpinner.setParent(this);

        switch (initialSetupMethod)
        {
            case ASYNC -> CompletableFuture.runAsync(this::setupEntity);
            case SYNC -> this.setupEntity();
            case NONE -> { /* 交给load方法 */ }
        }
    }

    public EntityDisplay(String id)
    {
        this(id, false, InitialSetupMethod.NONE);
    }

    @Override
    public void invalidatePosition()
    {
        super.invalidatePosition();
        loadingSpinner.invalidatePosition();
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

    private final AtomicInteger entitySize = new AtomicInteger(1);
    private int entityYOffset;

    private int getEntityYOffset(LivingEntity entity)
    {
        var type = Registries.ENTITY_TYPE.getId(entity.getType());

        return switch (type.toString())
        {
            case "minecraft:ender_dragon" -> -1;
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
            case "minecraft:horse", "minecraft:player" -> 8;
            default ->
            {
                //15 / ...
                var size = (int) ((Math.min(this.getFinalHeight(), this.getFinalWidth()) * 0.8) / Math.max(entity.getHeight(), entity.getWidth()));
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
            loadingEntity.set(true);

            var entityCache = EntityCache.getGlobalCache();
            var living = entityCache.getEntity(rawIdentifier, null);
            isLiving = entityCache.isLiving(rawIdentifier);

            if (living == null)
            {
                LivingEntity entity = null;

                if (isPlayerItSelf)
                {
                    entity = MinecraftClient.getInstance().player;
                    isLiving = true;
                }

                //没有和此ID匹配的实体
                if (entity == null)
                {
                    this.displayName = Text.literal(rawIdentifier);

                    if (postEntitySetup != null)
                        postEntitySetup.run();

                    loadingEntity.set(false);
                    return;
                }

                living = entity;
            }

            loadingEntity.set(false);
            allowRender = true;

            this.displayingEntity = living;
            this.displayName = living.getDisplayName();

            if (RenderSystem.isOnRenderThread())
            {
                entitySize.set(getEntitySize(living));
                entityYOffset = getEntityYOffset(living);
            }
            else
            {
                LivingEntity finalLiving = living;
                this.addSchedule(() ->
                {
                    entitySize.set(getEntitySize(finalLiving));
                    entityYOffset = getEntityYOffset(finalLiving);
                });
            }

            if (postEntitySetup != null)
                postEntitySetup.run();
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private final AtomicBoolean loadingEntity = new AtomicBoolean(false);

    public Runnable postEntitySetup;

    private boolean allowRender;

    private final LoadingSpinner loadingSpinner = new LoadingSpinner();

    private void renderLoading(DrawContext context)
    {
        loadingSpinner.render(context, 0, 0, 0);
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        if (displayingEntity == null && isLiving)
        {
            if (!loadingEntity.get())
                CompletableFuture.runAsync(this::setupEntity);

            renderLoading(context);
            return;
        }

        if (!allowRender || !isLiving)
        {
            if (displayLoadingIfInvalid)
                renderLoading(context);

            return;
        }

        try
        {
            if (displayingEntity == MinecraftClient.getInstance().player)
                PlayerRenderHelper.instance.skipRender = true;

            //context.fill(0, 0, finalWidth, finalHeight, MaterialColors.Red500.getColor());

            var x1 = finalWidth / 2;
            var y2 = finalHeight;

            drawEntity(context,
                    x1, 0, x1, y2,
                    entitySize.get(), 0.0625f + entityYOffset, -mouseX, -mouseY, displayingEntity);

            PlayerRenderHelper.instance.skipRender = false;

            if (displayingEntity.isRemoved())
                resetEntity();
        }
        catch (Throwable t)
        {
            allowRender = false;
            LoggerFactory.getLogger("morph").error(t.getMessage());
            t.printStackTrace();
        }
    }

    /**
     * Copied from {@link InventoryScreen#drawEntity(DrawContext, int, int, int, int, int, float, float, float, LivingEntity)}
     * Because they introduced scissor that is not compatible with our gui impl.
     */
    public static void drawEntity(DrawContext context, int x1, int y1, int x2, int y2, int size, float f, float mouseX, float mouseY, LivingEntity entity) {
        float g = (float)(x1 + x2) / 2.0f;
        float h = (float)(y1 + y2) / 2.0f;
        //context.enableScissor(x1, y1, x2, y2);
        float i = (float)Math.atan((g - mouseX) / 40.0f);
        float j = (float)Math.atan((h - mouseY) / 40.0f);
        Quaternionf quaternionf = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf quaternionf2 = new Quaternionf().rotateX(j * 20.0f * ((float)Math.PI / 180));
        quaternionf.mul(quaternionf2);
        float k = entity.bodyYaw;
        float l = entity.getYaw();
        float m = entity.getPitch();
        float n = entity.prevHeadYaw;
        float o = entity.headYaw;
        entity.bodyYaw = 180.0f + i * 20.0f;
        entity.setYaw(180.0f + i * 40.0f);
        entity.setPitch(-j * 20.0f);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();
        Vector3f vector3f = new Vector3f(0.0f, entity.getHeight() / 2.0f + f, 0.0f);
        InventoryScreen.drawEntity(context, g, h, size, vector3f, quaternionf, quaternionf2, entity);
        entity.bodyYaw = k;
        entity.setYaw(l);
        entity.setPitch(m);
        entity.prevHeadYaw = n;
        entity.headYaw = o;
        //context.disableScissor();
    }
}

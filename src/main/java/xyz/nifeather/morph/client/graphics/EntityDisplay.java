package xyz.nifeather.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.client.EntityCache;
import xyz.nifeather.morph.client.FeatherMorphClient;

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

    public EntityDisplay(@NotNull String rawIdentifier, boolean displayLoadingIfNotValid, InitialSetupMethod initialSetupMethod)
    {
        this.rawIdentifier = rawIdentifier;
        this.isPlayerItSelf = rawIdentifier.equals(FeatherMorphClient.UNMORPH_STIRNG);

        this.displayName = Text.translatable("gui.morphclient.loading")
                .formatted(Formatting.ITALIC, Formatting.GRAY);

        this.displayLoadingIfInvalid = displayLoadingIfNotValid;

        loadingSpinner.setAnchor(Anchor.Centre);
        loadingSpinner.setParent(this);
        loadingSpinner.setRelativeSizeAxes(Axes.Both);
        loadingSpinner.setSize(new Vector2f(1, 1));

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

    private AtomicBoolean isLiving = new AtomicBoolean(true);

    public boolean isLiving()
    {
        return isLiving.get();
    }

    private Text displayName;

    public Text getDisplayName()
    {
        return displayName;
    }

    private final AtomicInteger initialEntitySize = new AtomicInteger(1);
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

    protected int getInitialEntitySize(LivingEntity entity)
    {
        var type = Registries.ENTITY_TYPE.getId(entity.getType());

        return switch (type.toString())
        {
            case "minecraft:ender_dragon" -> 3;
            case "minecraft:squid", "minecraft:glow_squid" -> 10;
            case "minecraft:horse", "minecraft:player" -> 8;
            default -> 1;
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
            isLiving.set(entityCache.isLiving(rawIdentifier));

            if (living == null)
            {
                LivingEntity entity = null;

                if (isPlayerItSelf)
                {
                    entity = MinecraftClient.getInstance().player;
                    isLiving.set(true);
                }

                //没有和此ID匹配的实体
                if (entity == null)
                {
                    Runnable complete = () ->
                    {
                        this.displayName = Text.literal(rawIdentifier);

                        if (postEntitySetup != null)
                            postEntitySetup.run();

                        loadingEntity.set(false);
                    };

                    if (RenderSystem.isOnRenderThread())
                        complete.run();
                    else
                        this.addSchedule(complete);

                    return;
                }

                living = entity;
            }

            LivingEntity finalLiving = living;
            Runnable onComplete = () ->
            {
                loadingEntity.set(false);

                allowRender = true;

                this.displayingEntity = finalLiving;
                this.displayName = finalLiving.getDisplayName();

                initialEntitySize.set(getInitialEntitySize(finalLiving));
                entityYOffset = getEntityYOffset(finalLiving);

                if (postEntitySetup != null)
                    postEntitySetup.run();
            };

            if (RenderSystem.isOnRenderThread())
                onComplete.run();
            else
                this.addSchedule(onComplete);
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

    protected float getRenderScale()
    {
        float scaledMaxEntityBorder = Math.max(displayingEntity.getWidth(), displayingEntity.getHeight()) * initialEntitySize.get();

        var scale = Math.round((Math.min(this.getRenderHeight(), this.getRenderWidth()) * 0.8f) / scaledMaxEntityBorder);
        scale = Math.max(1, scale);

        return scale;
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float delta)
    {
        if (displayingEntity == null && isLiving())
        {
            if (!loadingEntity.get())
                CompletableFuture.runAsync(this::setupEntity);

            renderLoading(context);
            return;
        }

        if (!allowRender || !isLiving())
        {
            if (displayLoadingIfInvalid)
                renderLoading(context);

            return;
        }

        try
        {
            if (displayingEntity.isRemoved())
            {
                resetEntity();
                return;
            }

            if (displayingEntity == MinecraftClient.getInstance().player)
                PlayerRenderHelper.instance().skipRender = true;

            //context.fill(0, 0, renderWidth, renderHeight, MaterialColors.Red500.getColor());

            var scale = 1;

            float scaledMaxEntityBorder = Math.max(displayingEntity.getWidth(), displayingEntity.getHeight()) * initialEntitySize.get();

            scale = Math.round((Math.min(this.getRenderHeight(), this.getRenderWidth()) * 0.8f) / scaledMaxEntityBorder);
            scale = Math.max(1, scale);

            drawEntity(context,
                    0, 0, renderWidth, renderHeight,
                    scale * initialEntitySize.get(), 0.0625f + entityYOffset, mouseX, mouseY, displayingEntity);

            PlayerRenderHelper.instance().skipRender = false;
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
    public void drawEntity(DrawContext context,
                                  int x1, int y1,
                                  int x2, int y2,
                                  int size, float f,
                                  float mouseX, float mouseY,
                                  LivingEntity entity)
    {
        float regionXCenter = (float)(x1 + x2) / 2.0f;
        float regionYCenter = (float)(y1 + y2) / 2.0f;

        //context.drawText(MinecraftClient.getInstance().textRenderer, Text.literal("mX %s, mY %s".formatted(mouseX, mouseY)), 0, 0, -1, true);

        float xAtan = (float) Math.atan((getScreenSpaceX() + regionXCenter - mouseX) / 40.0f);
        float yAtan = (float) Math.atan((getScreenSpaceY() + regionYCenter - mouseY) / 40.0f);

        Quaternionf quaternionf = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf quaternionf2 = new Quaternionf().rotateX(yAtan * 20.0f * ((float)Math.PI / 180));
        quaternionf.mul(quaternionf2);
        float k = entity.bodyYaw;
        float l = entity.getYaw();
        float m = entity.getPitch();
        float n = entity.prevHeadYaw;
        float o = entity.headYaw;
        entity.bodyYaw = 180.0f + xAtan * 20.0f;
        entity.setYaw(180.0f + xAtan * 40.0f);
        entity.setPitch(-yAtan * 20.0f);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();
        Vector3f vector3f = new Vector3f(0.0f, entity.getHeight() / 2.0f + f, 0.0f);

        InventoryScreen.drawEntity(context, regionXCenter, regionYCenter, size, vector3f, quaternionf, quaternionf2, entity);

        entity.bodyYaw = k;
        entity.setYaw(l);
        entity.setPitch(m);
        entity.prevHeadYaw = n;
        entity.headYaw = o;
        //context.disableScissor();
    }
}

package xiamomc.morph.client.graphics.toasts;

import me.shedaniel.math.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.DisguiseSyncer;
import xiamomc.morph.client.EntityCache;
import xiamomc.morph.client.MorphClientObject;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;

public class DisguiseEntryToast extends MorphClientObject implements Toast
{
    private final String rawIdentifier;

    private final boolean isGrant;

    public DisguiseEntryToast(String rawIdentifier, boolean isGrant)
    {
        if (isGrant) grantInstance = this;
        else lostInstance = this;

        this.rawIdentifier = rawIdentifier;
        this.isGrant = isGrant;
    }

    @Nullable
    private LivingEntity displayingEntity;

    private final Recorder<Integer> outlineWidth = Recorder.of(this.getWidth());

    @Initializer
    private void load()
    {
        this.displayingEntity = EntityCache.getEntity(this.rawIdentifier);

        if (displayingEntity != null)
        {
            this.entitySize = getEntitySize(displayingEntity);
            this.entityOffset = getEntityYOffset(displayingEntity);
        }

        visibility.onValueChanged((o, visible) ->
        {
            if (visible == Visibility.SHOW)
                Transformer.delay(200).then(() -> Transformer.transform(outlineWidth, 2, 600, Easing.OutQuint));
            else
                Transformer.transform(outlineWidth, this.getWidth(), 600, Easing.OutQuint);
        }, true);
    }

    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    private static DisguiseEntryToast grantInstance;
    private static DisguiseEntryToast lostInstance;

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

    private int getEntityYOffset(LivingEntity entity)
    {
        var type = Registries.ENTITY_TYPE.getId(entity.getType());

        return switch (type.toString())
        {
            case "minecraft:squid", "minecraft:glow_squid" -> -6;
            case "minecraft:ghast" -> -3;
            case "minecraft:horse" -> 2;
            default -> 0;
        };
    }

    private int entitySize;
    private int entityOffset;

    private final Bindable<Visibility> visibility = new Bindable<>(Visibility.HIDE);

    private static final int colorGrant = Color.ofRGBA(75, 103, 16, 255).getColor();
    private static final int colorLost = Color.ofRGBA(179, 39, 33, 255).getColor();

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime)
    {
        // Draw background
        DrawableHelper.fill(matrices, 0, 0, this.getWidth(), this.getHeight(), 0xFF333333);

        // Draw entity
        if (displayingEntity != null)
            InventoryScreen.drawEntity(matrices, 20, this.getHeight() / 2 + 7 + entityOffset, entitySize, -30, 0, displayingEntity);

        // Draw text
        var textStartX = this.getWidth() * 0.25F - 4;
        var textStartY = this.getHeight() / 2 - textRenderer.fontHeight;

        Text entityDisplayName = Text.literal(this.rawIdentifier);
        if (displayingEntity != null)
            entityDisplayName = displayingEntity.getName();

        textRenderer.draw(matrices, Text.translatable("text.morphclient.toast.disguise_%s".formatted(isGrant ? "grant" : "lost")), textStartX, textStartY - 1, 0xffffffff);
        textRenderer.draw(matrices, entityDisplayName, textStartX, textStartY + textRenderer.fontHeight + 1, 0xffffffff);

        // Draw outline
        var color = isGrant ? colorGrant : colorLost;

        // Left Line
        var lineWidth = outlineWidth.get();
        DrawableHelper.fill(matrices, 0, 0, lineWidth, this.getHeight(), color);

        /*

        var visibility = this.isGrant
                ? (grantInstance == this ? Visibility.SHOW : Visibility.HIDE)
                : (lostInstance == this ? Visibility.SHOW : Visibility.HIDE);

        */

        // Update visibility
        var visibility = (double)startTime >= 3000.0 * manager.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
        this.visibility.set(visibility);

        return visibility;
    }
}

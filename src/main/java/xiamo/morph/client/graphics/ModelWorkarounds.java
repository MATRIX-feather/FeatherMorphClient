package xiamo.morph.client.graphics;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.Vec3dUtils;

import java.util.List;
import java.util.Map;

public class ModelWorkarounds
{
    private static ModelWorkarounds instance;

    public static ModelWorkarounds getInstance()
    {
        if (instance == null) instance = new ModelWorkarounds();

        return instance;
    }

    private final Map<Identifier, ModelPartConsumer<ModelPart, ModelPart>> workarounds = new Object2ObjectOpenHashMap<>();

    private void addWorkaround(EntityType<?> modelType, ModelPartConsumer<ModelPart, ModelPart> consumer)
    {
        workarounds.put(EntityType.getId(modelType), consumer);
    }

    private void addWorkaround(List<EntityType<?>> types, ModelPartConsumer<ModelPart, ModelPart> consumer)
    {
        types.forEach(t -> addWorkaround(t, consumer));
    }

    public ModelWorkarounds()
    {
        initWorkarounds();
    }

    private interface ModelPartConsumer<L, R>
    {
        @NotNull
        WorkaroundMeta accept(L l, R r);
    }

    public void initWorkarounds()
    {
        LoggerFactory.getLogger("morph").info("Initializing arm render workarounds");
        workarounds.clear();

        //No-op
        addWorkaround(List.of(EntityType.WARDEN, EntityType.VILLAGER, EntityType.SNOW_GOLEM), (l, r) ->
                WorkaroundMeta.of(Vec3d.ZERO, Vec3dUtils.ONE()));

        addWorkaround(List.of(EntityType.HOGLIN, EntityType.ZOGLIN), (l, r) ->
                WorkaroundMeta.of(new Vec3d(0, -0.57f, 0.8f), Vec3dUtils.ONE()));

        addWorkaround(List.of(EntityType.ZOMBIE_HORSE, EntityType.SKELETON_HORSE, EntityType.HORSE), (l, r) ->
                WorkaroundMeta.of(new Vec3d(0, -0.45f, 1f), Vec3dUtils.ONE()));

        addWorkaround(EntityType.POLAR_BEAR, (l, r) ->
                WorkaroundMeta.of(new Vec3d(0, -0.57f, 0.65f), Vec3dUtils.ONE()));

        addWorkaround(EntityType.CREEPER, (l, r) ->
                WorkaroundMeta.of(new Vec3d(0, -0.57f, 0.5f), Vec3dUtils.ONE()));

        addWorkaround(EntityType.IRON_GOLEM, (l, r) ->
                new WorkaroundMeta(Vec3dUtils.of(0, -.2, 0), Vec3dUtils.of(.75)));

        addWorkaround(EntityType.ALLAY, (l, r) ->
        {
            l.roll = r.roll = 0;
            return new WorkaroundMeta(Vec3dUtils.of(0, .25, .1), Vec3dUtils.of(1.5));
        });

        /*
        addWorkaround(List.of(EntityType.SPIDER, EntityType.CAVE_SPIDER), (l, r) ->
        {
            r.yaw = 0f;
            r.roll = -0.f;

            return new WorkaroundMeta(Vec3dUtils.of(0, 1, .7), Vec3dUtils.ONE());
        });
        */

        addWorkaround(EntityType.BLAZE, (l, r) ->
                new WorkaroundMeta(Vec3dUtils.of(0, -0.1, 0.2), Vec3dUtils.ONE()));

        addWorkaround(EntityType.CAMEL, (l, r) -> WorkaroundMeta.of(new Vec3d(0, -0.6, 0.7), Vec3dUtils.ONE()));

        addWorkaround(EntityType.ENDER_DRAGON, (l, r) ->
        {
            //0.55f
            l.yaw = -0.6f;
            r.yaw = -l.yaw;

            return new WorkaroundMeta(Vec3dUtils.of(0, -3.2, 0), Vec3dUtils.of(.6));
        });
    }

    private WorkaroundMeta defaultMeta()
    {
        return new WorkaroundMeta(new Vec3d(0, -0.6f, 0.45f), Vec3dUtils.ONE());
    }

    /**
     * 通过传入的类型获取对应的{@link WorkaroundMeta}
     * @param entityType 实体类型
     * @param left 左手模型
     * @param right 右手模型
     * @return {@link WorkaroundMeta}
     */
    public WorkaroundMeta apply(EntityType<?> entityType, ModelPart left, ModelPart right)
    {
        var workaround = workarounds.get(EntityType.getId(entityType));

        return workaround == null ? defaultMeta() : workaround.accept(left, right);
    }

    public record WorkaroundMeta(Vec3d offset, Vec3d scale)
    {
        public WorkaroundMeta(@NotNull Vec3d offset, @NotNull Vec3d scale)
        {
            this.offset = offset;
            this.scale = scale;
        }

        public static WorkaroundMeta of(Vec3d offset, Vec3d scale)
        {
            return new WorkaroundMeta(offset, scale);
        }
    }
}

package xiamo.morph.client.graphics;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ModelWorkarounds
{
    private static final Map<Identifier, ModelPartConsumer<ModelPart, ModelPart>> workarounds = new Object2ObjectOpenHashMap<>();

    private static void addWorkaround(EntityType<?> modelType, ModelPartConsumer<ModelPart, ModelPart> consumer)
    {
        workarounds.put(EntityType.getId(modelType), consumer);
    }

    private static void addWorkaround(List<EntityType<?>> types, ModelPartConsumer<ModelPart, ModelPart> consumer)
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

    public static void initWorkarounds()
    {
        LoggerFactory.getLogger("morph").info("Initializing arm render workarounds");
        workarounds.clear();

        //No-op
        addWorkaround(List.of(EntityType.WARDEN, EntityType.VILLAGER, EntityType.SNOW_GOLEM), (l, r) ->
                WorkaroundMeta.newMeta());

        addWorkaround(List.of(EntityType.HOGLIN, EntityType.ZOGLIN), (l, r) ->
                WorkaroundMeta.of(new Vec3f(0, -0.57f, 0.8f), WorkaroundMeta.VECONE()));

        addWorkaround(List.of(EntityType.ZOMBIE_HORSE, EntityType.SKELETON_HORSE, EntityType.HORSE), (l, r) ->
                WorkaroundMeta.of(new Vec3f(0, -0.45f, 1f), WorkaroundMeta.VECONE()));

        addWorkaround(EntityType.POLAR_BEAR, (l, r) ->
                WorkaroundMeta.of(new Vec3f(0, -0.57f, 0.65f), WorkaroundMeta.VECONE()));

        addWorkaround(EntityType.CREEPER, (l, r) ->
                WorkaroundMeta.of(new Vec3f(0, -0.57f, 0.5f), WorkaroundMeta.VECONE()));

        addWorkaround(EntityType.IRON_GOLEM, (l, r) ->
        {
            var meta = WorkaroundMeta.newMeta();
            meta.offset.set(0, -0.2f, 0);
            meta.scale.set(.75f, .75f, .75f);

            return meta;
        });

        addWorkaround(EntityType.ALLAY, (l, r) ->
        {
            var meta = WorkaroundMeta.newMeta();

            meta.offset.set(0, 0.25f, 0.1f);
            meta.scale.set(1.5f, 1.5f, 1.5f);

            l.roll = r.roll = 0;
            return meta;
        });

        addWorkaround(EntityType.ENDER_DRAGON, (l, r) ->
        {
            var meta = WorkaroundMeta.newMeta();

            meta.offset.set(0, -3.2f, 0f);
            meta.scale.set(0.6f, 0.6f, 0.6f);

            //0.55f
            l.yaw = -0.6f;
            r.yaw = -l.yaw;

            return meta;
        });
    }

    /**
     * 通过传入的类型获取对应的{@link WorkaroundMeta}
     * @param entityType 实体类型
     * @param left 左手模型
     * @param right 右手模型
     * @return {@link WorkaroundMeta}
     */
    public static WorkaroundMeta apply(EntityType<?> entityType, ModelPart left, ModelPart right)
    {
        var workaround = workarounds.get(EntityType.getId(entityType));

        return workaround == null ? new WorkaroundMeta(new Vec3f(0, -0.6f, 0.45f), WorkaroundMeta.VECONE()) : workaround.accept(left, right);
    }

    public record WorkaroundMeta(Vec3f offset, Vec3f scale)
    {
        public static WorkaroundMeta of(Vec3f offset, Vec3f scale)
        {
            return new WorkaroundMeta(offset, scale);
        }

        public static Vec3f VECONE()
        {
            return new Vec3f(1, 1, 1);
        }

        public static WorkaroundMeta newMeta()
        {
            return new WorkaroundMeta(new Vec3f(0, 0, 0), VECONE());
        }
    }
}

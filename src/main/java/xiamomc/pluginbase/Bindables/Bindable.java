package xiamomc.pluginbase.Bindables;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.WeakReferenceList;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class Bindable<T> implements IBindable<T>
{
    private T value;

    public Bindable()
    {
    }

    public Bindable(T value)
    {
        this.value = value;
    }

    private final WeakReferenceList<Bindable<T>> binds = new WeakReferenceList<>();

    /**
     * 设置此Bindable的值
     *
     * @param val 新值
     */
    public void set(T val)
    {
        if (value == val) return;

        var oldVal = value;
        value = val;

        this.triggerValueChange(this, oldVal, val);
    }

    /**
     * 在不同的Bindable之间同步值
     *
     * @param source 同步来源
     * @param newVal 新值
     */
    private void syncValue(Bindable<T> source, T newVal)
    {
        if (source == this || value == newVal) return;

        var oldVal = this.value;
        this.value = newVal;

        triggerValueChange(source, oldVal, newVal);
    }

    private int triggers;

    /**
     * 触发一次变动时间
     *
     * @param source 触发来源
     * @param newVal 新值
     */
    private void triggerValueChange(Bindable<T> source, T oldVal, T newVal)
    {
        triggers++;

        if (triggers >= 3)
        {
            removeReleasedRefs();
            triggers = 0;
        }

        valueChangeConsumers.forEach(c -> c.accept(oldVal, newVal));

        binds.forEach(b ->
        {
            if (b == this || b == null) return;

            b.syncValue(source, newVal);
        });
    }

    public void triggerChange()
    {
        this.triggerValueChange(this, null, value);
    }

    @ApiStatus.Internal
    public void setInternal(Object val)
    {
        set((T) val);
    }

    /**
     * 获取此Bindable的值
     *
     * @return 此Bindable的值
     */
    public T get()
    {
        return value;
    }

    @Nullable
    private Bindable<T> bindTarget;

    private void removeReleasedRefs()
    {
        binds.removeNull();
    }

    public void dispose()
    {
        if (bindTarget != null)
        {
            bindTarget.binds.remove(this);
            bindTarget.valueChangeConsumers.remove(this);

            bindTarget = null;
        }

        this.binds.forEach(ref ->
        {
            ref.binds.remove(this);
        });
    }

    /**
     * 将此Bindable和另外一个Bindable绑定
     * @apiNote 目前一旦绑定将不能解绑
     *
     * @param other 目标Bindable
     */
    public void bindTo(Bindable<T> other)
    {
        if (other == null || other == this || binds.contains(other.weakRef)) return;

        if (bindTarget != null)
        {
            this.binds.remove(bindTarget.weakRef);
            bindTarget.binds.remove(weakRef);
        }

        //让other改变值时可以触发这里的triggerValueChange
        other.binds.add(weakRef);

        //让这里改变值时也可以触发other的triggerValueChange
        this.binds.add(other.weakRef);

        set(other.value);
        bindTarget = other;
    }

    public void unBindFromTarget()
    {
        if (bindTarget == null) return;

        bindTarget.binds.remove(this);
    }

    public void unBindBindings()
    {
        this.binds.forEach(b ->
                b.binds.remove(this));

        if (bindTarget != null)
            bindTarget.binds.remove(this);
    }

    private final WeakReference<Bindable<T>> weakRef = new WeakReference<>(this);

    public void bindTo(IBindable<T> other)
    {
        if (!(other instanceof Bindable<T> bindable))
            throw new IllegalArgumentException("指定的目标不是Bindable实例");

        this.bindTo(bindable);
    }

    private final List<BiConsumer<T, T>> valueChangeConsumers = new ObjectArrayList<>();

    /**
     * 添加此Bindable值改变时要做的事
     *
     * @param consumer {@link BiConsumer}，调用时第一个参数为旧值，第二个为新值
     */
    public void onValueChanged(BiConsumer<T, T> consumer)
    {
        onValueChanged(consumer, false);
    }

    /**
     * 添加此Bindable值改变时要做的事
     *
     * @param consumer {@link BiConsumer}
     * @param runOnce 是否要立即执行一次
     * @apiNote 立即执行时，旧值永远为null
     */
    public void onValueChanged(BiConsumer<T, T> consumer, boolean runOnce)
    {
        valueChangeConsumers.add(consumer);

        if (runOnce)
            consumer.accept(null, value);
    }

    @Override
    public String toString() {
        return "" + value;
    }
}

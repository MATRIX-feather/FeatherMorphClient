package xiamomc.morph.client.bindables;

import java.util.function.BiConsumer;

public interface IBindable<T>
{
    public T get();

    public void onValueChanged(BiConsumer<T, T> consumer, boolean runOnce);

    public void bindTo(IBindable<T> other);
}

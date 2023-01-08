package xiamomc.pluginbase.Bindables;

import java.util.List;
import java.util.function.BiConsumer;

public interface IBindableList<T> extends List<T>
{
    public void onListChanged(BiConsumer<List<T>, TriggerReason> consumer, boolean runOnce);

    public void onListChanged(BiConsumer<List<T>, TriggerReason> consumer);

    public void bindTo(IBindableList<T> other);
}
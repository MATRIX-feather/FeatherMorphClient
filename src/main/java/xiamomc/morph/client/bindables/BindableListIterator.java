package xiamomc.morph.client.bindables;

import java.util.ListIterator;

public class BindableListIterator<T> implements ListIterator<T> {
    public BindableListIterator(BindableList<T> bindable)
    {
        this.backendBindable = bindable;
        this.backendIterator = bindable.list.listIterator();
    }

    private final ListIterator<T> backendIterator;

    private final BindableList<T> backendBindable;

    @Override
    public boolean hasNext() {
        return backendIterator.hasNext();
    }

    @Override
    public T next()
    {
        var val = backendIterator.next();

        this.current = val;
        return val;
    }

    private T current;

    @Override
    public boolean hasPrevious() {
        return backendIterator.hasPrevious();
    }

    @Override
    public T previous()
    {
        var val = backendIterator.previous();

        this.current = val;
        return val;
    }

    @Override
    public int nextIndex() {
        return backendIterator.nextIndex();
    }

    @Override
    public int previousIndex() {
        return backendIterator.previousIndex();
    }

    @Override
    public void remove()
    {
        backendIterator.remove();
        backendBindable.triggerChange(current, TriggerReason.REMOVE);
    }

    @Override
    public void set(T t)
    {
        backendIterator.set(t);
        backendBindable.triggerChange(t, TriggerReason.ADD);
    }

    @Override
    public void add(T t)
    {
        backendIterator.add(t);
        backendBindable.triggerChange(t, TriggerReason.ADD);
    }
}

package xiamo.morph.client;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

public class WeakReferenceList<T> implements Iterable<T>
{
    private final ObjectArrayList<WeakReference<T>> list = new ObjectArrayList<>();

    public void removeNull()
    {
        list.removeIf(ref -> ref.get() == null);
    }

    @Nullable
    public T get(int i)
    {
        var ref = list.get(i);

        return ref == null ? null : ref.get();
    }

    public T set(int i, T t)
    {
        var ref = new WeakReference<>(t);

        list.set(i, ref);

        return t;
    }

    public void add(int i, T t)
    {
        var ref = new WeakReference<>(t);

        list.add(i, ref);
    }

    public T remove(int i)
    {
        var ref = list.remove(i);

        return ref.get();
    }

    public int indexOf(Object o)
    {
        if (o == null) return -1;

        var ref = list.stream()
                .filter(r -> o.equals(r.get())).findFirst().orElse(null);

        if (ref != null) return list.indexOf(ref);

        return -1;
    }

    public int lastIndexOf(Object o)
    {
        if (o == null) return -1;

        var ref = list.stream()
                .filter(r -> o.equals(r.get())).findFirst().orElse(null);

        if (ref != null) return list.lastIndexOf(ref);

        return -1;
    }

    public int size()
    {
        return list.size();
    }

    public boolean isEmpty()
    {
        return list.isEmpty();
    }

    public boolean contains(WeakReference<T> weakRef)
    {
        if (weakRef == null) return false;

        return list.contains(weakRef);
    }

    public boolean contains(T t)
    {
        if (t == null) return false;

        return list.stream().anyMatch(ref -> t.equals(ref.get()));
    }

    public boolean add(WeakReference<T> weakRef)
    {
        if (weakRef == null) return false;

        return list.add(weakRef);
    }

    public boolean remove(WeakReference<T> weakRef)
    {
        if (weakRef == null) return false;

        return list.removeIf(ref -> weakRef == ref);
    }

    public boolean remove(T t)
    {
        if (t == null) return false;

        return list.removeIf(ref -> t.equals(ref.get()));
    }

    public boolean containsAll(@NotNull Collection<T> collection)
    {
        boolean success = true;

        for (var o : collection)
        {
            if (!this.contains(o))
            {
                success = false;
                break;
            }
        }

        return success;
    }

    public boolean addAll(@NotNull Collection<WeakReference<T>> collection)
    {
        var changed = false;

        for (var o : collection)
        {
            changed = changed || add(o);
        }

        return changed;
    }

    public boolean removeAll(@NotNull Collection<T> collection)
    {
        var changed = false;

        for (var o : collection)
        {
            changed = changed || remove(o);
        }

        return changed;
    }

    public void clear()
    {
        list.clear();
    }

    @NotNull
    @Override
    public Iterator<T> iterator()
    {
        return new WeakRefIterator<>(this);
    }

    private static class WeakRefIterator<T> implements ListIterator<T>
    {
        private final WeakReferenceList<T> refList;
        private final ListIterator<WeakReference<T>> backendIterator;

        private WeakRefIterator(WeakReferenceList<T> weakList)
        {
            this.refList = weakList;

            this.backendIterator = weakList.list.listIterator();
        }

        @Override
        public boolean hasNext()
        {
            return backendIterator.hasNext();
        }

        @Override
        public T next()
        {
            return backendIterator.next().get();
        }

        @Override
        public boolean hasPrevious()
        {
            return backendIterator.hasPrevious();
        }

        @Override
        public T previous()
        {
            return backendIterator.previous().get();
        }

        @Override
        public int nextIndex()
        {
            return backendIterator.nextIndex();
        }

        @Override
        public int previousIndex()
        {
            return backendIterator.previousIndex();
        }

        @Override
        public void remove()
        {
            backendIterator.remove();
        }

        @Override
        public void set(T t)
        {
            backendIterator.set(new WeakReference<>(t));
        }

        @Override
        public void add(T t)
        {
            backendIterator.add(new WeakReference<>(t));
        }
    }
}

package xiamomc.morph.client.graphics.transforms;

public class Recorder<T>
{
    private T val;

    public T get()
    {
        return val;
    }

    public void set(T val)
    {
        this.val = val;
    }

    public Recorder(T val) {
        this.val = val;
    }

    public static <TValue> Recorder<TValue> of(TValue value) {
        return new Recorder<>(value);
    }
}

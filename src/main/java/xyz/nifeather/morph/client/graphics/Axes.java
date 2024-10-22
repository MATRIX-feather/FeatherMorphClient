package xyz.nifeather.morph.client.graphics;

public enum Axes
{
    None(false, false),
    X(true, false),
    Y(false, true),
    Both(true, true);

    public final boolean modX;
    public final boolean modY;

    Axes(boolean modX, boolean modY)
    {
        this.modX = modX;
        this.modY = modY;
    }
}

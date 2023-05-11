package xiamomc.morph.client.graphics;

import java.util.Objects;

public class Margin
{
    public final float left;
    public final float right;
    public final float bottom;
    public final float top;

    public Margin()
    {
        this(0);
    }

    public Margin(float rad)
    {
        this(rad, rad, rad, rad);
    }

    public Margin(float l, float r)
    {
        this(l, r, 0, 0);
    }

    public Margin(float left, float right, float top, float bottom)
    {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public boolean equals(Margin other)
    {
        if (other == null) return false;
        return left == other.left && right == other.right && top == other.top && bottom == other.bottom;
    }
}

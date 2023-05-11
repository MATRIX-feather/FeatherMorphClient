package xiamomc.morph.client.graphics;

import static xiamomc.morph.client.graphics.PosMask.*;

public enum Anchor
{
    TopLeft(x1 | y1),
    TopCentre(x2 | y1),
    TopRight(x3 | y1),

    CentreLeft(x1 | y2),
    Centre(x2 | y2),
    CentreRight(x3 | y3),

    BottomLeft(x1 | y3),
    BottomCentre(x2 | y3),
    BottomRight(x3 | y3);

    public final int posMask;

    Anchor(int posMask)
    {
        this.posMask = posMask;
    }
}

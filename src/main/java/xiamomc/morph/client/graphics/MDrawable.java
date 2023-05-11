package xiamomc.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.client.MorphClientObject;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transform;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;

import java.util.concurrent.atomic.AtomicBoolean;

public class MDrawable extends MorphClientObject implements Drawable
{
    @NotNull
    protected Anchor anchor = Anchor.TopLeft;

    @NotNull
    public Anchor getAnchor()
    {
        return anchor;
    }

    public void setAnchor(@NotNull Anchor anchor)
    {
        if (anchor == this.anchor) return;

        this.anchor = anchor;
        invalidatePosition();
    }

    //region Position validation

    private final AtomicBoolean posValid = new AtomicBoolean(false);

    protected void invalidatePosition()
    {
        posValid.set(false);
    }

    protected boolean posValid()
    {
        return posValid.get();
    }

    protected void updatePosition()
    {
        float screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        float screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        var screenCentre = new Vector2f(screenWidth / 2, screenHeight / 2);

        float xOffset = x;
        float yOffset = y;

        var maskX = (anchor.posMask << 4) >> 4;
        if ((maskX & PosMask.x1) == PosMask.x1)
            xOffset += margin.left - margin.right;
        else if ((maskX & PosMask.x2) == PosMask.x2)
            xOffset += margin.left - margin.right + (screenCentre.getX() - this.width / 2);
        else if ((maskX & PosMask.x3) == PosMask.x3)
            xOffset += margin.left - margin.right + (screenWidth - this.width);

        var maskY = (anchor.posMask >> 4) << 4;
        if ((maskY & PosMask.y1) == PosMask.y1)
            yOffset += margin.top - margin.bottom;
        else if ((maskY & PosMask.y2) == PosMask.y2)
            yOffset += margin.top - margin.bottom + (screenCentre.getY() - this.height / 2);
        else if ((maskY & PosMask.y3) == PosMask.y3)
            yOffset += margin.top - margin.bottom + (screenHeight - this.height);

        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    private float xOffset;
    private float yOffset;

    //endregion Position validation

    //region W/H

    protected float width;
    protected float height;

    public float getWidth()
    {
        return width;
    }

    public void setWidth(float w)
    {
        if (width == w) return;

        this.width = w;
        invalidatePosition();
    }

    public float getHeight()
    {
        return height;
    }

    public void setHeight(float h)
    {
        if (height == h) return;

        this.height = h;
        invalidatePosition();
    }

    //endregion W/H

    //region X/Y

    private int x;
    private int y;

    public int getX()
    {
        return x;
    }

    public void setX(int newX)
    {
        if (x == newX) return;

        this.x = newX;
        invalidatePosition();
    }

    public int getY()
    {
        return y;
    }

    public void setY(int newY)
    {
        if (y == newY) return;

        this.y = newY;
        invalidatePosition();
    }

    //endregion X/Y

    //region MarginPadding

    @NotNull
    private Margin margin = new Margin();

    public Margin getMargin()
    {
        return margin;
    }

    public void setMargin(@NotNull Margin margin)
    {
        if (this.margin.equals(margin)) return;

        this.margin = margin;
        invalidatePosition();
    }

    //endregion MarginPadding

    protected void onRender(MatrixStack matrixStack, int mouseX, int mouseY, float delta)
    {
    }

    @Override
    public final void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        matrices.push();

        try
        {
            if (!posValid()) updatePosition();
            matrices.translate(xOffset, yOffset, 0);

            this.onRender(matrices, mouseX, mouseY, delta);
        }
        finally
        {
            matrices.pop();
        }
    }

    //region Transforms

    private Recorder<Integer> xRec;

    public Transform<Integer> moveToX(int x, long duration, Easing easing)
    {
        if (xRec == null)
        {
            xRec = new Recorder<>(this.x);
            xRec.onUpdate = this::setX;
        }

        return Transformer.transform(xRec, x, duration, easing);
    }

    private Recorder<Integer> yRec;

    public Transform<Integer> moveToY(int newY, long duration, Easing easing)
    {
        if (yRec == null)
        {
            yRec = new Recorder<>(this.y);
            yRec.onUpdate = this::setY;
        }

        return Transformer.transform(yRec, newY, duration, easing);
    }

    private Recorder<Float> hRec;

    public Transform<Float> resizeHeightTo(float newH, long duration, Easing easing)
    {
        if (hRec == null)
        {
            hRec = new Recorder<>(height);
            hRec.onUpdate = this::setHeight;
        }

        return Transformer.transform(hRec, newH, duration, easing);
    }

    private Recorder<Float> wRec;

    public Transform<Float> resizeWidthTo(float newW, long duration, Easing easing)
    {
        if (wRec == null)
        {
            wRec = new Recorder<>(width);
            wRec.onUpdate = this::setWidth;
        }

        return Transformer.transform(wRec, newW, duration, easing);
    }

    public Transform<Float> resizeTo(Vector2f wH, long duration, Easing easing)
    {
        this.resizeHeightTo(wH.getX(), duration, easing);

        return this.resizeWidthTo(wH.getY(), duration, easing);
    }

    //endregion Transforms
}


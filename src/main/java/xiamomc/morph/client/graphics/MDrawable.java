package xiamomc.morph.client.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.MorphClientObject;
import xiamomc.morph.client.graphics.color.MaterialColors;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transform;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;

import java.util.concurrent.atomic.AtomicBoolean;

public class MDrawable extends MorphClientObject implements Drawable, Element
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

    protected MDrawable parent;

    public void setParent(MDrawable parent)
    {
        if (this.parent == parent) return;

        if (this.parent != null)
            throw new RuntimeException("A drawable may not have multiple parents.");

        this.parent = parent;
        invalidatePosition();
    }

    public MDrawable getParent()
    {
        return parent;
    }

    //region Position validation

    private final AtomicBoolean posValid = new AtomicBoolean(false);

    @ApiStatus.Internal
    public void invalidatePosition()
    {
        posValid.set(false);
    }

    protected boolean posValid()
    {
        return posValid.get();
    }

    protected void updatePosition()
    {
        if (parent != null)
        {
            //获取用于遮罩的父级屏幕空间位置
            this.applyParentScreenSpaceY(parent.getScreenSpaceY());
            this.applyParentScreenSpaceX(parent.getScreenSpaceX());

            //应用父级Padding到可用空间中
            var ppadding = parent.getPadding();
            var rectW = parent.getWidth() - ppadding.left - ppadding.right;
            var rectH = parent.getHeight() - ppadding.top - ppadding.bottom;
            this.applyParentRect(new ScreenRect(0, 0, (int)rectW, (int)rectH));
        }

        float rectWidth = parentRect == null ? MinecraftClient.getInstance().getWindow().getScaledWidth() : parentRect.width();
        float rectHeight = parentRect == null ? MinecraftClient.getInstance().getWindow().getScaledHeight() : parentRect.height();

        var rectCentre = new Vector2f(rectWidth / 2, rectHeight / 2);

        float xDrawingOffset = x;
        float yDrawingOffset = y;

        // 坐标原点：左上角
        // 居中时，通过左侧Margin减去右侧Margin来取得此Drawable的X位移
        // 对父级Padding同理
        //
        // x1 左对齐：左侧Margin + 父级左侧Padding
        // x2 横轴居中：(左侧Margin - 右侧Margin) + 父级横轴空间 - (宽度 /  2) + (父级左侧Padding - 父级右侧Padding)
        // x3 右对齐：-右侧Margin + (父级横轴空间 - 宽度) - 父级右侧Padding
        var maskX = (anchor.posMask << 4) >> 4;
        if ((maskX & PosMask.x1) == PosMask.x1)
            xDrawingOffset += margin.left + (parent == null ? 0 : parent.padding.left);
        else if ((maskX & PosMask.x2) == PosMask.x2)
            xDrawingOffset += margin.getCentreOffsetX() + (rectCentre.getX() - this.width / 2) + (parent == null ? 0 : parent.padding.getCentreOffsetX());
        else if ((maskX & PosMask.x3) == PosMask.x3)
            xDrawingOffset += -margin.right + (rectWidth - this.width) - (parent == null ? 0 : parent.padding.right);

        // 坐标原点：左上角
        // 居中时，通过上方Margin减去下方Margin来取得此Drawable的X位移
        // 对父级Padding同理
        //
        // y1 向上对齐：上方Margin + 父级上方Padding
        // y2 纵轴居中：(上方Margin - 下方Margin) + [父级纵轴空间 - (高度 /  2)] + (父级上方Padding - 父级下方Padding)
        // y3 向下对齐：-下方Margin + (父级纵轴空间 - 高度) - 父级下方Padding
        var maskY = (anchor.posMask >> 4) << 4;
        if ((maskY & PosMask.y1) == PosMask.y1)
            yDrawingOffset += margin.top + (parent == null ? 0 : parent.padding.top);
        else if ((maskY & PosMask.y2) == PosMask.y2)
            yDrawingOffset += margin.getCentreOffset() + (rectCentre.getY() - this.height / 2) + (parent == null ? 0 : parent.padding.getCentreOffset());
        else if ((maskY & PosMask.y3) == PosMask.y3)
            yDrawingOffset += - margin.bottom + (rectHeight - this.height) - (parent == null ? 0 : parent.padding.bottom);

        this.xOffset = xDrawingOffset;
        this.yOffset = yDrawingOffset;

        this.screenSpaceX = xDrawingOffset + parentScreenSpaceX;
        this.screenSpaceY = yDrawingOffset + parentScreenSpaceY;

        posValid.set(true);
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

    public void setSize(Vector2f vector2f)
    {
        this.setWidth(vector2f.getX());
        this.setHeight(vector2f.getY());
    }

    //endregion W/H

    //region X/Y

    private int x;
    private int y;

    /**
     * 此Drawable在屏幕空间上的X值
     */
    private float screenSpaceX;

    /**
     * 此Drawable在屏幕空间上的Y值
     */
    private float screenSpaceY;

    public float getScreenSpaceX()
    {
        return screenSpaceX;
    }

    public float getScreenSpaceY()
    {
        return screenSpaceY;
    }

    /**
     * 父级Drawable在屏幕空间上的X值，用于Masking
     */
    private float parentScreenSpaceX;

    /**
     * 父级Drawable在屏幕空间上的Y值，用于Masking
     */
    private float parentScreenSpaceY;

    public void applyParentScreenSpaceX(float parentX)
    {
        if (this.parentScreenSpaceX == parentX) return;
        this.parentScreenSpaceX = parentX;

        invalidatePosition();
    }

    public void applyParentScreenSpaceY(float parentY)
    {
        if (this.parentScreenSpaceY == parentY) return;
        this.parentScreenSpaceY = parentY;

        invalidatePosition();
    }

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

    @Nullable
    private ScreenRect parentRect = null;

    @Nullable
    public ScreenRect getParentRect()
    {
        return parentRect;
    }

    /**
     * 设置父级Drawable在其相对位置上的宽高
     */
    public void applyParentRect(ScreenRect rect)
    {
        if (this.parentRect != null && this.parentRect.equals(rect))
            return;

        this.parentRect = rect;
        invalidatePosition();
    }

    //region MarginPadding

    @NotNull
    protected Margin padding = new Margin();

    @NotNull
    public Margin getPadding()
    {
        return padding;
    }

    public void setPadding(Margin padding)
    {
        padding = padding == null ? new Margin() : padding;
        this.padding = padding;
    }

    @NotNull
    private Margin margin = new Margin();

    @NotNull
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

    private boolean masking = false;
    public boolean masking()
    {
        return masking;
    }

    public void setMasking(boolean masking)
    {
        this.masking = masking;
    }

    protected void onRender(MatrixStack matrixStack, int mouseX, int mouseY, float delta)
    {
    }

    private boolean hovered;

    @Override
    public final void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        matrices.push();

        this.hovered = mouseX < this.screenSpaceX + width && mouseX > this.screenSpaceX
                && mouseY < this.screenSpaceY + height && mouseY > this.screenSpaceY;

        try
        {
            if (!posValid()) updatePosition();

            // Render parent rect
            //if (parentRect != null)
            //{
            //    DrawableHelper.fill(matrices, parentRect.getLeft(), parentRect.getTop(), parentRect.width(), parentRect.height(), MaterialColors.Orange500.getColor());
            //}

            matrices.translate(xOffset, yOffset, 1);

            // Render Self rect
            //DrawableHelper.fill(matrices, 0, 0, (int)width, (int)height, MaterialColors.Cyan500.getColor());

            if (masking)
            {
                var sX = (int)getScreenSpaceX();
                var sY = (int)getScreenSpaceY();

                //MinecraftClient.getInstance().textRenderer.draw(matrices, "sX: %s, sY: %s, W: %s, H: %s".formatted(sX, sY, width, height), 0, 0, 0xffffffff);

                DrawableHelper.enableScissor(sX, sY, (int)width + sX, (int)height + sY);
            }

            this.onRender(matrices, mouseX, mouseY, delta);
        }
        finally
        {
            matrices.pop();

            if (masking)
                DrawableHelper.disableScissor();
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

    public void dispose()
    {
    }

    //region Element

    private boolean focused;

    @Override
    public void setFocused(boolean focused)
    {
        this.focused = focused;
    }

    @Override
    public boolean isFocused()
    {
        return focused;
    }

    //endregion Element
}


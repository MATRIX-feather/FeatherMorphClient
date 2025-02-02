package xyz.nifeather.morph.client.graphics.toasts;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Text;
import org.joml.Vector3f;
import xyz.nifeather.morph.client.graphics.Anchor;
import xyz.nifeather.morph.client.graphics.EntityDisplay;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;
import xiamomc.pluginbase.Annotations.Initializer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DisguiseEntryToast extends LinedToast
{
    private final String rawIdentifier;

    private final boolean isGrant;

    public static final ConcurrentLinkedQueue<DisguiseEntryToast> instances = new ConcurrentLinkedQueue<>();

    public static void invalidateAll()
    {
        instances.forEach(DisguiseEntryToast::invalidate);
    }

    private final AtomicBoolean isValid = new AtomicBoolean(true);

    public void invalidate()
    {
        isValid.set(false);
        instances.remove(this);
    }

    public DisguiseEntryToast(String rawIdentifier, boolean isGrant)
    {
        this.rawIdentifier = rawIdentifier;
        this.isGrant = isGrant;

        this.entityDisplay = new ToastEntityDisplay(rawIdentifier, true, EntityDisplay.InitialSetupMethod.NONE);
        entityDisplay.setX(512);
        entityDisplay.setSize(new Vector2f(26, 20));
        entityDisplay.setMasking(true);

        entityDisplay.postEntitySetup = () -> setDescription(entityDisplay.getDisplayName());

        instances.add(this);

        drawAlpha.set(0f);
    }

    @Override
    protected boolean fadeInOnEnter()
    {
        return true;
    }

    @Initializer
    private void load()
    {
        var x = 4;
        var y = 0;

        switch (rawIdentifier)
        {
            case "minecraft:horse" -> {
                x -= 1;
                y += 2;
            }
            case "minecraft:axolotl" -> x -= 1;
            case "minecraft:armor_stand" -> y -= 1;
        }

        entityDisplay.setX(x);
        entityDisplay.setY(y);
        entityDisplay.setAnchor(Anchor.CentreLeft);
        entityDisplay.setParentScreenSpace(new ScreenRect(0, 0, this.getWidth(), this.getHeight()));

        setTitle(Text.translatable("text.morphclient.toast.disguise_%s".formatted(isGrant ? "grant" : "lost")));
        this.setLineColor(isGrant ? MaterialColors.Green500 : MaterialColors.Amber500);

        visibility.onValueChanged((o, n) ->
        {
            var isHide = n == Visibility.HIDE;
            if (isHide)
                instances.remove(this);
        }, true);
    }


    private final EntityDisplay entityDisplay;

    @Override
    protected void postBackgroundDrawing(DrawContext context, long startTime)
    {
        drawEntity = this.drawAlpha.get() > 0.95f;

        if (!drawEntity) return;

        var matrices = context.getMatrices();
        super.postBackgroundDrawing(context, startTime);

        // Push a new entry to allow us to do some tricks
        matrices.push();

        // Draw entity
        // Make entity display more pixel-perfect
        matrices.translate(0, 0.5, 0);
        var pos = matrices.peek().getPositionMatrix().getTranslation(new Vector3f(0, 0, 0));

        var mouseX = -50;
        if (rawIdentifier.equals("minecraft:axolotl"))
            mouseX = -100;

        entityDisplay.setParentScreenSpaceX(pos.x);
        entityDisplay.setParentScreenSpaceY(pos.y);
        entityDisplay.render(context, mouseX, -10, 0);

        // Pop back
        matrices.pop();
    }

    private boolean drawEntity = true;

    @Override
    public void update(ToastManager manager, long startTime)
    {
        super.update(manager, startTime);

        this.visibility.set(isValid.get() ? this.visibility.get() : Visibility.HIDE);
    }

    private static final class ToastEntityDisplay extends EntityDisplay
    {
        public ToastEntityDisplay(String rawIdentifier, boolean displayLoadingIfNotValid, InitialSetupMethod initialSetupMethod)
        {
            super(rawIdentifier, displayLoadingIfNotValid, initialSetupMethod);
        }

        public ToastEntityDisplay(String id)
        {
            super(id);
        }

        // workaround: MDrawable中如果此Drawable没有父级，那么会自动通过窗口确定位置
        //             然后无视之前设定的ParentScreenSpace
        @Override
        protected void updateParentScreenSpace()
        {
        }
    }
}

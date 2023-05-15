package xiamomc.morph.client.graphics.toasts;

import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Text;
import org.joml.Vector3f;
import xiamomc.morph.client.graphics.Anchor;
import xiamomc.morph.client.graphics.EntityDisplay;
import xiamomc.morph.client.graphics.color.MaterialColors;
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

        this.entityDisplay = new EntityDisplay(rawIdentifier, true);
        entityDisplay.setX(512);
        entityDisplay.setSize(new Vector2f(32, 16));

        entityDisplay.postEntitySetup = () -> setDescription(entityDisplay.getDisplayName());

        instances.add(this);

        visibility.onValueChanged((o, n) ->
        {
            if (n == Visibility.HIDE) instances.remove(this);
        });
    }

    @Initializer
    private void load()
    {
        var x = 5;
        var y = 0;

        if (rawIdentifier.equals("minecraft:horse"))
        {
            x -= 1;
            y += 2;
        }
        else if (rawIdentifier.equals("minecraft:axolotl"))
        {
            x += 1;
        }

        entityDisplay.setX(x);
        entityDisplay.setY(y);
        entityDisplay.setAnchor(Anchor.CentreLeft);
        entityDisplay.applyParentRect(new ScreenRect(0, 0, this.getWidth(), this.getHeight()));

        setTitle(Text.translatable("text.morphclient.toast.disguise_%s".formatted(isGrant ? "grant" : "lost")));
        this.setLineColor(isGrant ? MaterialColors.Green500 : MaterialColors.Amber500);
    }


    private final EntityDisplay entityDisplay;

    @Override
    protected void postBackgroundDrawing(MatrixStack matrices, ToastManager manager, long startTime)
    {
        super.postBackgroundDrawing(matrices, manager, startTime);

        // Push a new entry to allow us to do some tricks
        matrices.push();

        // Draw entity
        // Make entity display more pixel-perfect
        matrices.translate(0, 0.5, 0);
        var pos = matrices.peek().getPositionMatrix().getTranslation(new Vector3f(0, 0, 0));
        entityDisplay.applyParentScreenSpaceX(pos.x);
        entityDisplay.applyParentScreenSpaceY(pos.y);
        entityDisplay.render(matrices, -30, 0, 0);

        // Pop back
        matrices.pop();
    }

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime)
    {
        var result = super.draw(matrices, manager, startTime);
        result = isValid.get() ? result : Visibility.HIDE;

        this.visibility.set(result);
        return result;
    }
}

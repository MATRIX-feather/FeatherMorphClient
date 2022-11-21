package xiamo.morph.client.screens.disguise;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.MorphClient;
import xiamo.morph.client.bindables.Bindable;
import xiamo.morph.client.graphics.DrawableText;

public class DisguiseScreen extends Screen
{
    public DisguiseScreen()
    {
        super(Text.literal("选择界面"));

        morphClient = MorphClient.getInstance();

        morphClient.onMorphGrant(c ->
        {
            c.forEach(s ->
            {
                list.children().add(new StringWidget(s));
            });
        });

        morphClient.onMorphRevoke(c ->
        {
            c.forEach(s ->
            {
                list.children().removeIf(w -> w.getIdentifier().equals(s));
            });
        });

        morphClient.getAvaliableMorphs().forEach(s -> list.children().add(new StringWidget(s)));
    }

    private final Bindable<String> selectedIdentifier = new Bindable<>();

    private final MorphClient morphClient;

    private final IdentifierDrawableList list = new IdentifierDrawableList(client, 200, 0, 20, 0, 22);

    @Override
    public void close()
    {
        super.close();
        selectedIdentifier.set(null);
    }

    @Override
    protected void init()
    {
        super.init();
        assert this.client != null;

        selectedIdentifier.bindTo(MorphClient.selectedIdentifier);

        //列表
        var height = (int) (this.height * 0.8f);
        list.setHeight(height);
        list.setBottom(height);

        this.addDrawableChild(list);

        //侧边显示
        DrawableText drawableText;
        this.addDrawable(drawableText = new DrawableText());

        drawableText.setWidth(200);
        drawableText.setHeight(20);

        drawableText.setScreenY(this.height / 2);
        drawableText.setScreenX(200 + (int) ((this.width - 200) * 0.5f));

        selectedIdentifier.onValueChanged((o, n) ->
        {
            drawableText.setText(n == null ? "（自动伪装）" : "'" + n + "'");
        }, true);

        //按钮
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 5, this.height - 29, 150, 20, Text.literal("关闭"), (button) ->
        {
            this.client.setScreen(null);
        }));

        //width - 75: 居中
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 75 - 75 - 5, this.height - 29, 150, 20, Text.literal("伪装"), (button) ->
        {
            morphClient.sendMorphCommand(this.selectedIdentifier.get());
            this.client.setScreen(null);
        }));
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(MatrixStack matrices, int vOffset)
    {
        super.renderBackground(matrices, vOffset);
    }
}

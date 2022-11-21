package xiamo.morph.client.screens.disguise;

import com.ibm.icu.impl.ICUResourceBundleReader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
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
            c.forEach(s -> list.children().add(new StringWidget(s)));
        });

        morphClient.onMorphRevoke(c ->
        {
            c.forEach(s -> list.children().removeIf(w -> w.getIdentifier().equals(s)));
        });

        list.children().add(new StringWidget("morph:unmorph"));

        morphClient.getAvaliableMorphs().forEach(s -> list.children().add(new StringWidget(s)));

        selectedIdentifier.bindTo(MorphClient.selectedIdentifier);
        selectedIdentifier.set(MorphClient.currentIdentifier.get());

        MorphClient.currentIdentifier.onValueChanged((o, n) ->
        {
            selectedIdentifierText.setText("当前伪装：" + (n == null ? "暂无" : n));
        }, true);

        titleText.setWidth(200);
        titleText.setHeight(20);
    }

    private final Bindable<String> selectedIdentifier = new Bindable<>();

    private final MorphClient morphClient;

    private final IdentifierDrawableList list = new IdentifierDrawableList(client, 200, 0, 20, 0, 22);
    private final DrawableText titleText = new DrawableText("选择伪装");
    private final DrawableText selectedIdentifierText = new DrawableText();

    @Override
    public void close()
    {
        super.close();
    }

    @Override
    protected void init()
    {
        int fontMargin = 4;

        super.init();
        assert this.client != null;

        //列表
        list.updateSize(width, this.height, textRenderer.fontHeight * 2 + fontMargin * 2, this.height - 40);

        this.addDrawableChild(list);

        //侧边显示
        this.addDrawable(titleText);
        this.addDrawable(selectedIdentifierText);

        //titleText.setScreenY(this.height / 2);
        titleText.setScreenX(30);
        titleText.setScreenY(fontMargin);
        selectedIdentifierText.setScreenX(30);
        selectedIdentifierText.setScreenY(fontMargin + 2 + textRenderer.fontHeight);

        //按钮
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 5, this.height - 29, 150, 20, Text.literal("关闭"), (button) ->
        {
            this.close();
        }));

        //width - 75: 居中
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 75 - 75 - 5, this.height - 29, 150, 20, Text.literal("伪装"), (button) ->
        {
            morphClient.sendMorphCommand(this.selectedIdentifier.get());
            this.close();
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

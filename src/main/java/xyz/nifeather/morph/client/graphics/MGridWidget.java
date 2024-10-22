package xyz.nifeather.morph.client.graphics;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.GridWidget;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MGridWidget extends GridWidget implements IMDrawable
{
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
    }

    //region wtf

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        IMDrawable.super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        return IMDrawable.super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return IMDrawable.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return IMDrawable.super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return IMDrawable.super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return IMDrawable.super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return IMDrawable.super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return IMDrawable.super.charTyped(chr, modifiers);
    }

    @Nullable
    @Override
    public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
        return IMDrawable.super.getNavigationPath(navigation);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return IMDrawable.super.isMouseOver(mouseX, mouseY);
    }

    private boolean focused;

    @Override
    public void setFocused(boolean focused)
    {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Nullable
    @Override
    public GuiNavigationPath getFocusedPath() {
        return IMDrawable.super.getFocusedPath();
    }

    @Override
    public SelectionType getType() {
        return SelectionType.HOVERED;
    }

    @Override
    public boolean isNarratable() {
        return IMDrawable.super.isNarratable();
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder)
    {
    }

    @Override
    public int getNavigationOrder() {
        return IMDrawable.super.getNavigationOrder();
    }

    @Override
    public ScreenRect getNavigationFocus() {
        return super.getNavigationFocus();
    }

    @Override
    public void forEachChild(Consumer<ClickableWidget> consumer) {
        super.forEachChild(consumer);
    }

    @Override
    public void invalidatePosition()
    {
    }

    @Override
    public void invalidateLayout()
    {
    }

    private int depth = 0;

    /**
     * Depth of this IMDrawable, higher value means this drawable should be rendered below others
     */
    @Override
    public int getDepth()
    {
        return this.depth;
    }

    @Override
    public void setDepth(int depth)
    {
        this.depth = depth;
    }

    //endregion
}

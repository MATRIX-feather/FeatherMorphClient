package xyz.nifeather.morph.client.screens.disguise;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EntityDisplayEntry extends ElementListWidget.Entry<EntityDisplayEntry> implements Comparable<EntityDisplayEntry>
{
    private DisplayWidget displayWidget;

    private String identifier = "???";

    public String getEntityName()
    {
        return displayWidget.entityName() == null ? "???" : displayWidget.entityName();
    }

    public String getIdentifierString()
    {
        return identifier;
    }

    private Identifier identifierAsNms;

    private static final Identifier defaultIdentifier = Identifier.of("morph", "unknown");

    private int parentWidth = 0;
    private int expectedWidth = 0;

    public void updateParentAllowedScreenSpaceWidth(int allowedWidth)
    {
        this.parentWidth = allowedWidth;
        this.expectedWidth = parentWidth;

        this.children.forEach(displayWidget -> displayWidget.setWidth(expectedWidth));
    }

    public Identifier getIdentifier()
    {
        if (identifierAsNms != null)
            return identifierAsNms;

        var id = Identifier.tryParse(identifier.toLowerCase());
        if (id == null)
            id = defaultIdentifier;

        this.identifierAsNms = id;
        return id;
    }

    private final List<DisplayWidget> children = new ObjectArrayList<>();

    @Override
    public List<? extends Selectable> selectableChildren()
    {
        return children;
    }

    @Override
    public List<? extends Element> children()
    {
        return children;
    }

    public EntityDisplayEntry(String name)
    {
        initFields(name);
    }

    public void clearChildren()
    {
        children.forEach(DisplayWidget::dispose);
        children.clear();
    }

    private void initFields(String name)
    {
        this.identifier = name;
        displayWidget = new DisplayWidget(0, 0, 180, 20, name);

        if (expectedWidth > 0)
            displayWidget.setWidth(expectedWidth);

        children.add(displayWidget);
    }

    @Override
    public void render(DrawContext matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
    {
        displayWidget.screenSpaceY = y;
        displayWidget.screenSpaceX = x;
        displayWidget.render(matrices, mouseX, mouseY, tickDelta);
    }

    @Override
    public int compareTo(@NotNull EntityDisplayEntry entityDisplayEntry)
    {
        return identifier.compareTo(entityDisplayEntry.identifier);
    }
}

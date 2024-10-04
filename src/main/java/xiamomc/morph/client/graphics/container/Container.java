package xiamomc.morph.client.graphics.container;

import xiamomc.morph.client.graphics.MDrawable;

import java.util.ArrayList;
import java.util.List;

public class Container extends BasicContainer<MDrawable>
{
    public List<MDrawable> children()
    {
        return new ArrayList<>(super.children);
    }
}
package xiamomc.morph.client.screens.disguise;

import net.minecraft.text.Text;
import xiamomc.morph.client.graphics.Box;
import xiamomc.morph.client.graphics.IMDrawable;
import xiamomc.morph.client.screens.FeatherScreen;

public class DisguiseScreenNew extends FeatherScreen
{
    protected DisguiseScreenNew(Text title)
    {
        super(title);

        var children = new IMDrawable[]
        {
                new Box()
        };
    }
}

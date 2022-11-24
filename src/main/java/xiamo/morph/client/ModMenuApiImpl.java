package xiamo.morph.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuApiImpl implements ModMenuApi
{
    public MorphClient modInstance = MorphClient.getInstance();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return parent -> modInstance.getFactory(parent).build();
    }
}

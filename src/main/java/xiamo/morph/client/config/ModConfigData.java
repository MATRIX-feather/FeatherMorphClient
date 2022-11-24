package xiamo.morph.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "morphclient")
public class ModConfigData implements ConfigData
{
    public boolean alwaysShowPreviewInInventory = false;

    public boolean showClientMob = false;
}

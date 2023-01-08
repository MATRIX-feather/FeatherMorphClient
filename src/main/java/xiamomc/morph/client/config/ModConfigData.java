package xiamomc.morph.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import xiamomc.morph.client.MorphClient;

@Config(name = "morphclient")
public class ModConfigData implements ConfigData
{
    public boolean alwaysShowPreviewInInventory = false;

    public boolean allowClientView = true;

    public boolean verbosePackets = false;

    public boolean displayDisguiseOnHud = true;

    public boolean clientViewVisible()
    {
        return MorphClient.getInstance().morphManager.selfVisibleToggled.get() && allowClientView;
    }
}

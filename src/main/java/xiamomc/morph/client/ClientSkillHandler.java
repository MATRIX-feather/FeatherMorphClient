package xiamomc.morph.client;

import xiamomc.pluginbase.Annotations.Initializer;

public class ClientSkillHandler extends MorphClientObject
{
    private long skillCooldown = -1;
    private long currentCooldown = -1;

    public void setSkillCooldown(long cd)
    {
        this.skillCooldown = cd;
        this.currentCooldown = cd;
    }

    public long getSkillCooldown()
    {
        return skillCooldown;
    }

    public long getCurrentCooldown()
    {
        return currentCooldown;
    }

    @Initializer
    private void load()
    {
        this.addSchedule(this::update);
    }

    private void update()
    {
        this.addSchedule(this::update);

        if (currentCooldown > 0) currentCooldown--;
    }
}

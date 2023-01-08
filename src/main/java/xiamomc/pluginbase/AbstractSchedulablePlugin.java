package xiamomc.pluginbase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.slf4j.Logger;
import xiamomc.pluginbase.Managers.DependencyManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class AbstractSchedulablePlugin
{
    public abstract String getNameSpace();

    public abstract Logger getSLF4JLogger();

    private static final Map<String, AbstractSchedulablePlugin> instances = new ConcurrentHashMap<>();

    @Deprecated
    public static AbstractSchedulablePlugin GetInstance(String nameSpace)
    {
        return getInstance(nameSpace);
    }

    public static AbstractSchedulablePlugin getInstance(String nameSpace)
    {
        return instances.get(nameSpace);
    }

    protected final DependencyManager dependencyManager;

    protected final Logger logger = getSLF4JLogger();
    protected AbstractSchedulablePlugin()
    {
        dependencyManager = DependencyManager.getManagerOrCreate(this);

        instances.put(getNameSpace(), this);
    }

    protected final List<ScheduleInfo> schedules = new ObjectArrayList<>();

    @Deprecated
    public ScheduleInfo schedule(Consumer<?> consumer)
    {
        return this.schedule(() -> consumer.accept(null));
    }

    @Deprecated
    public ScheduleInfo schedule(Consumer<?> c, int delay)
    {
        return this.schedule(() -> c.accept(null), delay);
    }

    @Deprecated
    public ScheduleInfo schedule(Consumer<?> c, int delay, boolean isAsync)
    {
        return this.schedule(() -> c.accept(null), delay, isAsync);
    }

    public ScheduleInfo schedule(Runnable runnable)
    {
        return this.schedule(runnable, 1);
    }

    public ScheduleInfo schedule(Runnable function, int delay)
    {
        return this.schedule(function, delay, false);
    }

    public ScheduleInfo schedule(Runnable function, int delay, boolean async)
    {
        var si = new ScheduleInfo(function, delay, getCurrentTick(), async);

        if (!acceptSchedules())
        {
            si.cancel();
            return si;
        }

        synchronized (schedules)
        {
            //Logger.info("添加：" + si + "，当前TICK：" + currentTick);
            schedules.add(si);
        }

        return si;
    }

    public abstract long getCurrentTick();

    public abstract boolean acceptSchedules();
}

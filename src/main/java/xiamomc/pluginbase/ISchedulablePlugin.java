package xiamomc.pluginbase;

import java.util.function.Consumer;

public interface ISchedulablePlugin
{
    @Deprecated
    public ScheduleInfo schedule(Consumer<?> consumer);

    @Deprecated
    public ScheduleInfo schedule(Consumer<?> c, int delay);

    @Deprecated
    public ScheduleInfo schedule(Consumer<?> c, int delay, boolean isAsync);

    public ScheduleInfo schedule(Runnable runnable);

    public ScheduleInfo schedule(Runnable function, int delay);

    public ScheduleInfo schedule(Runnable function, int delay, boolean async);

    public abstract long getCurrentTick();

    public abstract boolean acceptSchedules();

    abstract void startMainLoop(Runnable mainLoopRunnable);

    abstract void runAsync(Runnable runnable);
}

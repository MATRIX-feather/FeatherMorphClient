package xiamomc.pluginbase;

public class ScheduleInfo
{
    public final Runnable Function;
    public final int Delay;
    public final long TickScheduled;
    public final boolean isAsync;

    private boolean isCanceled = false;

    public void cancel()
    {
        this.isCanceled = true;
    }

    public boolean isCanceled()
    {
        return isCanceled;
    }

    public ScheduleInfo(Runnable function, int delay, long tickScheduled, boolean isAsync)
    {
        this.Function = function;
        this.Delay = delay;
        this.TickScheduled = tickScheduled;

        this.isAsync = isAsync;
    }

    @Override
    public String toString()
    {
        return "于第" + this.TickScheduled + "刻创建，"
                + "并计划于" + this.Delay + "刻后执行的"
                + (isAsync ? "异步" : "") + "计划任务"
                + "（" + this.Function + "）";
    }
}

package xiamomc.pluginbase.Exceptions;

public class DependencyAlreadyRegistedException extends RuntimeException
{
    public DependencyAlreadyRegistedException(String s)
    {
        super(s);
    }
}

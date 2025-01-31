package xiamomc.pluginbase.Managers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.XiaMoJavaPlugin;
import xiamomc.pluginbase.Exceptions.DependencyAlreadyRegistedException;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DependencyManager
{
    //region 实例相关
    private final static Map<String, DependencyManager> instances = new ConcurrentHashMap<>();

    @Deprecated
    public static DependencyManager GetInstance(String namespace)
    {
        return getInstance(namespace);
    }

    public static DependencyManager getInstance(String namespace)
    {
        return instances.get(namespace);
    }

    /**
     * 获取或创建某个插件的依赖管理器
     * @param pluginInstance 插件实例
     * @return 此插件的依赖管理器
     */
    @Contract("null -> null; !null -> !null")
    @Nullable
    public static DependencyManager getManagerOrCreate(XiaMoJavaPlugin pluginInstance)
    {
        if (pluginInstance == null) return null;

        var depMgr = instances.get(pluginInstance.namespace());
        if (depMgr != null) return depMgr;

        depMgr = new DependencyManager(pluginInstance);

        return depMgr;
    }

    /**
     * @deprecated 建议使用 {@link DependencyManager#getManagerOrCreate(XiaMoJavaPlugin)}
     * @param plugin 插件实例
     */
    @Deprecated
    public DependencyManager(XiaMoJavaPlugin plugin)
    {
        registerPluginInstance(plugin);
    }

    public void registerPluginInstance(XiaMoJavaPlugin plugin)
    {
        if (instances.containsKey(plugin.namespace()))
        {
            LoggerFactory.getLogger("XiaMoBase").warn("已经有一个 " + plugin.namespace() + "的DependencyManager实例了");
            Thread.dumpStack();
        }

        instances.put(plugin.namespace(), this);
    }

    public void unRegisterPluginInstance(XiaMoJavaPlugin plugin)
    {
        instances.remove(plugin.namespace());
    }
    //endregion 实例相关

    //注册表
    private final Map<Class<?>, Object> registers = new ConcurrentHashMap<>();

    @Deprecated
    public void Cache(Object obj) throws DependencyAlreadyRegistedException
    {
        cache(obj);
    }

    /**
     * 注册一个对象到依赖表中
     *
     * @param obj 要注册的对象
     * @throws DependencyAlreadyRegistedException 该对象所对应的Class是否已被注册
     */
    public void cache(Object obj) throws DependencyAlreadyRegistedException
    {
        cacheAs(obj.getClass(), obj);
    }

    @Deprecated
    public void CacheAs(Class<?> classType, Object obj) throws DependencyAlreadyRegistedException
    {
        cacheAs(classType, obj);
    }

    /**
     * 将一个对象作为某个Class类型注册到依赖表中
     *
     * @param classType 要注册的Class类型
     * @param obj       要注册的对象
     * @throws DependencyAlreadyRegistedException 是否已经注册过一个相同的classType了
     * @throws IllegalArgumentException           传入的对象不能转化为classType的实例
     */
    public void cacheAs(Class<?> classType, Object obj) throws DependencyAlreadyRegistedException
    {
        synchronized (registers)
        {
            //检查obj是否能cast成classType
            if (!classType.isInstance(obj))
                throw new IllegalArgumentException(obj + "不能注册为" + classType);

            //检查是否重复注册
            if (registers.containsKey(classType))
                throw new DependencyAlreadyRegistedException("已经注册过一个" + classType.getSimpleName() + "的依赖了");

            registers.put(classType, obj);
        }
    }

    @Deprecated
    public boolean UnCache(Object obj)
    {
        return unCache(obj);
    }

    /**
     * 反注册一个对象
     *
     * @param obj 要反注册的对象
     * @return 是否成功
     */
    public boolean unCache(Object obj)
    {
        if (!registers.containsValue(obj))
            return false;

        registers.remove(obj.getClass(), obj);
        return true;
    }

    @Deprecated
    public void UnCacheAll()
    {
        this.unCacheAll();
    }

    /**
     * 反注册所有对象
     */
    public void unCacheAll()
    {
        registers.clear();
    }

    @Deprecated
    public <T> T Get(Class<T> classType)
    {
        return get(classType);
    }

    @Deprecated
    public <T> T Get(Class<T> classType, boolean throwOnNotFound)
    {
        return this.get(classType, throwOnNotFound);
    }

    /**
     * 从依赖表获取classType所对应的对象
     *
     * @param classType 目标Class类型
     * @return 找到的对象，返回null则未找到
     * @throws NullDependencyException 依赖未找到时抛出的异常
     */
    public <T> T get(Class<T> classType)
    {
        return this.get(classType, true);
    }

    @Nullable
    public <T> T get(Class<T> classType, boolean throwOnNotFound)
    {
        if (registers.containsKey(classType))
            return (T) registers.get(classType);

        if (throwOnNotFound) throw new NullDependencyException("依赖的对象（" + classType + "）未找到");
        else return null;
    }
}

package xiamomc.pluginbase.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记某个字段为类的依赖项。
 * <br>
 * <b>除非ShouldSolveImmediately是true，不然此依赖默认会在类被创建的下一个tick解析。</b>
 *
 * @apiNote 使用Resolved的类必须直接或者间接扩展PluginObject，标记为Resolved的字段必须为private访问
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Resolved
{
    /**
     * 标记此依赖是否要立即获取
     */
    boolean shouldSolveImmediately() default false;

    /**
     * 标记此依赖是否可以为null
     */
    boolean allowNull() default false;
}

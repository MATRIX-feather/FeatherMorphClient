package xiamomc.pluginbase.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记某个方法为该类的初始化方法。
 *
 * @apiNote 被标记的方法将在类被创建后的下个tick执行，并自动将获取到的依赖传递进去。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Initializer
{
}

package me.lengyu.qedge.hook.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author 冷雨
 * @Description 钩子项注解
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface HookItemAnnotation {
    String value() default "";
    String tag() default "";
    String desc() default "";
    String category() default HookCategory.OTHER;
    String process() default "";
}
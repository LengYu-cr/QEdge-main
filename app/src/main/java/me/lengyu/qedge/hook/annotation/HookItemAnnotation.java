package me.lengyu.qedge.hook.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HookItemAnnotation {
    String value() default "";
    String tag() default "";
    String desc() default "";
    String category() default HookCategory.OTHER;
    String process() default "";
}
package de.upb.spl.jumpstarter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Finish {
    boolean runOnExit() default true;
    boolean enabled() default true;
    int order() default 0;
}

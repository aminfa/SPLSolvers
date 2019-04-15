package de.upb.spl.jumpstarter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Reasoner {
    int order() default 0;
    boolean enabled() default true;

    int times() default 1;
}

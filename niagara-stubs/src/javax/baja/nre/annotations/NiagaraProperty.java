package javax.baja.nre.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(NiagaraProperties.class)
public @interface NiagaraProperty {
    String name();

    String type();

    String defaultValue() default "";

    int flags() default 0;
}

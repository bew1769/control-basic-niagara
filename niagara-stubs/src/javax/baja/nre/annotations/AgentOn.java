package javax.baja.nre.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AgentOn {
    String[] types() default {};

    Preference defaultAgent() default Preference.OPTIONAL;

    enum Preference {
        OPTIONAL,
        PREFERRED,
        REQUIRED
    }
}

package javamysqlconnectiondbhelper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DatabaseHelper {
    String host() default "";
    String port() default  "";
    String username() default "";
    String password() default "";
    String database() default "";
    String[] optionConfig() default {};
}

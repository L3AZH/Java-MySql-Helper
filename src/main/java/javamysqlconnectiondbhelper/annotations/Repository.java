package javamysqlconnectiondbhelper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Repository {
    /**
     * Can't get the helper value in annotation processor code
     * Must get by AnnotationMirror and AnnotationValue
     * @Author Lam Ha Tuan Anh
     */
    Class<?> helper();
}

package org.openremote.manager.shared.http;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SuccessStatusCode {
    int value() default 200;
}

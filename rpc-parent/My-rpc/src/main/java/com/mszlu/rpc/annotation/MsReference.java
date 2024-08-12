package com.mszlu.rpc.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD,ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MsReference {
//    String uri() default "";
//
//    Class resultType();
//    String host();
//    int port();
    String version() default "1.0";
}

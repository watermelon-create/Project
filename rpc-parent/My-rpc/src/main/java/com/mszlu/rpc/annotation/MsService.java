package com.mszlu.rpc.annotation;


import java.lang.annotation.*;

//可用于类上
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface MsService {

    String version() default "1.0";
}

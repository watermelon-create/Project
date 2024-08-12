package com.mszlu.rpc.annontation;


import java.lang.annotation.*;

//可用于类上
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface MsHttpClient {
    String value();
}

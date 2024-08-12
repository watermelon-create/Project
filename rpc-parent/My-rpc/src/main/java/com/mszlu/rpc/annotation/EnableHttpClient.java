package com.mszlu.rpc.annotation;

import com.mszlu.rpc.bean.MsBeanDefinitionRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({MsBeanDefinitionRegistry.class})
public @interface EnableHttpClient {
    String basePackage() default "";
}

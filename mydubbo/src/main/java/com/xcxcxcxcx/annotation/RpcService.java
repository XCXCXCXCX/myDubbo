package com.xcxcxcxcx.annotation;

import org.springframework.beans.factory.annotation.Required;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author XCXCXCXCX
 * @date 2018/9/4
 * @comments
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcService {
    String value() default "";
}

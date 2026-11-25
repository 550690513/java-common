package com.zhe.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法入参打印：方法执行前打印参数，抛出异常时打印异常信息。
 * <p>
 * 可标注在方法或类上，标注在类上时对该类所有 public 方法生效。
 */
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestLog {

}

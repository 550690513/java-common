package com.zhe.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法出入参全量打印：入口打印参数，返回时打印结果与耗时（仅最外层调用打印结果与耗时，避免嵌套调用重复打印）。
 * <p>
 * 可标注在方法或类上，标注在类上时对该类所有 public 方法生效。
 */
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogPrint {

}

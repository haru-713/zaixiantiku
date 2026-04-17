package com.example.zaixiantiku.common.annotation;

import java.lang.annotation.*;

/**
 * 自定义操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作描述
     */
    String operation() default "";
}

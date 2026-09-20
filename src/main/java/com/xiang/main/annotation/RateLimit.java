package com.xiang.main.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流：加在 Controller 方法上，按「当前方法 + 访问者 IP」统计访问次数，
 * 窗口期内超过 limit 次就拒绝请求（由 RateLimitAspect 实现）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口期内允许的最大访问次数 */
    int limit() default 5;

    /** 窗口长度，单位秒 */
    int window() default 60;
}

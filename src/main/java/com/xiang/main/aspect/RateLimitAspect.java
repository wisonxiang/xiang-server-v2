package com.xiang.main.aspect;

import com.xiang.main.annotation.RateLimit;
import com.xiang.main.common.IpUtils;
import com.xiang.main.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 限流切面：拦截带 @RateLimit 的方法，在业务代码执行前按「方法 + IP」计数，
 * 超限时抛 429，由 GlobalExceptionHandler 统一转成 Result
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private final RateLimitService rateLimitService;
    private final HttpServletRequest request;

    public RateLimitAspect(RateLimitService rateLimitService, HttpServletRequest request) {
        this.rateLimitService = rateLimitService;
        this.request = request;
    }

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String ip = IpUtils.getClientIp(request);
        // 带上方法名，不同接口各算各的次数，互不干扰
        String key = method.getDeclaringClass().getSimpleName() + "." + method.getName() + ":" + ip;

        if (!rateLimitService.tryAcquire(key, rateLimit.limit(), Duration.ofSeconds(rateLimit.window()))) {
            log.warn("请求过于频繁，key={}，限制 {} 次 / {} 秒", key, rateLimit.limit(), rateLimit.window());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试");
        }
    }
}

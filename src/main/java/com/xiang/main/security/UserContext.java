package com.xiang.main.security;

/**
 * 当前登录用户上下文：请求进来时在过滤器里塞值，业务层随时 UserContext.get() 取
 * 请求结束必须 clear：ThreadLocal 绑在线程上，而线程是池化复用的，不清会把上一个用户的信息带给下一个请求
 */
public final class UserContext {

    private static final ThreadLocal<LoginUserInfo> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUserInfo info) {
        HOLDER.set(info);
    }

    /** 未登录（或没经过过滤器，比如某些异步线程）时为 null */
    public static LoginUserInfo get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

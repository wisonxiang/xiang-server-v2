package com.xiang.main.security;

import com.xiang.main.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 未认证（未携带 token / token 无效 / 已过期）时的统一返回。
 * 如果请求的 URL 本身没有匹配的 Handler（未定义路由），返回 404 而不是 401。
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String DEFAULT_MESSAGE = "未登录或 token 无效";
    private static final String NOT_FOUND_MESSAGE = "接口不存在";

    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerMapping handlerMapping;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper,
                                       RequestMappingHandlerMapping handlerMapping) {
        this.objectMapper = objectMapper;
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        commence(request, response, DEFAULT_MESSAGE);
    }

    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         String message) throws IOException {
        if (hasHandler(request)) {
            write(response, HttpServletResponse.SC_UNAUTHORIZED, message);
        } else {
            write(response, HttpServletResponse.SC_NOT_FOUND, NOT_FOUND_MESSAGE);
        }
    }

    /** 探测失败时按"路由存在"处理，走 401，避免误报 404 */
    private boolean hasHandler(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            return chain != null;
        } catch (Exception e) {
            return true;
        }
    }

    private void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.fail(status, message));
    }
}

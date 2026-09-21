package com.xiang.main.exception;

import com.xiang.main.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidException(MethodArgumentNotValidException e) {
        String errMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(Result.PARAM_ERROR_CODE, errMsg));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误"));
    }

    @ExceptionHandler(LoginException.class)
    public ResponseEntity<Result<Void>> handleLoginException(LoginException e) {
        return ResponseEntity.ok(Result.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    /**
     * 业务主动抛出的状态码异常（如「用户名已存在」「用户不存在」），保留原始状态码与提示
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Void>> handleResponseStatusException(ResponseStatusException e) {
        int status = e.getStatusCode().value();
        return ResponseEntity.status(e.getStatusCode())
                .body(Result.fail(status, e.getReason()));
    }

    /**
     * 未定义路由：关掉静态资源映射后由 DispatcherServlet 抛 NoHandlerFoundException，
     * 静态资源未命中时抛 NoResourceFoundException，都按 404 返回，不能落进下面的兜底 500
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Result<Void>> handleNotFoundException(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(HttpStatus.NOT_FOUND.value(), "接口不存在"));
    }

    /** 请求方法与路由不匹配（如对只支持 POST 的接口发 GET），返回 405 而不是 500 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持"));
    }

    /**
     * 兜底：未知异常（数据库报错、空指针等）只把详情写日志，返回通用提示，
     * 避免表结构、SQL、堆栈等内部信息通过响应体泄漏给调用方
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(Result.ERROR_CODE, "服务器繁忙，请稍后重试"));
    }
}

package com.xiang.main.common;

import lombok.Data;

@Data
public class Result<T> {

    public static final int SUCCESS_CODE = 200;
    public static final int PARAM_ERROR_CODE = 400;
    public static final int ERROR_CODE = 500;

    private Integer code;
    private Boolean success;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(SUCCESS_CODE);
        result.setSuccess(true);
        result.setMessage("ok");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}

package com.shiyu.backend.common;

import com.shiyu.backend.context.TraceIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;


/**
 * 全局异常处理器。
 * 负责将各类异常转换为统一 `ApiResponse`。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常。
     *
     * @param ex 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理 `@RequestBody` 参数校验异常。
     *
     * @param ex 校验异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null ? BizCode.PARAM_ERROR.getMessage() : fieldError.getField() + ":" + fieldError.getDefaultMessage();
        return ApiResponse.fail(BizCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理表单绑定异常。
     *
     * @param ex 绑定异常
     * @return 失败响应
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null ? BizCode.PARAM_ERROR.getMessage() : fieldError.getField() + ":" + fieldError.getDefaultMessage();
        return ApiResponse.fail(BizCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理方法级参数约束异常。
     *
     * @param ex 约束异常
     * @return 失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = BizCode.PARAM_ERROR.getMessage();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            message = violation.getPropertyPath() + ":" + violation.getMessage();
            break;
        }
        return ApiResponse.fail(BizCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理路径参数/查询参数类型不匹配异常。
     *
     * @param ex 类型不匹配异常
     * @return 参数错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String traceId = TraceIdContext.get();
        String paramName = ex.getName();
        String paramValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        String uri = request != null ? request.getRequestURI() : "";
        log.warn("param type mismatch, traceId={}, param={}, value={}, uri={}", traceId, paramName, paramValue, uri);
        return ApiResponse.fail(BizCode.PARAM_ERROR.getCode(), BizCode.PARAM_ERROR.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String traceId = TraceIdContext.get();
        String method = request == null ? "" : request.getMethod();
        String uri = request == null ? "" : request.getRequestURI();
        log.warn("method not supported, traceId={}, method={}, uri={}, message={}", traceId, method, uri, ex.getMessage());
        return ApiResponse.fail(BizCode.PARAM_ERROR.getCode(), "请求方式不正确: " + method + " " + uri);
    }

    /**
     * 处理未捕获异常。
     *
     * @param ex 异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("unexpected error, traceId={}", TraceIdContext.get(), ex);
        return ApiResponse.fail(BizCode.SYSTEM_ERROR);
    }


}

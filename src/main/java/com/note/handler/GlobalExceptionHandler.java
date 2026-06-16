package com.note.handler;

import com.note.common.Result;
import com.note.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    // 处理自定义异常(最高优先级)
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("BusinessException: ", e);
        return Result.fail(e.getCode(), e.getMessage());
    }

    // 处理 404 资源不存在
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResourceFoundException(NoResourceFoundException e) {
        return Result.fail("资源不存在");
    }

    // 处理运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException: ", e);
        return Result.fail(e.getMessage());
    }

    // 处理其他异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("Exception: ", e);
        return Result.fail("服务器内部错误");
    }
}

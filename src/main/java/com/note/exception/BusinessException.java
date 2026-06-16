package com.note.exception;

import com.note.constant.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class BusinessException extends RuntimeException{
    // 自定义错误码
    private Integer code;
    // 错误提示信息
    private String msg;

    public BusinessException(String msg) {
        super(msg);
        this.code = ResultCode.FAIL.getCode();
        this.msg = msg;
    }

    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    // 传入枚举快速抛异常
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
    }
}

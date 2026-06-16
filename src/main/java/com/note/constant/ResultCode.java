package com.note.constant;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    RESOURCE_NOT_FOUND(404, "资源不存在"),
    PARAM_ERROR(400, "参数非法"),
    TOKEN_INVALID(401, "登录失效，请重新登录"),
    NOT_LOGIN(402, "未登录"),
    NOT_FOUND(407, "参数错误"),
    Empty(408, "参数为空"),
    PASSWORD_ERROR(409, "密码错误"),
    MORE_THAN_MAX_LENGTH(410,"超出最大字数限制" );

    private final Integer code;
    private final String msg;

    ResultCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
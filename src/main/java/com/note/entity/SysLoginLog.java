package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
@Schema(description = "登录日志")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录IP")
    private String ipAddr;

    @Schema(description = "浏览器信息")
    private String browser;

    @Schema(description = "登录状态：0-失败，1-成功")
    private Integer loginStatus;

    @Schema(description = "状态消息")
    private String msg;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

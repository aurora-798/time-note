package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
@Schema(description = "用户信息")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "日记风格偏好")
    private String diaryStyle;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "是否VIP：0-否，1-是")
    private Integer isVip;

    @Schema(description = "VIP到期时间")
    private LocalDateTime vipExpireTime;

    @Schema(description = "状态：0-禁用，1-正常")
    private Integer status;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除标记")
    private Integer deleteFlag;
}

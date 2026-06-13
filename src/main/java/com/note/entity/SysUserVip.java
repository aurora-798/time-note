package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_vip")
@Schema(description = "VIP权益记录")
public class SysUserVip {

    @TableId(type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "VIP等级")
    private Integer vipLevel;

    @Schema(description = "VIP开始时间")
    private LocalDateTime startTime;

    @Schema(description = "VIP结束时间")
    private LocalDateTime endTime;

    @Schema(description = "每日AI调用次数上限")
    private Integer dailyAiLimit;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

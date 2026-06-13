package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_token_record")
@Schema(description = "AI Token消耗记录")
public class AiTokenRecord {

    @TableId(type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "关联日记ID")
    private Long diaryId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "提示词Token数")
    private Integer promptTokens;

    @Schema(description = "补全Token数")
    private Integer completionTokens;

    @Schema(description = "总Token数")
    private Integer totalTokens;

    @Schema(description = "调用状态：0-失败，1-成功")
    private Integer callStatus;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "调用时间")
    private LocalDateTime callTime;
}

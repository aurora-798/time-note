package com.note.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class MyMetaFillHandler implements MetaObjectHandler {

    /**
     * 插入时填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // strictInsertFill：实体字段为null才填充，有值则保留用户传入的值
        strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }

    /**
     * 更新时填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新只刷新updateTime
        strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
}
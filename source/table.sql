CREATE TABLE `sys_user` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
                            `username` varchar(50) NOT NULL COMMENT '登录账号，唯一',
                            `password` varchar(100) NOT NULL COMMENT '加密密码',
                            `nickname` varchar(50) NOT NULL COMMENT '用户昵称',
                            `avatar` varchar(255) DEFAULT '' COMMENT '头像文件URL',
                            `gender` tinyint DEFAULT 0 COMMENT '0未知 1男 2女',
                            `age` int DEFAULT NULL COMMENT '年龄',
                            `diary_style` varchar(30) DEFAULT 'normal' COMMENT '默认日记风格：normal日常/literary文艺/simple简约',
                            `role` varchar(20) NOT NULL DEFAULT 'user' COMMENT '角色：admin管理员、user普通用户',
                            `is_vip` tinyint NOT NULL DEFAULT 0 COMMENT '0非VIP 1VIP',
                            `vip_expire_time` datetime DEFAULT NULL COMMENT 'VIP过期时间',
                            `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态：0禁用 1正常',
                            `phone` varchar(20) DEFAULT '' COMMENT '手机号',
                            `email` varchar(100) DEFAULT '' COMMENT '邮箱',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`),
                            KEY `idx_role` (`role`),
                            KEY `idx_status` (`status`),
                            KEY `idx_is_vip` (`is_vip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';


CREATE TABLE `sys_diary` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日记ID',
                             `user_id` bigint NOT NULL COMMENT '所属用户ID，关联sys_user.id',
                             `diary_date` date NOT NULL COMMENT '日记对应日期（哪一天的日记）',
                             `title` varchar(200) DEFAULT '今日日记' COMMENT '日记标题',
                             `content` longtext NOT NULL COMMENT '日记正文（AI生成内容）',
                             `source_content` longtext NULL COMMENT '用户原始输入：语音/文字原文',
                             `style` varchar(30) DEFAULT 'normal' COMMENT '本条日记使用的风格',
                             `word_count` int DEFAULT 0 COMMENT '日记字数统计',
                             `is_edit` tinyint NOT NULL DEFAULT 0 COMMENT '0AI原始内容 1用户手动编辑过',
                             `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常 0隐藏',
                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建生成时间',
                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                             `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                             PRIMARY KEY (`id`),
                             KEY `idx_user_id` (`user_id`),
                             KEY `idx_diary_date` (`diary_date`),
                             KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日记内容表';


CREATE TABLE `sys_media` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资源ID',
                             `user_id` bigint NOT NULL COMMENT '上传用户ID',
                             `diary_id` bigint DEFAULT NULL COMMENT '关联日记ID，未绑定日记为null',
                             `media_type` tinyint NOT NULL COMMENT '1图片 2视频',
                             `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
                             `file_url` varchar(500) NOT NULL COMMENT 'OSS访问地址',
                             `file_size` bigint NOT NULL COMMENT '文件大小，单位字节',
                             `suffix` varchar(20) NOT NULL COMMENT '文件后缀：jpg/mp4/mp3等',
                             `remark` varchar(500) DEFAULT '' COMMENT '用户对该素材的文字描述',
                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `delete_flag` tinyint NOT NULL DEFAULT 0,
                             PRIMARY KEY (`id`),
                             KEY `idx_user_id` (`user_id`),
                             KEY `idx_diary_id` (`diary_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传多媒体文件表';


CREATE TABLE `ai_token_record` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                   `user_id` bigint NOT NULL COMMENT '操作用户ID',
                                   `diary_id` bigint DEFAULT NULL COMMENT '本次生成对应的日记ID',
                                   `model_name` varchar(100) NOT NULL COMMENT '大模型名称：doubao、qwen等',
                                   `prompt_tokens` int NOT NULL DEFAULT 0 COMMENT '输入提示词token数',
                                   `completion_tokens` int NOT NULL DEFAULT 0 COMMENT '输出回答token数',
                                   `total_tokens` int NOT NULL DEFAULT 0 COMMENT '合计总token',
                                   `call_status` tinyint NOT NULL COMMENT '1调用成功 0调用失败',
                                   `error_msg` text NULL COMMENT '失败异常信息',
                                   `call_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_call_time` (`call_time`),
                                   KEY `idx_diary_id` (`diary_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用Token消耗记录表';


CREATE TABLE `sys_user_vip` (
                                `id` bigint NOT NULL AUTO_INCREMENT,
                                `user_id` bigint NOT NULL COMMENT '对应用户ID',
                                `vip_level` tinyint NOT NULL DEFAULT 0 COMMENT '0无 1普通VIP 2高级VIP',
                                `start_time` datetime NOT NULL COMMENT 'VIP生效开始时间',
                                `end_time` datetime NOT NULL COMMENT 'VIP过期结束时间',
                                `daily_ai_limit` int DEFAULT 5000 COMMENT '每日AI最大token额度',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户VIP权益表';

CREATE TABLE `sys_login_log` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `user_id` bigint DEFAULT NULL,
                                 `username` varchar(50) NOT NULL COMMENT '登录账号',
                                 `ip_addr` varchar(100) DEFAULT '' COMMENT '登录IP',
                                 `browser` varchar(200) DEFAULT '' COMMENT '浏览器设备',
                                 `login_status` tinyint NOT NULL COMMENT '1成功 0失败',
                                 `msg` varchar(500) DEFAULT '' COMMENT '登录信息',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';


CREATE TABLE `ai_chat_session` (
    `id` bigint NOT NULL COMMENT '会话ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `title` varchar(200) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_update` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 聊天会话表';


CREATE TABLE `ai_chat_message` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id` bigint NOT NULL COMMENT '会话ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role` varchar(20) NOT NULL COMMENT 'user | assistant',
    `content` longtext NOT NULL COMMENT '消息内容',
    `search_query` varchar(1000) DEFAULT NULL COMMENT '检索改写 query，仅 user 消息',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_time` (`session_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 聊天消息表';
CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    session_key VARCHAR(36) NOT NULL COMMENT '对外暴露的会话唯一标识(UUID)',
    title VARCHAR(256) DEFAULT '新对话' COMMENT '会话标题',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active|archived|deleted',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    UNIQUE INDEX idx_session_key (session_key),
    INDEX idx_status (status),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    conversation_id BIGINT NOT NULL COMMENT '关联conversations.id',
    role VARCHAR(16) NOT NULL COMMENT 'user|assistant|system',
    sequence INT NOT NULL DEFAULT 0 COMMENT '会话内消息序号(严格递增)',
    content TEXT COMMENT '消息正文',
    content_type VARCHAR(32) DEFAULT 'text' COMMENT 'text|thinking|reference|recommend',
    metadata JSON COMMENT '工具调用记录、搜索来源、首字耗时等',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '消息产生时间',
    INDEX idx_conversation_seq (conversation_id, sequence),
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

CREATE TABLE IF NOT EXISTS gateway_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL,
    client_ip VARCHAR(45),
    method VARCHAR(10),
    path VARCHAR(512),
    status_code INT,
    duration_ms BIGINT,
    user_agent VARCHAR(512),
    api_key_hash VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created_at (created_at),
    INDEX idx_client_ip (client_ip),
    INDEX idx_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='gateway audit logs';

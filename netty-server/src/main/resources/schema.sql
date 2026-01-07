-- R2DBC용 테이블 생성 (H2)
CREATE TABLE IF NOT EXISTS message_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_message_log_message_id ON message_log(message_id);

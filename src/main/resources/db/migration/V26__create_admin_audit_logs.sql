CREATE TABLE admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_username VARCHAR(100) NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL
);

CREATE INDEX idx_admin_audit_logs_admin_username
    ON admin_audit_logs(admin_username);

CREATE INDEX idx_admin_audit_logs_entity
    ON admin_audit_logs(entity_type, entity_id);

CREATE INDEX idx_admin_audit_logs_timestamp
    ON admin_audit_logs(timestamp);

USE atm_management;

ALTER TABLE accounts
    ADD COLUMN card_status ENUM('ACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE IF NOT EXISTS card_status_history (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    action ENUM('BLOCK','UNBLOCK') NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_status_history_account
        FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    INDEX idx_card_history_account_date (account_id, created_at)
);

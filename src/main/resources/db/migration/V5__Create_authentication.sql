CREATE TABLE auth_account
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id    BIGINT,
    name           VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    phone          VARCHAR(50),
    location       VARCHAR(500),
    password_hash  VARCHAR(255),
    auth_provider  VARCHAR(30)  NOT NULL DEFAULT 'LOCAL',
    google_subject VARCHAR(255),
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT uk_auth_account_email UNIQUE (email),
    CONSTRAINT uk_auth_account_phone UNIQUE (phone),
    CONSTRAINT uk_auth_account_google_subject UNIQUE (google_subject),
    CONSTRAINT fk_auth_account_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE TABLE auth_otp
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    identifier  VARCHAR(255) NOT NULL,
    code_hash   VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6),
    attempts    INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_auth_otp_identifier_created (identifier, created_at)
);

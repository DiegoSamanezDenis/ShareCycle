CREATE TABLE users (
    user_id              BINARY(16)   NOT NULL,
    user_type            VARCHAR(20)  NOT NULL DEFAULT 'RIDER',
    full_name            VARCHAR(200) NOT NULL,
    street_address       VARCHAR(300) NOT NULL,
    email                VARCHAR(200) NOT NULL,
    username             VARCHAR(100) NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    role                 VARCHAR(20)  NOT NULL,
    payment_method_token VARCHAR(200) NULL,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('RIDER', 'OPERATOR')),
    CONSTRAINT ck_users_type CHECK (user_type IN ('RIDER', 'OPERATOR')),
    CONSTRAINT ck_users_email_format CHECK (LOCATE('@', email) > 1)
);

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_user_type ON users (user_type);

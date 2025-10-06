CREATE TABLE IF NOT EXISTS users (
  user_id              BINARY(16)      NOT NULL,  -- store UUID as 16 bytes
  full_name            VARCHAR(200)    NOT NULL,
  street_address       VARCHAR(300)    NOT NULL,
  email                VARCHAR(200)    NOT NULL,
  username             VARCHAR(100)    NOT NULL,
  password_hash        VARCHAR(100)    NOT NULL,  -- bcrypt hash
  role                 VARCHAR(20)     NOT NULL,  -- RIDER or OPERATOR
  payment_method_token VARCHAR(200)    NULL,      -- riders only
  created_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT pk_users PRIMARY KEY (user_id),
  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT uq_users_email UNIQUE (email)
);

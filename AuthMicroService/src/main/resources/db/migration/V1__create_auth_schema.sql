CREATE TABLE IF NOT EXISTS users (
    id_user VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100),
    password VARCHAR(255) NOT NULL,
    surname VARCHAR(100),
    email VARCHAR(150) NOT NULL UNIQUE,
    language VARCHAR(10),
    profile_image LONGBLOB,
    house_user VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email),
    INDEX idx_users_house_user (house_user)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refresh_tokens_token (token),
    INDEX idx_refresh_tokens_username (username),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (username) REFERENCES users (username) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

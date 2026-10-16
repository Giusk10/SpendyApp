CREATE DATABASE IF NOT EXISTS spendy_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS spendy_expense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON spendy_auth.* TO 'spendy'@'%';
GRANT ALL PRIVILEGES ON spendy_expense.* TO 'spendy'@'%';
FLUSH PRIVILEGES;

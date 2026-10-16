-- ==============================================================================
-- SpendyApp - Inizializzazione Database e Configurazione Privilegi Minimi
-- ==============================================================================

-- 1. Creazione degli schemi applicativi isolati
CREATE DATABASE IF NOT EXISTS spendy_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS spendy_expense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Applicazione del principio del minimo privilegio (Least Privilege)
-- L'utente applicativo standard 'spendy' dispone esclusivamente dei permessi DML
-- per il runtime applicativo (SELECT, INSERT, UPDATE, DELETE) e dei permessi DDL
-- strettamente necessari alle migrazioni Flyway all'avvio (CREATE, DROP, ALTER, INDEX, REFERENCES).
-- Sono espressamente revocati/esclusi: ALL PRIVILEGES, GRANT OPTION, permessi globali (*.*),
-- DROP DATABASE, RELOAD, SUPER, PROCESS, SHUTDOWN, FILE.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON spendy_auth.* TO 'spendy'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON spendy_expense.* TO 'spendy'@'%';

FLUSH PRIVILEGES;

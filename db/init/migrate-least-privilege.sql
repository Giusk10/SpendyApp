-- ==============================================================================
-- SpendyApp - Script di Migrazione Privilegi Minimi (Least Privilege)
-- ==============================================================================
-- Da eseguire su database MySQL esistenti per revocare i privilegi amministrativi
-- dall'utente 'spendy' e applicare il principio del privilegio minimo senza perdita di dati.
--
-- Modalità di esecuzione:
-- docker compose exec -i mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" < db/init/migrate-least-privilege.sql
-- ==============================================================================

-- 1. Revoca di ALL PRIVILEGES e GRANT OPTION dall'utente spendy esistente
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'spendy'@'%';

-- 2. Assegnazione dei soli privilegi minimi DML (runtime) e DDL (Flyway)
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON spendy_auth.* TO 'spendy'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON spendy_expense.* TO 'spendy'@'%';

-- 3. (Opzionale) Creazione account applicativi dedicati per isolamento completo degli schemi:
-- Inserire una password sicura al posto di '<STRONG_AUTH_PASSWORD>' e '<STRONG_EXPENSE_PASSWORD>':
--
-- CREATE USER IF NOT EXISTS 'spendy_auth'@'%' IDENTIFIED BY '<STRONG_AUTH_PASSWORD>';
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON spendy_auth.* TO 'spendy_auth'@'%';
--
-- CREATE USER IF NOT EXISTS 'spendy_expense'@'%' IDENTIFIED BY '<STRONG_EXPENSE_PASSWORD>';
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON spendy_expense.* TO 'spendy_expense'@'%';

FLUSH PRIVILEGES;

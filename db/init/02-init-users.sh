#!/bin/sh
set -eu

# ==============================================================================
# SpendyApp - Script di Provisioning Account Separati a Privilegio Minimo
# ==============================================================================
# Viene eseguito automaticamente all'inizializzazione del container MySQL se
# sono configurate le variabili MYSQL_AUTH_USER/PASSWORD e MYSQL_EXPENSE_USER/PASSWORD.

if [ -n "${MYSQL_AUTH_USER:-}" ] && [ -n "${MYSQL_AUTH_PASSWORD:-}" ]; then
    echo "🔐 Provisioning account dedicato per AuthMicroService: ${MYSQL_AUTH_USER}..."
    mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
        CREATE USER IF NOT EXISTS '${MYSQL_AUTH_USER}'@'%' IDENTIFIED BY '${MYSQL_AUTH_PASSWORD}';
        GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON ${MYSQL_DATABASE_AUTH:-spendy_auth}.* TO '${MYSQL_AUTH_USER}'@'%';
        FLUSH PRIVILEGES;
EOSQL
fi

if [ -n "${MYSQL_EXPENSE_USER:-}" ] && [ -n "${MYSQL_EXPENSE_PASSWORD:-}" ]; then
    echo "🔐 Provisioning account dedicato per ExpenseMicroService: ${MYSQL_EXPENSE_USER}..."
    mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
        CREATE USER IF NOT EXISTS '${MYSQL_EXPENSE_USER}'@'%' IDENTIFIED BY '${MYSQL_EXPENSE_PASSWORD}';
        GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON ${MYSQL_DATABASE_EXPENSE:-spendy_expense}.* TO '${MYSQL_EXPENSE_USER}'@'%';
        FLUSH PRIVILEGES;
EOSQL
fi

# Se entrambi gli account dedicati sono stati configurati, revoca i privilegi all'utente condiviso 'spendy'
if [ -n "${MYSQL_AUTH_USER:-}" ] && [ -n "${MYSQL_EXPENSE_USER:-}" ]; then
    echo "🔒 Account dedicati attivi: revoca privilegi utente condiviso '${MYSQL_USER:-spendy}'..."
    mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
        REVOKE ALL PRIVILEGES, GRANT OPTION FROM '${MYSQL_USER:-spendy}'@'%';
        FLUSH PRIVILEGES;
EOSQL
fi

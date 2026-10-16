#!/bin/sh
set -eu

# SpendyApp - Provisioning account MySQL con privilegi separati.
# Eseguito solo al primo bootstrap del volume MySQL: per volumi esistenti usare
# una migrazione esplicita dopo backup e verifica delle credenziali.
#
# Le password vengono escaped come stringhe SQL. NO_BACKSLASH_ESCAPES evita che
# backslash presenti nelle password siano interpretati dal parser SQL.

validate_identifier() {
    case "$1" in
        ""|*[!A-Za-z0-9_]*)
            echo "Errore: identificatore MySQL non valido." >&2
            exit 1
            ;;
    esac
}

sql_escape() {
    # SQL mode NO_BACKSLASH_ESCAPES attivo nella sessione mysql sottostante.
    printf '%s' "$1" | sed "s/'/''/g"
}

AUTH_DB="${MYSQL_DATABASE_AUTH:-spendy_auth}"
EXPENSE_DB="${MYSQL_DATABASE_EXPENSE:-spendy_expense}"
validate_identifier "$AUTH_DB"
validate_identifier "$EXPENSE_DB"

if [ -n "${MYSQL_AUTH_USER:-}" ] && [ -n "${MYSQL_AUTH_PASSWORD:-}" ]; then
    validate_identifier "$MYSQL_AUTH_USER"
    AUTH_PASSWORD_SQL=$(sql_escape "$MYSQL_AUTH_PASSWORD")
    echo "Provisioning account Auth dedicato (password non mostrata)."
    mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
SET SESSION sql_mode = CONCAT(@@sql_mode, ',NO_BACKSLASH_ESCAPES');
CREATE USER IF NOT EXISTS '${MYSQL_AUTH_USER}'@'%' IDENTIFIED BY '${AUTH_PASSWORD_SQL}';
ALTER USER '${MYSQL_AUTH_USER}'@'%' IDENTIFIED BY '${AUTH_PASSWORD_SQL}';
EOSQL
    if [ -n "${MYSQL_AUTH_MIGRATION_USER:-}" ] && [ -n "${MYSQL_AUTH_MIGRATION_PASSWORD:-}" ]; then
        GRANT_SQL="GRANT SELECT, INSERT, UPDATE, DELETE ON ${AUTH_DB}.* TO '${MYSQL_AUTH_USER}'@'%';"
    else
        # Compatibilità: senza account Flyway separato, il runtime deve migrare lo schema.
        GRANT_SQL="GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, DROP ON ${AUTH_DB}.* TO '${MYSQL_AUTH_USER}'@'%';"
    fi
    mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "$GRANT_SQL"
fi

if [ -n "${MYSQL_EXPENSE_USER:-}" ] && [ -n "${MYSQL_EXPENSE_PASSWORD:-}" ]; then
    validate_identifier "$MYSQL_EXPENSE_USER"
    EXPENSE_PASSWORD_SQL=$(sql_escape "$MYSQL_EXPENSE_PASSWORD")
    echo "Provisioning account Expense dedicato (password non mostrata)."
    mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
SET SESSION sql_mode = CONCAT(@@sql_mode, ',NO_BACKSLASH_ESCAPES');
CREATE USER IF NOT EXISTS '${MYSQL_EXPENSE_USER}'@'%' IDENTIFIED BY '${EXPENSE_PASSWORD_SQL}';
ALTER USER '${MYSQL_EXPENSE_USER}'@'%' IDENTIFIED BY '${EXPENSE_PASSWORD_SQL}';
EOSQL
    if [ -n "${MYSQL_EXPENSE_MIGRATION_USER:-}" ] && [ -n "${MYSQL_EXPENSE_MIGRATION_PASSWORD:-}" ]; then
        GRANT_SQL="GRANT SELECT, INSERT, UPDATE, DELETE ON ${EXPENSE_DB}.* TO '${MYSQL_EXPENSE_USER}'@'%';"
    else
        GRANT_SQL="GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, DROP ON ${EXPENSE_DB}.* TO '${MYSQL_EXPENSE_USER}'@'%';"
    fi
    mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "$GRANT_SQL"
fi

# Flyway riceve account separati, con DDL limitato allo schema del servizio.
if [ -n "${MYSQL_AUTH_MIGRATION_USER:-}" ] && [ -n "${MYSQL_AUTH_MIGRATION_PASSWORD:-}" ]; then
    validate_identifier "$MYSQL_AUTH_MIGRATION_USER"
    AUTH_MIGRATION_PASSWORD_SQL=$(sql_escape "$MYSQL_AUTH_MIGRATION_PASSWORD")
    echo "Provisioning account migrazione Auth (password non mostrata)."
    mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
SET SESSION sql_mode = CONCAT(@@sql_mode, ',NO_BACKSLASH_ESCAPES');
CREATE USER IF NOT EXISTS '${MYSQL_AUTH_MIGRATION_USER}'@'%' IDENTIFIED BY '${AUTH_MIGRATION_PASSWORD_SQL}';
ALTER USER '${MYSQL_AUTH_MIGRATION_USER}'@'%' IDENTIFIED BY '${AUTH_MIGRATION_PASSWORD_SQL}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, DROP ON ${AUTH_DB}.* TO '${MYSQL_AUTH_MIGRATION_USER}'@'%';
EOSQL
fi

if [ -n "${MYSQL_EXPENSE_MIGRATION_USER:-}" ] && [ -n "${MYSQL_EXPENSE_MIGRATION_PASSWORD:-}" ]; then
    validate_identifier "$MYSQL_EXPENSE_MIGRATION_USER"
    EXPENSE_MIGRATION_PASSWORD_SQL=$(sql_escape "$MYSQL_EXPENSE_MIGRATION_PASSWORD")
    echo "Provisioning account migrazione Expense (password non mostrata)."
    mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
SET SESSION sql_mode = CONCAT(@@sql_mode, ',NO_BACKSLASH_ESCAPES');
CREATE USER IF NOT EXISTS '${MYSQL_EXPENSE_MIGRATION_USER}'@'%' IDENTIFIED BY '${EXPENSE_MIGRATION_PASSWORD_SQL}';
ALTER USER '${MYSQL_EXPENSE_MIGRATION_USER}'@'%' IDENTIFIED BY '${EXPENSE_MIGRATION_PASSWORD_SQL}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, DROP ON ${EXPENSE_DB}.* TO '${MYSQL_EXPENSE_MIGRATION_USER}'@'%';
EOSQL
fi

# Revoca l'account condiviso solo quando esplicitamente richiesto (REVOKE_SHARED_USER=true)
# e quando entrambi i runtime account dedicati sono configurati.
if [ -n "${MYSQL_AUTH_USER:-}" ] && [ -n "${MYSQL_EXPENSE_USER:-}" ]; then
    if [ "${REVOKE_SHARED_USER:-false}" = "true" ]; then
        echo "Revoca privilegi all'account condiviso '${MYSQL_USER:-spendy}'."
        mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
REVOKE ALL PRIVILEGES, GRANT OPTION FROM '${MYSQL_USER:-spendy}'@'%';
EOSQL
    else
        echo "Mantenimento dei privilegi per l'account condiviso '${MYSQL_USER:-spendy}' (REVOKE_SHARED_USER non abilitato)."
    fi
fi


# Migrazione degli account MySQL su volumi esistenti

## Scopo

L'entrypoint ufficiale dell'immagine MySQL esegue `/docker-entrypoint-initdb.d` solo quando
`/var/lib/mysql` è vuoto. Aggiornare il repository o ricreare i container non applica quindi
automaticamente i nuovi grant a un volume già esistente.

Questa procedura non elimina né ricrea database o volumi. Effettuarla in una finestra di
manutenzione e dopo aver verificato un backup ripristinabile.

## 1. Configurare gli account

Nel file `.env` configurare account runtime e migrator distinti per entrambi i servizi:

- `MYSQL_AUTH_USER` / `MYSQL_AUTH_PASSWORD`
- `MYSQL_EXPENSE_USER` / `MYSQL_EXPENSE_PASSWORD`
- `MYSQL_AUTH_MIGRATION_USER` / `MYSQL_AUTH_MIGRATION_PASSWORD`
- `MYSQL_EXPENSE_MIGRATION_USER` / `MYSQL_EXPENSE_MIGRATION_PASSWORD`

Usare password casuali e uniche; non copiare le password di esempio in produzione.
Non committare `.env`.

## 2. Validare e fare backup

Verificare che il servizio MySQL sia sano:

```sh
docker compose ps mysql
```

Creare un backup prima di modificare privilegi o password. Esempio, con le credenziali root
già disponibili nella shell locale:

```sh
docker compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --databases spendy_auth spendy_expense' > spendy-before-account-migration.sql
```

Verificare che il file non sia vuoto e che il backup sia conservato in un luogo sicuro.

## 3. Aggiornare l'ambiente del container MySQL

Dopo il backup, ricreare il solo container MySQL per caricare le nuove variabili da `.env`. Il volume nominato viene mantenuto; non usare `down -v`:

```sh
docker compose up -d --force-recreate mysql
docker compose ps mysql
```

Attendere che MySQL risulti healthy prima di continuare.

## 4. Provisioning account e grant

Con il container aggiornato e i valori presenti in `.env`, eseguire lo script già montato:

```sh
docker compose exec mysql sh /docker-entrypoint-initdb.d/99-init-users.sh
```

Lo script valida gli identificatori, non stampa le password e assegna privilegi per schema.
Quando gli account runtime e migrator sono entrambi configurati, gli account runtime ricevono
solo `SELECT/INSERT/UPDATE/DELETE`, i migrator ricevono i privilegi DDL necessari a Flyway e
l'account condiviso `spendy` viene revocato.

## 5. Riavvio e verifica

```sh
docker compose up -d --force-recreate auth-service expense-service gateway-service
docker compose ps
./scripts/smoke-test.sh
```

Controllare anche i log dei due servizi per confermare che Flyway abbia completato le migrazioni.
Se l'applicazione non parte, non eliminare il volume: correggere gli account/grant o ripristinare
il backup secondo la procedura operativa.

## Nota di compatibilità

Se non sono configurati account migrator separati, lo script mantiene i privilegi DDL per
l'account runtime dedicato affinché Flyway possa continuare a funzionare. Questa modalità è
compatibile con gli ambienti esistenti ma non offre la separazione runtime/migrazione completa.

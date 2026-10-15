#!/usr/bin/env python3
"""
MongoDB to MySQL Data Migration Script for SpendyApp.

Transfers existing users, refresh tokens, and expenses from MongoDB collections
to MySQL relational schemas (spendy_auth and spendy_expense) using idempotent
batch insertions.

Usage:
    python3 scripts/migrate_mongo_to_mysql.py [options]

Environment Variables supported:
    MONGO_URI, MONGO_AUTH_DB, MONGO_EXPENSE_DB
    MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD
    MYSQL_DATABASE_AUTH, MYSQL_DATABASE_EXPENSE
"""

import argparse
import datetime
import decimal
import os
import sys
from typing import Any, Dict, Optional, Tuple

try:
    import pymysql
    from pymysql.cursors import DictCursor
except ImportError:
    print("Error: 'pymysql' package is required. Install via: pip install pymysql", file=sys.stderr)
    sys.exit(1)

try:
    from pymongo import MongoClient
    from bson import ObjectId
except ImportError:
    print("Error: 'pymongo' package is required. Install via: pip install pymongo", file=sys.stderr)
    sys.exit(1)


def parse_date(value: Any) -> Optional[datetime.datetime]:
    if value is None:
        return None
    if isinstance(value, datetime.datetime):
        return value
    if isinstance(value, datetime.date):
        return datetime.datetime.combine(value, datetime.time.min)
    if isinstance(value, (int, float)):
        # Timestamp in millis or seconds
        if value > 1e11:
            value = value / 1000.0
        return datetime.datetime.fromtimestamp(value, tz=datetime.timezone.utc).replace(tzinfo=None)
    if isinstance(value, str):
        v = value.strip()
        if not v:
            return None
        # ISO formats
        for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d", "%Y-%m-%dT%H:%M:%S.%f", "%Y-%m-%dT%H:%M:%SZ"):
            try:
                return datetime.datetime.strptime(v.rstrip("Z"), fmt.rstrip("Z"))
            except ValueError:
                continue
    return None


def to_decimal(val: Any) -> decimal.Decimal:
    if val is None:
        return decimal.Decimal("0.00")
    if isinstance(val, decimal.Decimal):
        return val
    try:
        return decimal.Decimal(str(val)).quantize(decimal.Decimal("0.01"))
    except (decimal.InvalidOperation, ValueError):
        return decimal.Decimal("0.00")


def migrate_users(mongo_db, mysql_conn, dry_run: bool, batch_size: int) -> Tuple[int, int, int]:
    # Collections can be 'User' or 'users'
    coll_names = mongo_db.list_collection_names()
    coll_name = "User" if "User" in coll_names else ("users" if "users" in coll_names else None)
    if not coll_name:
        print("  [Auth] No 'User' or 'users' collection found in MongoDB. Skipping.")
        return 0, 0, 0

    collection = mongo_db[coll_name]
    total = collection.count_documents({})
    print(f"  [Auth] Found {total} documents in MongoDB collection '{coll_name}'.")

    imported = 0
    skipped = 0
    failed = 0

    insert_sql = """
        INSERT INTO users (id_user, username, name, password, surname, email, language, profile_image, house_user)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            name = VALUES(name),
            surname = VALUES(surname),
            email = VALUES(email),
            language = VALUES(language),
            profile_image = VALUES(profile_image),
            house_user = VALUES(house_user)
    """

    cursor = collection.find()
    batch = []

    for doc in cursor:
        doc_id = str(doc.get("_id", ""))
        username = doc.get("username")
        email = doc.get("email")
        password = doc.get("password")

        if not username or not email or not password:
            print(f"  [Auth] Warning: Skipping user with missing mandatory fields: {doc_id}")
            skipped += 1
            continue

        name = doc.get("name")
        surname = doc.get("surname")
        language = doc.get("language")
        house_user = doc.get("houseUser") or doc.get("house_user")
        profile_img = doc.get("profileImage") or doc.get("profile_image")
        if profile_img and isinstance(profile_img, str):
            profile_img = profile_img.encode("utf-8")

        batch.append((
            doc_id, username, name, password, surname, email, language, profile_img, house_user
        ))

        if len(batch) >= batch_size:
            if not dry_run:
                try:
                    with mysql_conn.cursor() as cur:
                        cur.executemany(insert_sql, batch)
                    mysql_conn.commit()
                    imported += len(batch)
                except Exception as e:
                    mysql_conn.rollback()
                    print(f"  [Auth] Error batch inserting users: {e}")
                    failed += len(batch)
            else:
                imported += len(batch)
            batch = []

    if batch:
        if not dry_run:
            try:
                with mysql_conn.cursor() as cur:
                    cur.executemany(insert_sql, batch)
                mysql_conn.commit()
                imported += len(batch)
            except Exception as e:
                mysql_conn.rollback()
                print(f"  [Auth] Error final batch inserting users: {e}")
                failed += len(batch)
        else:
            imported += len(batch)

    return imported, skipped, failed


def migrate_refresh_tokens(mongo_db, mysql_conn, dry_run: bool, batch_size: int) -> Tuple[int, int, int]:
    coll_names = mongo_db.list_collection_names()
    coll_name = "RefreshToken" if "RefreshToken" in coll_names else ("refresh_tokens" if "refresh_tokens" in coll_names else None)
    if not coll_name:
        print("  [Auth] No 'RefreshToken' or 'refresh_tokens' collection found in MongoDB. Skipping.")
        return 0, 0, 0

    collection = mongo_db[coll_name]
    total = collection.count_documents({})
    print(f"  [Auth] Found {total} documents in MongoDB collection '{coll_name}'.")

    # Fetch existing usernames in MySQL to avoid FK violations
    existing_users = set()
    with mysql_conn.cursor() as cur:
        cur.execute("SELECT username FROM users")
        for row in cur.fetchall():
            existing_users.add(row["username"])

    imported = 0
    skipped = 0
    failed = 0

    insert_sql = """
        INSERT INTO refresh_tokens (id, token, username, expiry_date)
        VALUES (%s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            token = VALUES(token),
            expiry_date = VALUES(expiry_date)
    """

    cursor = collection.find()
    batch = []

    for doc in cursor:
        doc_id = str(doc.get("_id", ""))
        token = doc.get("token")
        username = doc.get("username")
        expiry_raw = doc.get("expiryDate") or doc.get("expiry_date")
        expiry_date = parse_date(expiry_raw)

        if not token or not username or not expiry_date:
            skipped += 1
            continue

        if username not in existing_users:
            print(f"  [Auth] Skipping token for non-existent user: {username}")
            skipped += 1
            continue

        batch.append((doc_id, token, username, expiry_date))

        if len(batch) >= batch_size:
            if not dry_run:
                try:
                    with mysql_conn.cursor() as cur:
                        cur.executemany(insert_sql, batch)
                    mysql_conn.commit()
                    imported += len(batch)
                except Exception as e:
                    mysql_conn.rollback()
                    print(f"  [Auth] Error batch inserting tokens: {e}")
                    failed += len(batch)
            else:
                imported += len(batch)
            batch = []

    if batch:
        if not dry_run:
            try:
                with mysql_conn.cursor() as cur:
                    cur.executemany(insert_sql, batch)
                mysql_conn.commit()
                imported += len(batch)
            except Exception as e:
                mysql_conn.rollback()
                print(f"  [Auth] Error final batch inserting tokens: {e}")
                failed += len(batch)
        else:
            imported += len(batch)

    return imported, skipped, failed


def migrate_expenses(mongo_db, mysql_conn, dry_run: bool, batch_size: int) -> Tuple[int, int, int]:
    coll_names = mongo_db.list_collection_names()
    coll_name = "Expense" if "Expense" in coll_names else ("expenses" if "expenses" in coll_names else None)
    if not coll_name:
        print("  [Expense] No 'Expense' or 'expenses' collection found in MongoDB. Skipping.")
        return 0, 0, 0

    collection = mongo_db[coll_name]
    total = collection.count_documents({})
    print(f"  [Expense] Found {total} documents in MongoDB collection '{coll_name}'.")

    imported = 0
    skipped = 0
    failed = 0

    insert_sql = """
        INSERT INTO expenses (id, type, product, started_date, completed_date, description, amount, fee, currency, state, category, username)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            type = VALUES(type),
            product = VALUES(product),
            started_date = VALUES(started_date),
            completed_date = VALUES(completed_date),
            description = VALUES(description),
            amount = VALUES(amount),
            fee = VALUES(fee),
            currency = VALUES(currency),
            state = VALUES(state),
            category = VALUES(category),
            username = VALUES(username)
    """

    cursor = collection.find()
    batch = []

    for doc in cursor:
        doc_id = str(doc.get("_id", ""))
        username = doc.get("username")
        amount = to_decimal(doc.get("amount"))

        if not username:
            print(f"  [Expense] Warning: Skipping expense with missing username: {doc_id}")
            skipped += 1
            continue

        exp_type = doc.get("type")
        product = doc.get("product")
        started_date = parse_date(doc.get("startedDate") or doc.get("started_date"))
        completed_date = parse_date(doc.get("completedDate") or doc.get("completed_date"))
        description = doc.get("description")
        fee = to_decimal(doc.get("fee", 0.00))
        currency = doc.get("currency", "EUR")
        state = doc.get("state")
        category = doc.get("category")

        batch.append((
            doc_id, exp_type, product, started_date, completed_date,
            description, amount, fee, currency, state, category, username
        ))

        if len(batch) >= batch_size:
            if not dry_run:
                try:
                    with mysql_conn.cursor() as cur:
                        cur.executemany(insert_sql, batch)
                    mysql_conn.commit()
                    imported += len(batch)
                except Exception as e:
                    mysql_conn.rollback()
                    print(f"  [Expense] Error batch inserting expenses: {e}")
                    failed += len(batch)
            else:
                imported += len(batch)
            batch = []

    if batch:
        if not dry_run:
            try:
                with mysql_conn.cursor() as cur:
                    cur.executemany(insert_sql, batch)
                mysql_conn.commit()
                imported += len(batch)
            except Exception as e:
                mysql_conn.rollback()
                print(f"  [Expense] Error final batch inserting expenses: {e}")
                failed += len(batch)
        else:
            imported += len(batch)

    return imported, skipped, failed


def main():
    parser = argparse.ArgumentParser(description="Migrate SpendyApp data from MongoDB to MySQL")
    parser.add_argument("--mongo-uri", default=os.getenv("MONGO_URI", "mongodb://localhost:27017"))
    parser.add_argument("--mongo-auth-db", default=os.getenv("MONGO_AUTH_DB", "AuthDatabase"))
    parser.add_argument("--mongo-expense-db", default=os.getenv("MONGO_EXPENSE_DB", "SpendyApp"))
    parser.add_argument("--mysql-host", default=os.getenv("MYSQL_HOST", "localhost"))
    parser.add_argument("--mysql-port", type=int, default=int(os.getenv("MYSQL_PORT", "3306")))
    parser.add_argument("--mysql-user", default=os.getenv("MYSQL_USER", "root"))
    parser.add_argument("--mysql-password", default=os.getenv("MYSQL_PASSWORD", ""))
    parser.add_argument("--mysql-auth-db", default=os.getenv("MYSQL_DATABASE_AUTH", "spendy_auth"))
    parser.add_argument("--mysql-expense-db", default=os.getenv("MYSQL_DATABASE_EXPENSE", "spendy_expense"))
    parser.add_argument("--batch-size", type=int, default=500)
    parser.add_argument("--dry-run", action="store_true", help="Simulate migration without modifying MySQL")

    args = parser.parse_args()

    print("=" * 60)
    print("🚀 SpendyApp MongoDB to MySQL Migration Utility")
    print(f"   Mode: {'DRY RUN (no database changes)' if args.dry_run else 'LIVE MIGRATION'}")
    print(f"   MySQL Target: {args.mysql_host}:{args.mysql_port} (User: {args.mysql_user})")
    print(f"   MongoDB Source: {args.mongo_uri}")
    print("=" * 60)

    try:
        mongo_client = MongoClient(args.mongo_uri, serverSelectionTimeoutMS=5000)
        mongo_client.server_info()
        print("✅ Successfully connected to MongoDB.")
    except Exception as e:
        print(f"❌ Failed to connect to MongoDB: {e}")
        sys.exit(1)

    # 1. Migrate Auth Service Data
    print(f"\n[1/2] Migrating Auth Database ({args.mongo_auth_db} -> {args.mysql_auth_db})...")
    try:
        auth_conn = pymysql.connect(
            host=args.mysql_host,
            port=args.mysql_port,
            user=args.mysql_user,
            password=args.mysql_password,
            database=args.mysql_auth_db,
            cursorclass=DictCursor,
            charset="utf8mb4",
            autocommit=False
        )
        print(f"✅ Connected to MySQL database '{args.mysql_auth_db}'.")
    except Exception as e:
        print(f"❌ Failed to connect to MySQL '{args.mysql_auth_db}': {e}")
        sys.exit(1)

    try:
        auth_mongo_db = mongo_client[args.mongo_auth_db]
        u_imp, u_skip, u_fail = migrate_users(auth_mongo_db, auth_conn, args.dry_run, args.batch_size)
        t_imp, t_skip, t_fail = migrate_refresh_tokens(auth_mongo_db, auth_conn, args.dry_run, args.batch_size)
        print(f"  -> Users: {u_imp} imported/updated, {u_skip} skipped, {u_fail} failed")
        print(f"  -> RefreshTokens: {t_imp} imported/updated, {t_skip} skipped, {t_fail} failed")
    finally:
        auth_conn.close()

    # 2. Migrate Expense Service Data
    print(f"\n[2/2] Migrating Expense Database ({args.mongo_expense-db if hasattr(args, 'mongo_expense-db') else args.mongo_expense_db} -> {args.mysql_expense_db})...")
    try:
        expense_conn = pymysql.connect(
            host=args.mysql_host,
            port=args.mysql_port,
            user=args.mysql_user,
            password=args.mysql_password,
            database=args.mysql_expense_db,
            cursorclass=DictCursor,
            charset="utf8mb4",
            autocommit=False
        )
        print(f"✅ Connected to MySQL database '{args.mysql_expense_db}'.")
    except Exception as e:
        print(f"❌ Failed to connect to MySQL '{args.mysql_expense_db}': {e}")
        sys.exit(1)

    try:
        expense_mongo_db = mongo_client[args.mongo_expense_db]
        e_imp, e_skip, e_fail = migrate_expenses(expense_mongo_db, expense_conn, args.dry_run, args.batch_size)
        print(f"  -> Expenses: {e_imp} imported/updated, {e_skip} skipped, {e_fail} failed")
    finally:
        expense_conn.close()

    print("\n" + "=" * 60)
    print("🏁 Data Migration Complete!")
    print(f"   Users:         {u_imp} imported, {u_skip} skipped, {u_fail} failed")
    print(f"   RefreshTokens: {t_imp} imported, {t_skip} skipped, {t_fail} failed")
    print(f"   Expenses:      {e_imp} imported, {e_skip} skipped, {e_fail} failed")
    print("=" * 60)


if __name__ == "__main__":
    main()

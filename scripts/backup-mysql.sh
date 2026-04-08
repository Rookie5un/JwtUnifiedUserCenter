#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "${SCRIPT_DIR}/.." && pwd)
BACKUP_DIR="${BACKUP_DIR:-${PROJECT_ROOT}/backups}"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-jwt_center}"
MYSQL_USER="${MYSQL_USER:-jwt_app}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-jwt_app_password}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

mkdir -p "${BACKUP_DIR}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/${MYSQL_DB}_${TIMESTAMP}.sql.gz"

MYSQL_PWD="${MYSQL_PASSWORD}" mysqldump \
  --single-transaction \
  --skip-lock-tables \
  --routines \
  --triggers \
  --set-gtid-purged=OFF \
  -h "${MYSQL_HOST}" \
  -P "${MYSQL_PORT}" \
  -u "${MYSQL_USER}" \
  "${MYSQL_DB}" | gzip > "${BACKUP_FILE}"

find "${BACKUP_DIR}" -type f -name "${MYSQL_DB}_*.sql.gz" -mtime +"${BACKUP_RETENTION_DAYS}" -delete

echo "Backup created: ${BACKUP_FILE}"

#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <backup-file.sql.gz|backup-file.sql>" >&2
  exit 1
fi

BACKUP_FILE="$1"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-jwt_center}"
MYSQL_USER="${MYSQL_USER:-jwt_app}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-jwt_app_password}"

if [ ! -f "${BACKUP_FILE}" ]; then
  echo "Backup file not found: ${BACKUP_FILE}" >&2
  exit 1
fi

case "${BACKUP_FILE}" in
  *.sql.gz)
    gzip -dc "${BACKUP_FILE}" | MYSQL_PWD="${MYSQL_PASSWORD}" mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" "${MYSQL_DB}"
    ;;
  *.sql)
    MYSQL_PWD="${MYSQL_PASSWORD}" mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" "${MYSQL_DB}" < "${BACKUP_FILE}"
    ;;
  *)
    echo "Unsupported file type: ${BACKUP_FILE}" >&2
    exit 1
    ;;
esac

echo "Restore completed from: ${BACKUP_FILE}"

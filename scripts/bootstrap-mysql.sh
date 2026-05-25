#!/bin/sh
set -eu

MYSQL_ADMIN_HOST="${MYSQL_ADMIN_HOST:-127.0.0.1}"
MYSQL_ADMIN_PORT="${MYSQL_ADMIN_PORT:-3306}"
MYSQL_ADMIN_USER="${MYSQL_ADMIN_USER:-root}"
MYSQL_ADMIN_PASSWORD="${MYSQL_ADMIN_PASSWORD:-root}"

MYSQL_DB="${MYSQL_DB:-jwt_center}"
MYSQL_USER="${MYSQL_USER:-jwt_app}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-jwt_app_password}"

MYSQL_PWD="${MYSQL_ADMIN_PASSWORD}" mysql \
  -h "${MYSQL_ADMIN_HOST}" \
  -P "${MYSQL_ADMIN_PORT}" \
  -u "${MYSQL_ADMIN_USER}" <<EOSQL
CREATE DATABASE IF NOT EXISTS \`${MYSQL_DB}\`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED BY '${MYSQL_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${MYSQL_DB}\`.* TO '${MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
EOSQL

MYSQL_PWD="${MYSQL_ADMIN_PASSWORD}" mysql \
  -h "${MYSQL_ADMIN_HOST}" \
  -P "${MYSQL_ADMIN_PORT}" \
  -u "${MYSQL_ADMIN_USER}" "${MYSQL_DB}" <<'EOSQL'
CREATE TABLE IF NOT EXISTS permissions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  action VARCHAR(40) NOT NULL,
  code VARCHAR(80) NOT NULL,
  description VARCHAR(255) NULL,
  name VARCHAR(80) NOT NULL,
  resource VARCHAR(80) NOT NULL,
  type ENUM('API', 'BUTTON', 'MENU') NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_permissions_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS departments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  description VARCHAR(255) NULL,
  name VARCHAR(80) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_departments_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  code VARCHAR(50) NOT NULL,
  description VARCHAR(255) NULL,
  name VARCHAR(80) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6) NULL,
  department_id BIGINT NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  email VARCHAR(120) NULL,
  password_hash VARCHAR(120) NOT NULL,
  phone VARCHAR(32) NULL,
  status ENUM('ACTIVE', 'DISABLED') NOT NULL,
  username VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  KEY idx_users_department_id (department_id),
  CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS deleted_at DATETIME(6) NULL;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS department_id BIGINT NULL;

SET @users_department_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'department'
);

SET @sql := IF(@users_department_column_exists > 0,
  'INSERT INTO departments (created_at, updated_at, description, name)
   SELECT NOW(6), NOW(6), CONCAT(source.name, '' team''), source.name
   FROM (
     SELECT DISTINCT TRIM(department) AS name
     FROM users
     WHERE department IS NOT NULL
       AND TRIM(department) <> ''''
   ) source
   LEFT JOIN departments d ON d.name = source.name
   WHERE d.id IS NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_department_column_exists > 0,
  'UPDATE users u
   JOIN departments d ON d.name = TRIM(u.department)
   SET u.department_id = d.id
   WHERE u.department_id IS NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO departments (created_at, updated_at, description, name)
SELECT NOW(6), NOW(6), 'Unassigned team', 'Unassigned'
WHERE EXISTS (SELECT 1 FROM users WHERE department_id IS NULL)
  AND NOT EXISTS (SELECT 1 FROM departments WHERE name = 'Unassigned');

UPDATE users
SET department_id = (SELECT id FROM departments WHERE name = 'Unassigned')
WHERE department_id IS NULL;

ALTER TABLE users
  MODIFY department_id BIGINT NOT NULL;

SET @users_department_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND index_name = 'idx_users_department_id'
);

SET @sql := IF(@users_department_index_exists = 0,
  'ALTER TABLE users ADD KEY idx_users_department_id (department_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @users_department_fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.referential_constraints
  WHERE constraint_schema = DATABASE()
    AND constraint_name = 'fk_users_department'
);

SET @sql := IF(@users_department_fk_exists = 0,
  'ALTER TABLE users ADD CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments (id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_department_column_exists > 0,
  'ALTER TABLE users DROP COLUMN department',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  KEY idx_user_roles_role_id (role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  KEY idx_role_permissions_permission_id (permission_id),
  CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
  CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6) NULL,
  token VARCHAR(160) NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_tokens_token (token),
  KEY idx_refresh_tokens_user_id (user_id),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS performance_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  amount DECIMAL(14,2) NOT NULL,
  approved_at DATETIME(6) NULL,
  department VARCHAR(80) NOT NULL,
  note VARCHAR(400) NULL,
  occurred_on DATE NOT NULL,
  rejected_reason VARCHAR(255) NULL,
  status ENUM('APPROVED', 'PENDING', 'REJECTED') NOT NULL,
  type VARCHAR(80) NOT NULL,
  approved_by BIGINT NULL,
  owner_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_performance_records_owner_id (owner_id),
  KEY idx_performance_records_approved_by (approved_by),
  KEY idx_performance_records_status (status),
  KEY idx_performance_records_department (department),
  CONSTRAINT fk_performance_records_owner FOREIGN KEY (owner_id) REFERENCES users (id),
  CONSTRAINT fk_performance_records_approved_by FOREIGN KEY (approved_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS approval_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  decision ENUM('APPROVED', 'REJECTED') NOT NULL,
  reason VARCHAR(255) NULL,
  performance_record_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_approval_records_record_id (performance_record_id),
  KEY idx_approval_records_reviewer_id (reviewer_id),
  CONSTRAINT fk_approval_records_record FOREIGN KEY (performance_record_id) REFERENCES performance_records (id),
  CONSTRAINT fk_approval_records_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS operation_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  action VARCHAR(80) NOT NULL,
  actor_id BIGINT NULL,
  actor_username VARCHAR(50) NULL,
  detail VARCHAR(500) NULL,
  resource_id VARCHAR(80) NULL,
  resource_type VARCHAR(80) NOT NULL,
  result ENUM('FAILURE', 'SUCCESS') NOT NULL,
  PRIMARY KEY (id),
  KEY idx_operation_logs_actor_id (actor_id),
  KEY idx_operation_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
EOSQL

echo "MySQL bootstrap completed for database: ${MYSQL_DB}"

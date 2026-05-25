-- Migrates users.department text values to users.department_id foreign keys.
-- Run this script against the target jwt_center MySQL database.

CREATE TABLE IF NOT EXISTS departments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  description VARCHAR(255) NULL,
  name VARCHAR(80) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_departments_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
   WHERE u.department_id IS NULL OR u.department_id = 0',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO departments (created_at, updated_at, description, name)
SELECT NOW(6), NOW(6), 'Unassigned team', 'Unassigned'
WHERE EXISTS (SELECT 1 FROM users WHERE department_id IS NULL OR department_id = 0)
  AND NOT EXISTS (SELECT 1 FROM departments WHERE name = 'Unassigned');

UPDATE users
SET department_id = (SELECT id FROM departments WHERE name = 'Unassigned')
WHERE department_id IS NULL OR department_id = 0;

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
  FROM information_schema.key_column_usage
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'department_id'
    AND referenced_table_schema = DATABASE()
    AND referenced_table_name = 'departments'
    AND referenced_column_name = 'id'
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

SELECT
  c.column_name,
  c.column_type,
  c.is_nullable,
  k.constraint_name,
  k.referenced_table_name,
  k.referenced_column_name
FROM information_schema.columns c
LEFT JOIN information_schema.key_column_usage k
  ON k.table_schema = c.table_schema
  AND k.table_name = c.table_name
  AND k.column_name = c.column_name
  AND k.referenced_table_name IS NOT NULL
WHERE c.table_schema = DATABASE()
  AND c.table_name = 'users'
  AND c.column_name = 'department_id';

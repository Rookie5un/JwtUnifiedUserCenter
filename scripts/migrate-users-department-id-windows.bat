@echo off
setlocal

rem Usage:
rem   migrate-users-department-id-windows.bat
rem   migrate-users-department-id-windows.bat 127.0.0.1 3306 jwt_center root 123456
rem
rem Environment variables are also supported:
rem   MYSQL_HOST, MYSQL_PORT, MYSQL_DB, MYSQL_USER, MYSQL_PASSWORD

set "MYSQL_HOST=%~1"
set "MYSQL_PORT=%~2"
set "MYSQL_DB=%~3"
set "MYSQL_USER=%~4"
set "MYSQL_PASSWORD=%~5"

if "%MYSQL_HOST%"=="" set "MYSQL_HOST=%MYSQL_HOST%"
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=%MYSQL_PORT%"
if "%MYSQL_DB%"=="" set "MYSQL_DB=%MYSQL_DB%"
if "%MYSQL_USER%"=="" set "MYSQL_USER=%MYSQL_USER%"
if "%MYSQL_PASSWORD%"=="" set "MYSQL_PASSWORD=%MYSQL_PASSWORD%"

if "%MYSQL_HOST%"=="" set "MYSQL_HOST=127.0.0.1"
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=3306"
if "%MYSQL_DB%"=="" set "MYSQL_DB=jwt_center"
if "%MYSQL_USER%"=="" set "MYSQL_USER=root"
if "%MYSQL_PASSWORD%"=="" set "MYSQL_PASSWORD=123456"

set "SCRIPT_DIR=%~dp0"
set "SQL_FILE=%SCRIPT_DIR%migrate-users-department-id.sql"

if not exist "%SQL_FILE%" (
  echo SQL file not found: "%SQL_FILE%"
  exit /b 1
)

where mysql >nul 2>nul
if errorlevel 1 (
  echo mysql.exe was not found in PATH.
  echo Please install MySQL Client or add MySQL bin directory to PATH.
  exit /b 1
)

echo Migrating database "%MYSQL_DB%" on %MYSQL_HOST%:%MYSQL_PORT% ...
mysql ^
  --default-character-set=utf8mb4 ^
  -h "%MYSQL_HOST%" ^
  -P "%MYSQL_PORT%" ^
  -u "%MYSQL_USER%" ^
  -p"%MYSQL_PASSWORD%" ^
  "%MYSQL_DB%" ^
  < "%SQL_FILE%"

if errorlevel 1 (
  echo Migration failed.
  exit /b 1
)

echo Migration completed.
endlocal

@echo off
echo ====================================================
echo Starting Student Management System with MySQL...
echo Target Database: university2_0_erp (localhost:3306)
echo Username: root
echo ====================================================

if "%DB_PASSWORD%"=="" (
    echo [INFO] DB_PASSWORD is not set in current console session.
    echo [INFO] Please set DB_PASSWORD if your MySQL root account requires a password.
)

call mvnw.cmd spring-boot:run
pause

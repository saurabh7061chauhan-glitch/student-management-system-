@echo off
echo ====================================================
echo Starting Student Management System with MySQL...
echo Target Database: university2_0_erp (localhost:3306)
echo Username: root
echo ====================================================

if "%DB_PASSWORD%"=="" (
    set DB_PASSWORD=@1S2aurabh@1S2
)

call mvnw.cmd spring-boot:run
pause

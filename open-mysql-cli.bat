@echo off
title MySQL Terminal - university2_0_erp
echo =======================================================
echo   Connected to Database: university2_0_erp
echo   Commands to try:
echo     SHOW TABLES;
echo     SELECT id, student_id, full_name, email FROM students;
echo     SELECT * FROM courses;
echo     exit;
echo =======================================================
if "%DB_PASSWORD%"=="" (
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p university2_0_erp
) else (
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root "--password=%DB_PASSWORD%" university2_0_erp
)

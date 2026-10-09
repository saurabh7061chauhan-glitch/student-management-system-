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
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p@1S2aurabh@1S2 university2_0_erp

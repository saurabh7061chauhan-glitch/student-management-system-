@echo off
echo Starting Student Management System in Development Mode (H2 in-memory)...
call mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
pause

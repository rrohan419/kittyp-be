@echo off
REM Windows launcher — avoids PowerShell splitting -Dspring-boot.run.profiles=local
cd /d "%~dp0"
call "%~dp0mvnw.cmd" spring-boot:run -Dspring-boot.run.profiles=local %*

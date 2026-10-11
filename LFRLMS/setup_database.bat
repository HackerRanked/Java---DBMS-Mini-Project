@echo off
setlocal
cd /d "%~dp0"

set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe"
if not exist "%MYSQL_EXE%" set "MYSQL_EXE=mysql"

set "RESET_DATABASE=0"
if /i "%~1"=="/reset" goto confirm_reset
goto prepare_sql

:confirm_reset
echo WARNING: this will permanently delete the existing lfrlms database and all its data.
set /p "RESET_CONFIRM=Type RESET to continue: "
if /i not "%RESET_CONFIRM%"=="RESET" goto reset_cancelled
set "RESET_DATABASE=1"

:prepare_sql
set "MYSQL_INPUT=%TEMP%\lfrlms_setup_%RANDOM%.sql"
type nul > "%MYSQL_INPUT%"
if "%RESET_DATABASE%"=="1" goto write_reset
goto append_files

:write_reset
>> "%MYSQL_INPUT%" echo DROP DATABASE IF EXISTS lfrlms;

:append_files
for %%F in (sql\01_schema.sql sql\02_functions_procedures.sql sql\03_views.sql sql\04_triggers.sql sql\05_seed.sql) do (
  type "%%F" >> "%MYSQL_INPUT%"
  echo.>>"%MYSQL_INPUT%"
)
if exist "sql\06_demo_queries.sql" (
  type "sql\06_demo_queries.sql" >> "%MYSQL_INPUT%"
  echo.>>"%MYSQL_INPUT%"
)
type "sql\07_negative_tests.sql" >> "%MYSQL_INPUT%"

echo Running the SQL scripts in one MySQL session...
"%MYSQL_EXE%" --force -u root -p < "%MYSQL_INPUT%"
set "MYSQL_STATUS=%ERRORLEVEL%"
del "%MYSQL_INPUT%" >nul 2>&1
endlocal & exit /b %MYSQL_STATUS%

:reset_cancelled
echo Database reset cancelled.
endlocal & exit /b 1

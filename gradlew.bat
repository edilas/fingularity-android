@echo off
setlocal
set VER=8.13
if "%GRADLE_USER_HOME%"=="" (
  set "BASE=%USERPROFILE%\.gradle\wrapper\dists\fingularity-gradle-%VER%"
) else (
  set "BASE=%GRADLE_USER_HOME%\wrapper\dists\fingularity-gradle-%VER%"
)
set "BIN=%BASE%\gradle-%VER%\bin\gradle.bat"
if not exist "%BIN%" (
  if not exist "%BASE%" mkdir "%BASE%"
  set "ZIP=%BASE%\gradle-%VER%-bin.zip"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%VER%-bin.zip' -OutFile '%BASE%\gradle-%VER%-bin.zip'; Expand-Archive -Force '%BASE%\gradle-%VER%-bin.zip' '%BASE%'"
  if errorlevel 1 exit /b 1
)
call "%BIN%" %*

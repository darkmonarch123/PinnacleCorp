@echo off
setlocal enabledelayedexpansion

set MAVEN_VERSION=3.9.9
set WRAPPER_DIR=%~dp0.mvn\wrapper
set MAVEN_HOME_DIR=%~dp0.mvn\maven-%MAVEN_VERSION%
set MAVEN_ZIP=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip

if exist "%MAVEN_HOME_DIR%\bin\mvn.cmd" goto RUN

echo Maven not found locally — downloading Maven %MAVEN_VERSION% (one-time setup)...
if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'"
if errorlevel 1 (
    echo Download failed. Check your internet connection and try again.
    exit /b 1
)

echo Extracting Maven...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%~dp0.mvn' -Force"
if errorlevel 1 (
    echo Extraction failed.
    exit /b 1
)

ren "%~dp0.mvn\apache-maven-%MAVEN_VERSION%" "maven-%MAVEN_VERSION%"
del "%MAVEN_ZIP%"

echo Maven installed locally in .mvn\maven-%MAVEN_VERSION%

:RUN
"%MAVEN_HOME_DIR%\bin\mvn.cmd" %*

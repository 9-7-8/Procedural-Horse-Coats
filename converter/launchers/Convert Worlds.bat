@echo off
rem Opens the Ixora's Horse Overhaul world converter. Put this file next to ixoras-converter.jar and double-click it.
rem Uses a Java it finds: first on the PATH, then the one the Minecraft launcher installed.
rem UNVERIFIED: the launcher runtime folder below is where the official launcher usually puts Java; never tried.
cd /d "%~dp0"
where javaw >nul 2>nul
if %errorlevel%==0 (
  start "" javaw -jar ixoras-converter.jar %*
  exit /b
)
for /d %%a in ("%APPDATA%\.minecraft\runtime\*") do for /d %%b in ("%%a\*") do for /d %%c in ("%%b\*") do (
  if exist "%%c\bin\javaw.exe" (
    start "" "%%c\bin\javaw.exe" -jar ixoras-converter.jar %*
    exit /b
  )
)
echo Java was not found. Install Java 17 or newer from adoptium.net, or run the converter from a command line.
pause

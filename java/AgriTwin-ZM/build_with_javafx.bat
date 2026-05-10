@echo off
REM ============================================================
REM  AgriTwin-ZM v1.0 -- Windows build script (with JavaFX GUI)
REM  Compiles ALL .java files including GuiApp.java.
REM
REM  Requirement:
REM    Set JAVAFX_LIB to the path of your JavaFX SDK 'lib' folder,
REM    e.g.  set JAVAFX_LIB=C:\javafx-sdk-21\lib
REM ============================================================

setlocal
if "%JAVAFX_LIB%"=="" (
    echo Please set JAVAFX_LIB to your JavaFX SDK 'lib' folder first.
    echo Example:
    echo   set JAVAFX_LIB=C:\javafx-sdk-25\javafx-sdk-25.0.3\lib
    exit /b 1
)

set SRC=src
set OUT=out
set LIB=lib
if not exist %OUT% mkdir %OUT%

REM PDF export requires pdfbox-app-3.0.1.jar in the lib\ folder.
REM Download from https://pdfbox.apache.org/download.html and place it there.
set CP_EXTRA=
if exist "%LIB%\*.jar" set CP_EXTRA=;%LIB%\*

del /q %TEMP%\agritwin_sources.txt 2>nul
for /R %SRC% %%F in (*.java) do echo %%F>>%TEMP%\agritwin_sources.txt

echo Compiling AgriTwin-ZM with JavaFX modules from %JAVAFX_LIB%...
javac --module-path "%JAVAFX_LIB%" --add-modules javafx.controls -cp ".%CP_EXTRA%" -d %OUT% @%TEMP%\agritwin_sources.txt
if errorlevel 1 (
    echo *** Compilation FAILED ***
    exit /b 1
)

echo.
echo Compilation OK.
echo.
echo Run console mode:
echo   java -cp %OUT% agritwin.Main
echo Run GUI mode:
echo   java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls -cp "%OUT%%CP_EXTRA%" agritwin.Main --gui
echo Run tests:
echo   java -cp %OUT% agritwin.TestRunner
echo.
endlocal

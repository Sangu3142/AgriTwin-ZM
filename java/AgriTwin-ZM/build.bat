@echo off
REM ============================================================
REM  AgriTwin-ZM v1.0 -- Windows build script
REM  Compiles all .java files into .\out, then offers to run.
REM ============================================================

setlocal
set SRC=src
set OUT=out

if not exist %OUT% mkdir %OUT%

echo Compiling AgriTwin-ZM (console-only, no JavaFX)...
echo (To include the JavaFX GUI, see build_with_javafx.bat)
echo.

REM Build the file list, EXCLUDING GuiApp.java (which needs JavaFX).
REM This guarantees the console build always succeeds with a vanilla JDK.
del /q %TEMP%\agritwin_sources.txt 2>nul
for /R %SRC% %%F in (*.java) do (
    echo %%F | findstr /I "GuiApp.java" >nul
    if errorlevel 1 echo %%F>>%TEMP%\agritwin_sources.txt
)

javac -d %OUT% @%TEMP%\agritwin_sources.txt
if errorlevel 1 (
    echo.
    echo *** Compilation FAILED ***
    exit /b 1
)

echo Compilation OK. Class files in %OUT%\
echo.
echo Run options:
echo   java -cp %OUT% agritwin.Main          (console)
echo   java -cp %OUT% agritwin.TestRunner    (run the 7 unit tests)
echo.
endlocal

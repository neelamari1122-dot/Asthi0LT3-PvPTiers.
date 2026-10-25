@echo off
where java >nul 2>&1 || (echo Please install JDK 21 & pause & exit /b 1)
where gradle >nul 2>&1 || (echo Please install Gradle 9.6.1 or build on GitHub Actions & pause & exit /b 1)
gradle build
if errorlevel 1 (echo Compilation failed. Do NOT rename ZIP to JAR. & pause & exit /b 1)
echo Actual compiled Fabric mod: build\libs\Asthi0LT3-PvPTiers-1.21.11-1.1.0.jar
pause

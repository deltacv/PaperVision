@echo off
setlocal enabledelayedexpansion

if "%1"=="" (
    echo Error: The path to the root project is required as the first argument.
    exit /b 1
)

if "%2"=="" (
    echo Error: The signing key is required as the second argument.
    exit /b 1
)

for %%i in (EOCVSimPlugin\build\libs\*.jar) do (
    java -classpath "%~1\tools\Common.jar" io.github.deltacv.eocvsim.plugin.security.PluginSigningTool --plugin=%%i --authority=deltacv --key=%2
)
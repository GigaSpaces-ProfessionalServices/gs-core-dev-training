
set SCRIPTS_DIR="%~dp0"

call %SCRIPTS_DIR%\settings.bat

%GS_HOME%\bin\gs.bat host run-agent --auto --gsc=1


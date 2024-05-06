
set SCRIPTS_DIR="%~dp0"

call %SCRIPTS_DIR%\settings.bat

set CWD=%cd%

cd %SCRIPTS_DIR%\..

%GS_HOME%\bin\gs.bat service deploy processor-pu processor\target\*.jar

cd %CWD%


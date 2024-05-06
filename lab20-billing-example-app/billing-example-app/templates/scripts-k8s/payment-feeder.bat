
set SCRIPTS_DIR="%~dp0"

set CWD=%cd%

cd %SCRIPTS_DIR%\yaml

kubectl apply -f myjob.yaml

cd %CWD%
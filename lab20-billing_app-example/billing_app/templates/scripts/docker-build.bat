

set SCRIPTS_DIR="%~dp0"

set CWD=%cd%

docker login -u {{docker.username}}

rem build the processor
echo ########################################
echo # Building the processor for Docker...
echo ########################################

cd %SCRIPTS_DIR%\..\processor

docker buildx build --platform linux/amd64 --push --no-cache -t {{docker.username}}/processor:{{app.docker.version}} .


rem build the feeder
echo ########################################
echo # Building the payment-feeder for Docker...
echo ########################################

cd %SCRIPTS_DIR%\..\payment-feeder

docker buildx build --platform linux/amd64 --push --no-cache -t {{docker.username}}/payment-feeder:{{app.docker.version}} .


rem build the rest-app
echo ########################################
echo # Building the rest-application for Docker...
echo ########################################
cd %SCRIPTS_DIR%\..\rest-application

docker buildx build --platform linux/amd64 --push --no-cache -t {{docker.username}}/rest-app:{{app.docker.version}} .


cd %CWD%

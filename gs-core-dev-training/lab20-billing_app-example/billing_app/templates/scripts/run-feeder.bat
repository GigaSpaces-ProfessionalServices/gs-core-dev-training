
set SCRIPTS_DIR="%~dp0"

cd %SCRIPTS_DIR%\..\payment-feeder

mvn clean spring-boot:run -Dspring-boot.run.profiles=localhost


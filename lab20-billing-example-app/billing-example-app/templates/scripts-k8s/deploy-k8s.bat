
set SCRIPTS_DIR="%~dp0"

set CWD=%cd%

cd %SCRIPTS_DIR%\yaml

echo ########################################
echo This script will deploy GigaSpaces on a Kubernetes environment.


echo ########################################
echo # Creating 'cache' helm repo...
helm repo add cache https://resources.gigaspaces.com/helm-charts-dih 

helm repo update cache
echo # helm repo setup done.
echo ########################################


echo ########################################
echo # Installing GigaSpaces Manager...
helm install manager cache/xap-manager --version {{gigaspaces.version}} --set global.security.enabled=false,java.options="-Dcom.gs.hsqldb.all-metrics-recording.enabled=false"
echo # GigaSpaces Manager helm install done. Some resources such as pods may take some minutes to complete.
echo ########################################


echo ########################################
echo # For demonstration purposes, we will expose the UI via NodePort.
kubectl apply -f np.yaml
echo # NodePort configuration done.
echo ########################################


echo ########################################
echo # Installing operator...
helm install operator cache/xap-operator --version {{gigaspaces.version}} --set  global.security.enabled=false
echo # Operator helm install done.
echo ########################################


echo ########################################
echo # Installing space...
helm install processor cache/xap-pu --version {{gigaspaces.version}} --set schema=partitioned,partitions=1,ha=false,resourceUrl=pu.jar,image.repository={{docker.username}}/processor,image.tag={{app.docker.version}},java.options="-Dcom.gs.hsqldb.all-metrics-recording.enabled=false"
echo # Space helm install done.
echo ########################################

cd %CWD%